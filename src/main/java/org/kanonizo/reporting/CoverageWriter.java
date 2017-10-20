package org.kanonizo.reporting;

import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.framework.CUTChromosome;
import org.kanonizo.framework.Chromosome;
import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestSuiteChromosome;
import org.kanonizo.util.HashSetCollector;
import com.scythe.instrumenter.instrumentation.objectrepresentation.Branch;
import com.scythe.instrumenter.instrumentation.objectrepresentation.Line;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Set<Line> linesCovered = testCases.stream().map(testCase -> testCase.getAllLinesCovered(cut))
            .collect(new HashSetCollector<>());
        Set<Line> linesMissed = cut.getCoverableLines().stream().filter(line -> !linesCovered.contains(line))
            .collect(HashSet::new, HashSet::add, HashSet::addAll);

        Set<Branch> branchesFullyCovered = new HashSet<>();
        HashMap<Branch, Boolean> branchesPartiallyCovered = new HashMap<>();
        for (TestCaseChromosome testCase : testCases) {
          for (Branch b : testCase.getAllBranchesFullyCovered(cut)) {
            branchesFullyCovered.add(b);
            branchesPartiallyCovered.remove(b);
          }
          for (Branch b : testCase.getAllBranchesPartiallyCovered(cut)) {
            if (!branchesFullyCovered.contains(b)) {
              if (!branchesPartiallyCovered.containsKey(b)) {
                branchesPartiallyCovered.put(b, b.getTrueHits() > 0);
              } else {
                if (branchesPartiallyCovered.get(b) && b.getFalseHits() > 0) {
                  branchesPartiallyCovered.remove(b);
                  branchesFullyCovered.add(b);
                } else if (!branchesPartiallyCovered.get(b) && b.getTrueHits() > 0) {
                  branchesPartiallyCovered.remove(b);
                  branchesFullyCovered.add(b);
                }
              }
            }
          }
        }
        Set<Branch> branchesMissed = cut.getCoverableBranches().stream()
            .filter(branch -> !branchesFullyCovered.contains(branch) && !branchesPartiallyCovered.containsKey(branch))
            .collect(HashSet::new, HashSet::add, HashSet::addAll);
        double percentageCoverage = (double) linesCovered.size() / cut.getTotalLines();
        double percentageBranch = (double) (2 * branchesFullyCovered.size() + branchesPartiallyCovered.size())
            / (cut.getTotalBranches());
        String[] csv = new String[] { cut.getCUT().getName(), Integer.toString(linesCovered.size()),
            Integer.toString(linesMissed.size()),
                linesCovered.size() > 0 ? linesCovered.stream().map(line -> Integer.toString(line.getLineNumber())).reduce((lineNumber, lineNumber2) -> lineNumber+":"+lineNumber2).get() : "",
                linesMissed.size() > 0 ? linesMissed.stream().map(line -> Integer.toString(line.getLineNumber())).reduce((lineNumber, lineNumber2) -> lineNumber+":"+lineNumber2).get() : "",
                Double.toString(percentageCoverage),
            Integer.toString(cut.getTotalBranches()),
            Double.toString((2 * branchesFullyCovered.size() + branchesPartiallyCovered.size())),
            Double.toString((2 * branchesMissed.size() + branchesPartiallyCovered.size())),
            Double.toString(percentageBranch) };
        addRow(csv);
      }
    }
  }

}