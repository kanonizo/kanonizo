package org.kanonizo.algorithms.heuristics.historybased;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.util.Util;

@Algorithm
public class Elbaum extends HistoryBased {

  @Parameter(key = "time_since_last_failure", description = "Cut off point at which we consider a failure to be relevant.", category = "history")
  public static int timeLimit = 5;

  @Parameter(key = "new_test_case_executions", description = "Cut off point at which we consider a test case to be no longer new", category="history")
  public static int newTestCaseLimit = 5;

  Map<TestCase, Integer> priority;

  @Override
  public void init(List<TestCase> testCases){
    super.init(testCases);
    priority = new HashMap<>();
    for(TestCase tc : testCases) {
      if (getTimeSinceLastFailure(tc) <= timeLimit || getNumExecutions(tc) <= newTestCaseLimit) {
        priority.put(tc, 1);
      } else {
        priority.put(tc, 2);
      }
    }
    priority = Util.sortByValue(priority);
  }
  @Override
  public TestCase selectTestCase(List<TestCase> testCases) {
    TestCase tc = priority.keySet().iterator().next();
    priority.remove(tc);
    return tc;
  }

  @Override
  public String readableName() {
    return "elbaum";
  }
}
