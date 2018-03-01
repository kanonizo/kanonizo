package org.kanonizo.reporting;

import org.kanonizo.Framework;
import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.framework.objects.TestSuite;

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
        new String[]{"TestCase", "ExecutionTime", "Passed", "TotalLinesCovered"});
    TestSuite optimal = algorithm.getCurrentOptimal();
    optimal.getTestCases().forEach(testCase -> {
      String[] csv = new String[]{testCase.toString(),
          Long.toString(testCase.getExecutionTime()), Boolean.toString(!testCase.hasFailures()),
          Integer.toString(Framework.getInstrumenter().getLinesCovered(testCase).size())};
      addRow(csv);
    });
  }

  @Override
  public String getDir() {
    return "ordering";
  }
}
