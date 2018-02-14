package org.kanonizo.reporting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.framework.CUTChromosome;
import org.kanonizo.framework.Chromosome;
import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestSuiteChromosome;
import org.kanonizo.util.HashSetCollector;

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
    for (CUTChromosome cut : cuts) {
      if (!cut.getCUT().isInterface()) {
        Set<Integer> linesCovered = testCases.stream().map(testCase -> testCase.getAllLinesCovered(cut))
            .collect(new HashSetCollector<>());
        Set<Integer> linesMissed = cut.getCoverableLines().stream().filter(line -> !linesCovered.contains(line))
            .collect(HashSet::new, HashSet::add, HashSet::addAll);
        Set<Integer> branchesCovered = testCases.stream().map(testCase -> testCase.getAllBranchesCovered(cut))
            .collect(new HashSetCollector<>());
        Set<Integer> branchesMissed = cut.getCoverableBranches().stream().filter(branch -> !branchesCovered.contains(branch))
            .collect(HashSet::new, HashSet::add, HashSet::addAll);
        List<Integer> orderedLinesCovered = new ArrayList<Integer>(linesCovered);
        Collections.sort(orderedLinesCovered);
        List<Integer> orderedLinesMissed = new ArrayList<Integer>(linesMissed);
        Collections.sort(orderedLinesMissed);
        double percentageCoverage = cut.getTotalLines() > 0 ? (double) linesCovered.size() / cut.getTotalLines() : 0;
        double percentageBranch = cut.getTotalBranches() > 0 ? (double) (branchesCovered.size() / cut.getTotalBranches()) : 0;
        String[] csv = new String[] { cut.getCUT().getName(), Integer.toString(linesCovered.size()),
            Integer.toString(linesMissed.size()),
                linesCovered.size() > 0 ? orderedLinesCovered.stream().map(line -> Integer.toString(line)).reduce((lineNumber, lineNumber2) -> lineNumber+":"+lineNumber2).get() : "",
                linesMissed.size() > 0 ? orderedLinesMissed.stream().map(line -> Integer.toString(line)).reduce((lineNumber, lineNumber2) -> lineNumber+":"+lineNumber2).get() : "",
                Double.toString(percentageCoverage),
            Integer.toString(cut.getTotalBranches()),
            Double.toString(branchesCovered.size()),
            Double.toString(branchesMissed.size()),
            Double.toString(percentageBranch) };
        addRow(csv);
      }
    }
  }

}