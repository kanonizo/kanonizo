package com.dpaterson.testing.reporting;

import com.dpaterson.testing.algorithms.SearchAlgorithm;
import com.dpaterson.testing.framework.TestSuiteChromosome;

public class TestCaseOrderingWriter extends CsvWriter {
  private SearchAlgorithm algorithm;

  protected TestCaseOrderingWriter() {
  }

  public TestCaseOrderingWriter(SearchAlgorithm algorithm) {
    this.algorithm = algorithm;
  }

  @Override
  protected void prepareCsv() {
    setHeaders(
        new String[] { "TestCase", "ExecutionTime", "Passed", "PercentageLinesCovered", "PercentageBranchesCovered" });
    TestSuiteChromosome optimal = algorithm.getCurrentOptimal();
    optimal.getTestCases().forEach(testCase -> {
      String[] csv = new String[] { testCase.getTestClass().getName() + "." + testCase.getMethod().getName(),
          Long.toString(testCase.getExecutionTime()), Boolean.toString(testCase.getFailures().size() == 0),
          Double.toString(optimal.getLineCoverage(testCase)), Double.toString(optimal.getBranchCoverage(testCase)) };
      addRow(csv);
    });
  }

  @Override
  public String getDir() {
    return "ordering";
  }
}
