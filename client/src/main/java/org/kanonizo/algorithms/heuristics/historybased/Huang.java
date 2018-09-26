package org.kanonizo.algorithms.heuristics.historybased;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junit.framework.AssertionFailedError;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.framework.objects.TestCase;

@Algorithm
public class Huang extends HistoryBased {

  private List<TestCase> failingTestCases = new ArrayList<>();

  public void init(List<TestCase> testCases){
    super.init(testCases);
    // find the most recent execution of test cases containing at least one failure
    int executionNo = getTimeSinceLastFailure();
    // find the list of test cases that failed in the most recent execution with failures
    failingTestCases = getFailingTestCases(executionNo);
    // sort by severity of failure
    Collections.sort(failingTestCases, (o1, o2) -> {
        Throwable cause1 = getCause(o1, executionNo);
        Throwable cause2 = getCause(o2, executionNo);
        return Integer.compare(getSeverity(cause1), getSeverity(cause2));
    });
  }

  private int getSeverity(Throwable cause){
    if(cause.getClass().equals(NullPointerException.class)){
      return 100;
    } else if (cause.getClass().equals(AssertionFailedError.class)){
      return 10;
    } else {
      return 1;
    }
  }

  @Override
  public TestCase selectTestCase(List<TestCase> testCases) {
    if (failingTestCases.size() > 0) {
      TestCase t = failingTestCases.get(0);
      failingTestCases.remove(t);
      return t;
    }
    return testCases.get(0);
  }

  @Override
  public String readableName() {
    return "huang";
  }
}
