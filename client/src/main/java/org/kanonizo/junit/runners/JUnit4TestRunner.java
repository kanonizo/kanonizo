package org.kanonizo.junit.runners;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AssumptionViolatedException;
import org.junit.Ignore;
import org.junit.internal.requests.ClassRequest;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.junit.KanonizoTestFailure;
import org.kanonizo.junit.KanonizoTestResult;

public class JUnit4TestRunner implements KanonizoTestRunner {
  private final Logger logger = LogManager.getLogger(JUnit4TestRunner.class);
  private JUnitCore runner = new JUnitCore();

  @Override
  public KanonizoTestResult runTest(TestCase tc) {
    Request request = getRequest(tc.getTestClass(), tc.getMethod());
    Runner testRunner = request.getRunner();
    Result testResult = runner.run(testRunner);
    List<KanonizoTestFailure> failures = testResult.getFailures().stream().map(failure -> new KanonizoTestFailure(failure.getException())).collect(Collectors.toList());
    return new KanonizoTestResult(tc.getTestClass(), tc.getMethod(), testResult.wasSuccessful(), failures, testResult.getRunTime());
  }

  private Request getRequest(Class<?> testClass, Method testMethod) {
    if (testClass.getAnnotation(RunWith.class) == null) { //do not override external runners
      try {
        Class.forName("org.junit.runners.BlockJUnit4ClassRunner"); //ignore IgnoreIgnored for junit4.4 and <
        if (testMethod != null && testMethod.getAnnotation(Ignore.class) != null) { //override ignored case only
          final Request classRequest = new ClassRequest(testClass) {
            public Runner getRunner() {
              try {
                return new IgnoreIgnoredTestJUnit4ClassRunner(testClass);
              } catch (Exception ignored) {
              }
              return super.getRunner();
            }
          };
          return classRequest.filterWith(Description.createTestDescription(testClass, testMethod.getName()));
        }
      } catch (Exception ignored) {
        logger.error(ignored);
      }
    }
    return Request.method(testClass, testMethod.getName());
  }

  private static class IgnoreIgnoredTestJUnit4ClassRunner extends BlockJUnit4ClassRunner

  {
    public IgnoreIgnoredTestJUnit4ClassRunner(Class clazz) throws Exception {
      super(clazz);
    }

    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
      final Description description = describeChild(method);
      final EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);
      eachNotifier.fireTestStarted();
      try {
        methodBlock(method).evaluate();
      } catch (AssumptionViolatedException e) {
        eachNotifier.addFailedAssumption(e);
      } catch (Throwable e) {
        eachNotifier.addFailure(e);
      } finally {
        eachNotifier.fireTestFinished();
      }
    }
  }

}
