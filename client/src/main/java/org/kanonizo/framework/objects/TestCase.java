package org.kanonizo.framework.objects;

import com.scythe.instrumenter.InstrumentationProperties;
import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import com.scythe.instrumenter.analysis.task.AbstractTask;
import com.scythe.instrumenter.analysis.task.Task;
import com.scythe.instrumenter.analysis.task.TaskTimer;
import java.io.File;
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
import org.junit.runner.Description;
import org.kanonizo.Framework;
import org.kanonizo.framework.TestCaseStore;
import org.kanonizo.junit.KanonizoTestFailure;
import org.kanonizo.junit.KanonizoTestResult;
import org.kanonizo.junit.TestingUtils;
import org.kanonizo.junit.runners.JUnit3TestRunner;
import org.kanonizo.junit.runners.JUnit4TestRunner;
import org.kanonizo.junit.runners.KanonizoTestRunner;

public class TestCase {
  @Parameter(key = "timeout", description = "Test cases can in some cases run infinitely. The timeout property allows the user to define a point at which to cut off long running test cases. The use of this property is controlled by Properties.USE_TIMEOUT", category = "TCP")
  public static int TIMEOUT = 100000;

  @Parameter(key = "use_timeout", description = "Whether or not to use the test case timeout defined by Properties.TIMEOUT. Since for deterministic test cases we should not be expecting any infinite loops, it becomes less likely that timeouts will be hit", category = "TCP")
  public static boolean USE_TIMEOUT = true;

  @Parameter(key = "execute_in_root_folder", description = "Test cases by default execute in the directory from which kanonizo is executed from. Using this flag forces them to execute in the project root. Ensure project root is not null for correct results", category = "Test Case")
  public static boolean EXECUTE_IN_ROOT_FOLDER = false;

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
    File rootFolder;
    if (EXECUTE_IN_ROOT_FOLDER && (rootFolder = Framework.getInstance().getRootFolder()) != null) {
      System.setProperty("user.dir", rootFolder.getAbsolutePath());
    }
    KanonizoTestRunner testCaseRunner;
    if (TestingUtils.isJUnit4Class(testClass)) {
      testCaseRunner = new JUnit4TestRunner(this);
    } else {
      testCaseRunner = new JUnit3TestRunner();
    }
    KanonizoTestResult result = null;
    if (USE_TIMEOUT) {
      ExecutorService service = Executors.newSingleThreadExecutor();
      Future<KanonizoTestResult> res = service.submit(() -> testCaseRunner.runTest(this));
      try {
        result = res.get(TIMEOUT, UNIT);
      } catch (TimeoutException e) {
        logger.debug("Test " + testMethod.getName() + " timed out.");
        return;
      } catch (InterruptedException e) {
        logger.error(e);
      } catch (ExecutionException e) {
        logger.error(e);
      }
    } else {
      result = testCaseRunner.runTest(this);
    }
    setResult(result);
    if (InstrumentationProperties.LOG) {
      TaskTimer.taskEnd(timerTask);
    }
  }

  public void setResult(KanonizoTestResult result) {
    this.result = result;
  }

  public boolean hasFailures() {
    return result != null && result.getFailures().size() > 0;
  }

  public boolean isSuccessful(){
    return result != null && result.isSuccessful();
  }

  public List<KanonizoTestFailure> getFailures() {
    if (result == null) {
      logger.info("No result for test case " + this);
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(result.getFailures());
  }

  public long getExecutionTime() {
    if (result == null) {
      // deserialised test case maybe?
      return -1;
    }
    return result.getExecutionTime();
  }

  public Class<?> getTestClass() {
    return testClass;
  }

  public String getTestClassName() {
    return testClass.getSimpleName();
  }

  public Method getMethod() {
    return testMethod;
  }

  public String getMethodName() {
    return testMethod.getName();
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
    return Description.createTestDescription(testClass, testMethod.getName()).toString();
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
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
