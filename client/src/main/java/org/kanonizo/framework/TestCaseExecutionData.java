package org.kanonizo.framework;

import com.google.gson.Gson;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestCaseExecutionData {
  private double branchCoverage;
  private double lineCoverage;
  private Set<Integer> linesCovered;
  private Set<Integer> branchesCovered;

  public TestCaseExecutionData(double branchCoverage, double lineCoverage, Set<Integer> linesCovered, Set<Integer> branchesCovered) {
    this.branchCoverage = branchCoverage;
    this.lineCoverage = lineCoverage;
    this.linesCovered = linesCovered;
    this.branchesCovered = branchesCovered;
  }

  public double getBranchCoverage() {
    return branchCoverage;
  }

  public double getLineCoverage() {
    return lineCoverage;
  }

  public Set<Integer> getLinesCovered() {
    return Collections.unmodifiableSet(linesCovered);
  }

  public Set<Integer> getBranchesCovered() {
    return Collections.unmodifiableSet(branchesCovered);
  }

  @Override
  public TestCaseExecutionData clone() {
    TestCaseExecutionData clone = new TestCaseExecutionData(branchCoverage, lineCoverage, new HashSet<>(),
        new HashSet<>());
    clone.linesCovered.addAll(linesCovered);
    clone.branchesCovered.addAll(branchesCovered);
    return clone;
  }

  @Override
  public String toString() {
    Gson gson = new Gson();
    return gson.toJson(this);
  }

}