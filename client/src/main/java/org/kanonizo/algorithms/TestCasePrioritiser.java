package org.kanonizo.algorithms;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.stoppingconditions.StoppingCondition;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;

public abstract class TestCasePrioritiser extends AbstractSearchAlgorithm {
  private Logger logger = LogManager.getLogger(TestCasePrioritiser.class);
  protected Instrumenter inst;
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
      fw.notifyTestCaseSelection(tc);
      fw.getDisplay().reportProgress(orderedTestCases.size(), testCases.size() + orderedTestCases.size());
    }
    if (!testCases.isEmpty()) {
      StoppingCondition terminatingStoppingCondition = stoppingConditions.stream()
          .filter(cond -> cond.shouldFinish(this)).findFirst().get();
      logger.info("Algorithm terminated by "+terminatingStoppingCondition.getClass().getSimpleName());
      orderedTestCases.addAll(testCases);
    }
    suite.setTestCases(orderedTestCases);
    fw.getDisplay().fireTestSuiteChange(suite);
    setCurrentOptimal(suite);
  }
  public void init(){
    inst = Framework.getInstance().getInstrumenter();
  }
  public abstract TestCase selectTestCase(List<TestCase> testCases);
}
