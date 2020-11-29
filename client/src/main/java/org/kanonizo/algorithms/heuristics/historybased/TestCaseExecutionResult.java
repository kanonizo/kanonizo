package org.kanonizo.algorithms.heuristics.historybased;

class TestCaseExecutionResult
{
  public static TestCaseExecutionResult NULL_EXECUTION = new TestCaseExecutionResult(-1, true, null);
  private long executionTime;
  private boolean passed;
  private Throwable failureCause;

  public TestCaseExecutionResult(long executionTime, boolean passed, Throwable failureCause) {
    this.executionTime = executionTime;
    this.passed = passed;
    this.failureCause = failureCause;
  }

  public long getExecutionTime() {
    return executionTime;
  }

  public boolean isPassed() {
    return passed;
  }

  public Throwable getFailureCause() {
    return failureCause;
  }
}