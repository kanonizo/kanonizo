package org.kanonizo.reporting;

import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.framework.TestSuiteChromosome;

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
        new String[] { "TestCase", "ExecutionTime", "Passed" });
    TestSuiteChromosome optimal = algorithm.getCurrentOptimal();
    optimal.getTestCases().forEach(testCase -> {
      String[] csv = new String[] { testCase.getTestClass().getName() + "." + testCase.getMethod().getName(),
          Long.toString(testCase.getExecutionTime()), Boolean.toString(testCase.getFailures().size() == 0) };
      addRow(csv);
    });
  }

  @Override
  public String getDir() {
    return "ordering";
  }
}
