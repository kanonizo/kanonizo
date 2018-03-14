package org.kanonizo.reporting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.kanonizo.Framework;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Branch;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.util.HashSetCollector;

public class CoverageWriter extends CsvWriter {
  private SystemUnderTest system;

  public CoverageWriter(SystemUnderTest system) {
    this.system = system;
  }

  @Override
  public String getDir() {
    return "coverage";
  }

  @Override
  public void prepareCsv() {
    String[] headers = new String[] { "Class", "ClassId", "NumLinesCovered", "NumLinesMissed", "LinesCovered", "LinesMissed", "PercentageLineCoverage",
        "Total Branches", "BranchesCovered", "BranchesMissed", "PercentageBranchCoverage" };
    setHeaders(headers);
    List<ClassUnderTest> cuts = system.getClassesUnderTest();
    List<TestCase> testCases = system.getTestSuite().getTestCases();
    Instrumenter inst = Framework.getInstance().getInstrumenter();
    for (ClassUnderTest cut : cuts) {
      if (!cut.getCUT().isInterface()) {
        Set<Line> linesCovered = new HashSet<>();
        Set<Branch> branchesCovered = new HashSet<>();
        for(TestCase tc : testCases){
          Set<Line> lines = inst.getLinesCovered(tc).stream().filter(line -> line.getParent().equals(cut)).collect(Collectors.toSet());
          Set<Branch> branches = inst.getBranchesCovered(tc);
          linesCovered.addAll(lines);
          branchesCovered.addAll(branches);
        }
        int totalLines = inst.getTotalLines(cut);
        int totalBranches = inst.getTotalBranches(cut);
        Set<Line> linesMissed = cut.getLines().stream().filter(line -> !linesCovered.contains(line))
            .collect(HashSet::new, HashSet::add, HashSet::addAll);
        Set<Branch> branchesMissed = cut.getBranches().stream()
            .filter(branch -> !branchesCovered.contains(branch))
            .collect(HashSet::new, HashSet::add, HashSet::addAll);
        List<Line> orderedLinesCovered = new ArrayList<>(linesCovered);
        Collections.sort(orderedLinesCovered);
        List<Line> orderedLinesMissed = new ArrayList<Line>(linesMissed);
        Collections.sort(orderedLinesMissed);
        double percentageCoverage = totalLines > 0 ? (double) linesCovered.size() / totalLines : 0;
        double percentageBranch = totalBranches > 0 ? (double) branchesCovered.size() / totalBranches : 0;
        String[] csv = new String[] { cut.getCUT().getName(), Integer.toString(cut.getId()), Integer.toString(linesCovered.size()),
            Integer.toString(linesMissed.size()),
                linesCovered.size() > 0 ? orderedLinesCovered.stream().map(line -> Integer.toString(line.getLineNumber())).reduce((lineNumber, lineNumber2) -> lineNumber+":"+lineNumber2).get() : "",
                linesMissed.size() > 0 ? orderedLinesMissed.stream().map(line -> Integer.toString(line.getLineNumber())).reduce((lineNumber, lineNumber2) -> lineNumber+":"+lineNumber2).get() : "",
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