package org.kanonizo.algorithms;

import java.util.ArrayList;
import java.util.List;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;

public abstract class TestCasePrioritiser extends AbstractSearchAlgorithm {

  @Override
  protected final void generateSolution() {
    init();
    TestSuite suite = problem.clone().getTestSuite();
    List<TestCase> testCases = suite.getTestCases();
    List<TestCase> orderedTestCases = new ArrayList<>();
    while (!testCases.isEmpty() && !shouldFinish()) {
      TestCase tc = selectTestCase(testCases);
      testCases.remove(tc);
      orderedTestCases.add(tc);
      fw.getDisplay().fireTestCaseSelected(tc);
      fw.getDisplay().reportProgress(orderedTestCases.size(), testCases.size() + orderedTestCases.size());
    }
    if (!testCases.isEmpty()) {
      orderedTestCases.addAll(testCases);
    }
    suite.setTestCases(orderedTestCases);
    fw.getDisplay().fireTestSuiteChange(suite);
    setCurrentOptimal(suite);
  }
  public void init(){

  }
  public abstract TestCase selectTestCase(List<TestCase> testCases);
}
