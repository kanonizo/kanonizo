package org.kanonizo.reporting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.framework.CUTChromosome;
import org.kanonizo.framework.Chromosome;
import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestSuiteChromosome;
import org.kanonizo.framework.instrumentation.Instrumenter;

public class CoverageWriter extends CsvWriter {
  private Chromosome problem;
  private SearchAlgorithm algorithm;

  public CoverageWriter(Chromosome problem, SearchAlgorithm algorithm) {
    this.problem = problem;
    this.algorithm = algorithm;
  }

  @Override
  public String getDir() {
    return "coverage";
  }

  @Override
  public void prepareCsv() {
    String[] headers = new String[] { "Class", "NumLinesCovered", "NumLinesMissed", "LinesCovered", "LinesMissed", "PercentageLineCoverage",
        "Total Branches", "BranchesCovered", "BranchesMissed", "PercentageBranchCoverage" };
    setHeaders(headers);
    List<CUTChromosome> cuts = ((TestSuiteChromosome) problem).getSUT().getClassesUnderTest();
    List<TestCaseChromosome> testCases = algorithm.getCurrentOptimal().getTestCases();
    Instrumenter inst = Framework.getInstrumenter();
    for (CUTChromosome cut : cuts) {
      if (!cut.getCUT().isInterface()) {
        Set<Integer> linesCovered = new HashSet();
        Set<Integer> branchesCovered = new HashSet();
        for(TestCaseChromosome tc : testCases){
          Map<CUTChromosome, Set<Integer>> lines = inst.getLinesCovered(tc);
          Map<CUTChromosome, Set<Integer>> branches = inst.getBranchesCovered(tc);
          if(lines.containsKey(cut)){
            linesCovered.addAll(lines.get(cut));
          }
          if(branches.containsKey(cut)){
            branchesCovered.addAll(branches.get(cut));
          }
        }
        int totalLines = inst.getTotalLines(cut);
        int totalBranches = inst.getTotalBranches(cut);
        Set<Integer> linesMissed = IntStream.range(1, totalLines).filter(line -> !linesCovered.contains(line))
            .collect(HashSet::new, HashSet::add, HashSet::addAll);
        Set<Integer> branchesMissed = IntStream.range(1, totalBranches).filter(branch -> !branchesCovered.contains(branch))
            .collect(HashSet::new, HashSet::add, HashSet::addAll);
        List<Integer> orderedLinesCovered = new ArrayList<Integer>(linesCovered);
        Collections.sort(orderedLinesCovered);
        List<Integer> orderedLinesMissed = new ArrayList<Integer>(linesMissed);
        Collections.sort(orderedLinesMissed);
        double percentageCoverage = totalLines > 0 ? (double) linesCovered.size() / totalLines : 0;
        double percentageBranch = totalBranches > 0 ? (double) branchesCovered.size() / totalBranches : 0;
        String[] csv = new String[] { cut.getCUT().getName(), Integer.toString(linesCovered.size()),
            Integer.toString(linesMissed.size()),
                linesCovered.size() > 0 ? orderedLinesCovered.stream().map(line -> Integer.toString(line)).reduce((lineNumber, lineNumber2) -> lineNumber+":"+lineNumber2).get() : "",
                linesMissed.size() > 0 ? orderedLinesMissed.stream().map(line -> Integer.toString(line)).reduce((lineNumber, lineNumber2) -> lineNumber+":"+lineNumber2).get() : "",
                Double.toString(percentageCoverage),
            Integer.toString(totalBranches),
            Double.toString(branchesCovered.size()),
            Double.toString(branchesMissed.size()),
            Double.toString(percentageBranch) };
        addRow(csv);
      }
    }
  }

}