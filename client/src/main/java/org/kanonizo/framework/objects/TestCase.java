package org.kanonizo.framework.objects;

import com.scythe.instrumenter.InstrumentationProperties;
import com.scythe.instrumenter.analysis.ClassAnalyzer;
import com.scythe.instrumenter.analysis.task.AbstractTask;
import com.scythe.instrumenter.analysis.task.Task;
import com.scythe.instrumenter.analysis.task.TaskTimer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.kanonizo.Properties;
import org.kanonizo.framework.TestCaseStore;

public class TestCase {
  private static final long TIMEOUT = Properties.TIMEOUT;
  private static final TimeUnit UNIT = TimeUnit.MILLISECONDS;
  private Class<?> testClass;
  private Method testMethod;
  private int testSize;
  private static JUnitCore core = new JUnitCore();
  private long executionTime;
  private List<Failure> failures = new ArrayList<>();
  private static int count = 0;
  private int id;
  private Result result;
  private TestSuite parent;

  public TestCase(){
    this.id= ++count;
    TestCaseStore.register(id, this);
  }


  public void setParent(TestSuite parent){
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
    if (Properties.USE_TIMEOUT) {
      Request req = Request.method(testClass, testMethod.getName());
      ExecutorService service = Executors.newSingleThreadExecutor();
      Future<Result> res = service.submit(new Callable<Result>() {
        @Override
        public Result call() {
          return core.run(req);
        }
      });
      try {
        setResult(res.get(TIMEOUT, UNIT));
      } catch (TimeoutException e) {
        ClassAnalyzer.out.println("Test " + testMethod.getName() + " timed out.");
        return;
      } catch (InterruptedException e) {
        e.printStackTrace(ClassAnalyzer.out);
      } catch (ExecutionException e) {
        e.printStackTrace(ClassAnalyzer.out);
      }
    } else {
      Request r = Request.method(testClass, testMethod.getName());
      Result res = core.run(r);
      setResult(res);
    }
    if (InstrumentationProperties.LOG) {
      TaskTimer.taskEnd(timerTask);
    }
    if (result.getFailureCount() > 0) {
      failures.addAll(result.getFailures());
    }
    executionTime = System.currentTimeMillis() - startTime;
  }

  private void setResult(Result result) {
    this.result = result;
  }

  public boolean hasFailures() {
    return failures.size() > 0;
  }

  public List<Failure> getFailures() {
    return Collections.unmodifiableList(failures);
  }

  public long getExecutionTime() {
    return executionTime;
  }

  public void setTestClass(Class<?> testClass) {
    this.testClass = testClass;
  }

  public Class<?> getTestClass() {
    return testClass;

  }

  public void setMethod(Method method) {
    this.testMethod = method;
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
    return other instanceof TestCase && testClass.equals(((TestCase) other).testClass)
        && testMethod.equals(((TestCase) other).testMethod);
  }

  @Override
  public String toString() {
    return testClass.getName() + "." + testMethod.getName();
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
