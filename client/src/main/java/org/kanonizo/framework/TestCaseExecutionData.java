package org.kanonizo.framework;

import com.google.gson.Gson;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TestCaseExecutionData {
  private double branchCoverage;
  private double lineCoverage;
  private Map<String, Set<Integer>> linesCovered;
  private Map<String, Set<Integer>> branchesCovered;

  public TestCaseExecutionData(double branchCoverage, double lineCoverage, Map<String, Set<Integer>> linesCovered, Map<String, Set<Integer>> branchesCovered) {
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

  public Map<String, Set<Integer>> getLinesCovered() {
    return Collections.unmodifiableMap(linesCovered);
  }

  public Map<String, Set<Integer>> getBranchesCovered() {
    return Collections.unmodifiableMap(branchesCovered);
  }

  @Override
  public TestCaseExecutionData clone() {
    TestCaseExecutionData clone = new TestCaseExecutionData(branchCoverage, lineCoverage, new HashMap<>(),
        new HashMap<>());
    clone.linesCovered.putAll(linesCovered);
    clone.branchesCovered.putAll(branchesCovered);
    return clone;
  }

  @Override
  public String toString() {
    Gson gson = new Gson();
    return gson.toJson(this);
  }

}