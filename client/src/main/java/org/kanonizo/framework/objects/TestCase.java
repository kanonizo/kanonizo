package org.kanonizo.framework.objects;

import com.scythe.instrumenter.InstrumentationProperties;
import com.scythe.instrumenter.analysis.task.AbstractTask;
import com.scythe.instrumenter.analysis.task.Task;
import com.scythe.instrumenter.analysis.task.TaskTimer;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.Properties;
import org.kanonizo.framework.TestCaseStore;
import org.kanonizo.junit.KanonizoTestFailure;
import org.kanonizo.junit.KanonizoTestResult;
import org.kanonizo.junit.TestingUtils;
import org.kanonizo.junit.runners.JUnit3TestRunner;
import org.kanonizo.junit.runners.JUnit4TestRunner;
import org.kanonizo.junit.runners.KanonizoTestRunner;
import org.kanonizo.util.Util;

public class TestCase {
  private static final long TIMEOUT = Properties.TIMEOUT;
  private static final TimeUnit UNIT = TimeUnit.MILLISECONDS;
  private final Logger logger = LogManager.getLogger(TestCase.class);
  private Class<?> testClass;
  private Method testMethod;
  private int testSize;
  private static int count = 0;
  private KanonizoTestResult result;
  private int id;
  private TestSuite parent;

  public TestCase(Class<?> testClass, Method testMethod) {
    if (testClass == null || testMethod == null) {
      throw new IllegalArgumentException("Test Class and Test Method must not be null!");
    }
    this.testClass = testClass;
    this.testMethod = testMethod;
    this.id = ++count;
    TestCaseStore.register(id, this);
  }

  private TestCase() {
  }

  public void setParent(TestSuite parent) {
    this.parent = parent;
  }

  public int getId() {
    return id;
  }

  /**
   * Executes a single test method on the JUnitCore class, using default
   * Runners and configuration. This method must reload the class from the
   * class loader as it will have been instrumented since it is first created.
   * If the instrumented version is not loaded, code coverage goes a little
   * bit funky.
   *
   * @throws ClassNotFoundException if the ClassLoader can't find the {@link #testClass} by name
   */
  public void run() {
    long startTime = System.currentTimeMillis();
    // reload testclass from memory class loader to get the instrumented
    // version
    Task timerTask = new TestCaseExecutionTimer(testClass.getName(), testMethod.getName());
    if (InstrumentationProperties.LOG) {
      TaskTimer.taskStart(timerTask);
    }
    KanonizoTestRunner testCaseRunner = TestingUtils.isJUnit4Class(testClass) ? new JUnit4TestRunner() : new JUnit3TestRunner();
    if (Properties.USE_TIMEOUT) {
      ExecutorService service = Executors.newSingleThreadExecutor();
      Future<KanonizoTestResult> res = service.submit(() -> testCaseRunner.runTest(this));
      try {
        setResult(res.get(TIMEOUT, UNIT));
      } catch (TimeoutException e) {
        logger.debug("Test " + testMethod.getName() + " timed out.");
        return;
      } catch (InterruptedException e) {
        logger.error(e);
      } catch (ExecutionException e) {
        logger.error(e);
      }
    } else {
      KanonizoTestResult res = testCaseRunner.runTest(this);
      setResult(res);
    }
    if (InstrumentationProperties.LOG) {
      TaskTimer.taskEnd(timerTask);
    }
  }

  private void setResult(KanonizoTestResult result) {
    this.result = result;
  }

  public boolean hasFailures() {
    return result.getFailures().size() > 0;
  }

  public List<KanonizoTestFailure> getFailures() {
    return Collections.unmodifiableList(result.getFailures());
  }

  public long getExecutionTime() {
    return result.getExecutionTime();
  }

  public Class<?> getTestClass() {
    return testClass;

  }

  public Method getMethod() {
    return testMethod;
  }

  public double getSize() {
    return testSize;
  }

  public void setSize(int testSize) {
    this.testSize = testSize;
  }

  @Override
  public TestCase clone() {
    TestCase clone = new TestCase();
    clone.testMethod = testMethod;
    clone.testClass = testClass;
    clone.testSize = testSize;
    clone.id = id;
    clone.parent = parent;
    return clone;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null) {
      return false;
    }
    if (getClass() != other.getClass()) {
      return false;
    }
    TestCase otherTest = (TestCase) other;
    return testMethod.equals(otherTest.testMethod) && testClass.equals(otherTest.testClass);
  }

  public int hashCode() {
    int prime = 41;
    return prime * testClass.hashCode() * testMethod.hashCode();
  }

  @Override
  public String toString() {
    return testClass.getName() + "." + testMethod.getName() + Util.getSignature(testMethod);
  }

  private static final class TestCaseExecutionTimer extends AbstractTask {
    private String testClass;
    private String testMethod;

    private TestCaseExecutionTimer(String testClass, String testMethod) {
      this.testClass = testClass;
      this.testMethod = testMethod;
    }

    @Override
    public String asString() {
      return "Executing " + testClass + "." + testMethod;
    }

  }

}
