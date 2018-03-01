package org.kanonizo.junit.runners;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.kanonizo.framework.objects.ParameterisedTestCase;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.junit.KanonizoTestFailure;
import org.kanonizo.junit.KanonizoTestResult;

public class JUnit4TestRunner implements KanonizoTestRunner {

  private final Logger logger = LogManager.getLogger(JUnit4TestRunner.class);
  private JUnitCore runner = new JUnitCore();

  @Override
  public KanonizoTestResult runTest(TestCase tc) {
    Request request = getRequest(tc);
    Runner testRunner = request.getRunner();
    Result testResult = runner.run(testRunner);
    List<KanonizoTestFailure> failures = testResult.getFailures().stream()
        .map(failure -> new KanonizoTestFailure(failure.getException()))
        .collect(Collectors.toList());
    return new KanonizoTestResult(tc.getTestClass(), tc.getMethod(), testResult.wasSuccessful(),
        failures, testResult.getRunTime());
  }

  private Request getRequest(TestCase tc) {
    Class<?> testClass = tc.getTestClass();
    Method testMethod = tc.getMethod();
    try {
      final RunWith runWith = testClass.getAnnotation(RunWith.class);
      if (runWith != null) {
        final Class<? extends Runner> runnerClass = runWith.value();
        if (runnerClass.isAssignableFrom(Parameterized.class)) {
          try {
            if(tc instanceof ParameterisedTestCase){
              ParameterisedTestCase ptc = (ParameterisedTestCase) tc;
              Class.forName(
                  "org.junit.runners.BlockJUnit4ClassRunner"); //ignore IgnoreIgnored for junit4.4 and <
              return Request.runner(new ParameterizedMethodRunner(testClass, testMethod.getName(), ptc.getParameters()));
            }

          } catch (Throwable thrown) {
            logger.error(thrown);
          }
        }
      } else {
        if (testMethod != null
            && testMethod.getAnnotation(Ignore.class) != null) { //override ignored case only
          final Request classRequest = new ClassRequest(testClass) {
            public Runner getRunner() {
              try {
                return new IgnoreIgnoredTestJUnit4ClassRunner(testClass);
              } catch (Exception ignored) {
              }
              return super.getRunner();
            }
          };
          return classRequest
              .filterWith(Description.createTestDescription(testClass, testMethod.getName()));
        }
      }
    } catch (Exception ignored) {
      logger.error(ignored);
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


  private static class ParameterizedMethodRunner extends Parameterized {

    private final String myMethodName;
    private final Object[] parameters;
    public ParameterizedMethodRunner(Class clazz, String methodName, Object[] parameters) throws Throwable {
      super(clazz);
      myMethodName = methodName;
      this.parameters = parameters;
    }

    protected List getChildren() {
      final List children = new ArrayList<>(super.getChildren());
      for(Iterator<?> it = children.iterator(); it.hasNext();){
        try {
          final BlockJUnit4ClassRunnerWithParameters child = (BlockJUnit4ClassRunnerWithParameters) it.next();
          final Field testParameterField = BlockJUnit4ClassRunnerWithParameters.class.getDeclaredField("parameters");
          testParameterField.setAccessible(true);
          final Object[] testParameters = (Object[]) testParameterField.get(child);
          if(!Arrays.equals(testParameters, parameters)){
            it.remove();
          }
          child.filter(new Filter(){

            @Override
            public boolean shouldRun(Description description) {
              String methodName = description.getMethodName();
              methodName = methodName.substring(0, methodName.indexOf('['));
              return methodName.equals(myMethodName);
            }

            @Override
            public String describe() {
              return getDescription().getDisplayName();
            }

          });
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      return children;
    }
  }
}
