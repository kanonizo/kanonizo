package org.kanonizo.reporting;

import org.kanonizo.Framework;
import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.algorithms.TestCasePrioritiser;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.listeners.TestCaseSelectionListener;

public class TestCaseOrderingWriter extends CsvWriter implements TestCaseSelectionListener {
  private SearchAlgorithm algorithm;
  private Instrumenter inst;
  private boolean finalWrite;
  protected TestCaseOrderingWriter() {
  }

  public TestCaseOrderingWriter(SearchAlgorithm algorithm) {
    super();
    this.algorithm = algorithm;

    finalWrite = !(algorithm instanceof TestCasePrioritiser);
    inst = Framework.getInstance().getInstrumenter();
    Framework.getInstance().addSelectionListener(this);
    setHeaders(
        new String[]{"TestCase", "ExecutionTime", "Passed", "TotalLinesCovered"});
  }

  @Override
  protected void prepareCsv() {
    if(finalWrite) {
      TestSuite optimal = algorithm.getCurrentOptimal();

      optimal.getTestCases().forEach(testCase -> {
        String[] csv = new String[]{testCase.toString(),
            Long.toString(testCase.getExecutionTime()), Boolean.toString(!testCase.hasFailures()),
            Integer.toString(inst.getLinesCovered(testCase).size())};
        addRow(csv);
      });
    }
  }

  @Override
  public String getDir() {
    return "ordering";
  }

  @Override
  public void testCaseSelected(TestCase tc) {
    String[] csv = new String[]{tc.toString(),
        Long.toString(tc.getExecutionTime()), Boolean.toString(!tc.hasFailures()),
        Integer.toString(inst.getLinesCovered(tc).size())};
    writeRow(csv);
  }
}
