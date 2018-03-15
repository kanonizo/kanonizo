package org.kanonizo.junit;

import java.lang.reflect.Method;
import java.util.List;

public class KanonizoTestResult {
  private List<KanonizoTestFailure> failures;
  private boolean successful;
  private Class<?> testClass;
  private Method testMethod;
  private long executionTime;

  public KanonizoTestResult(Class<?> testClass, Method testMethod, boolean successful, List<KanonizoTestFailure> failures, long executionTime) {
    this.testClass = testClass;
    this.testMethod = testMethod;
    this.successful = successful;
    this.failures = failures;
    this.executionTime = executionTime;
  }

  public List<KanonizoTestFailure> getFailures() {
    return failures;
  }

  public boolean isSuccessful() {
    return successful;
  }

  public Class<?> getTestClass() {
    return testClass;
  }

  public Method getTestMethod() {
    return testMethod;
  }

  public long getExecutionTime() {
    return executionTime;
  }

}
