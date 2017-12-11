package org.kanonizo.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.kanonizo.util.ArrayStringCollector;
import com.scythe.instrumenter.instrumentation.objectrepresentation.Branch;
import com.scythe.instrumenter.instrumentation.objectrepresentation.Line;

public class TestCaseExecutionData {
  private double branchCoverage;
  private double lineCoverage;
  private List<Line> linesCovered;
  private List<Branch> branchesFullyCovered;
  private List<Branch> branchesPartiallyCovered;

  public TestCaseExecutionData(double branchCoverage, double lineCoverage, List<Line> linesCovered,
      List<Branch> branchesFullyCovered, List<Branch> branchesPartiallyCovered) {
    this.branchCoverage = branchCoverage;
    this.lineCoverage = lineCoverage;
    this.linesCovered = linesCovered;
    this.branchesFullyCovered = branchesFullyCovered;
    this.branchesPartiallyCovered = branchesPartiallyCovered;
  }

  public double getBranchCoverage() {
    return branchCoverage;
  }

  public double getLineCoverage() {
    return lineCoverage;
  }

  public List<Line> getLinesCovered() {
    return Collections.unmodifiableList(linesCovered);
  }

  public List<Branch> getBranchesPartiallyCovered() {
    return new ArrayList<>(branchesPartiallyCovered);
  }

  public List<Branch> getBranchesFullyCovered() {
    return new ArrayList<>(branchesFullyCovered);
  }

  @Override
  public TestCaseExecutionData clone() {
    TestCaseExecutionData clone = new TestCaseExecutionData(branchCoverage, lineCoverage, new ArrayList<Line>(),
        new ArrayList<Branch>(), new ArrayList<Branch>());
    clone.linesCovered.addAll(linesCovered);
    clone.branchesPartiallyCovered.addAll(branchesPartiallyCovered);
    clone.branchesFullyCovered.addAll(branchesFullyCovered);
    return clone;
  }

  @Override
  public String toString() {
    return "Coverage achieved: \nBranch Coverage - " + getBranchCoverage() + "\nLine Coverage - " + getLineCoverage()
        + "\nLines Covered - " + linesCovered.stream().map(Line::getLineNumber).map(i -> Integer.toString(i))
            .collect(new ArrayStringCollector());
  }

}