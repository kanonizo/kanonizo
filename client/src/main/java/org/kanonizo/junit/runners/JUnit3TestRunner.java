package org.kanonizo.junit.runners;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import junit.textui.TestRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.junit.KanonizoTestFailure;
import org.kanonizo.junit.KanonizoTestResult;
import org.kanonizo.util.NullPrintStream;
import org.kanonizo.util.Util;

public class JUnit3TestRunner extends TestRunner implements KanonizoTestRunner {
  private static Logger logger = LogManager.getLogger(JUnit3TestRunner.class);

  public JUnit3TestRunner() {
    super(NullPrintStream.instance);
  }

  @Override
  public KanonizoTestResult runTest(TestCase tc) {
    Test test = createMethodSuite(tc.getTestClass(), tc.getMethod());
    if (test == null) {
      logger.error("Unable to create test case");
      throw new RuntimeException();
    }
    long startTime = System.currentTimeMillis();
    TestResult result = doRun(test, false);
    long runTime = System.currentTimeMillis() - startTime;
    List<KanonizoTestFailure> failures = new ArrayList<>();
    Enumeration<TestFailure> testFailures = result.failures();
    Enumeration<TestFailure> errors = result.errors();
    List<TestFailure> allErrors = Util.combine(testFailures, errors);
    for(TestFailure f : allErrors){
      failures.add(new KanonizoTestFailure(f.thrownException(), f.trace()));
    }
    return new KanonizoTestResult(tc.getTestClass(), tc.getMethod(), result.wasSuccessful(), failures, runTime);
  }

  private static <T> Test createMethodSuite(Class<T> testClass, Method testMethod) {
    try {
      Constructor<T> con = Util.getConstructor(testClass, new Class[0]);
      if (con == null) {
        con = Util.getConstructor(testClass, new Class[]{String.class});
        if (con == null) {
          logger.error(testClass.getSimpleName() + " has no default or String constructor");
        }
        return (Test) con.newInstance(new Object[]{testMethod.getName()});
      } else {
        junit.framework.TestCase test = (junit.framework.TestCase) con.newInstance(new Object[0]);
        test.setName(testMethod.getName());
        return test;
      }

    } catch (ClassCastException e1) {
      boolean methodExists;
      try {
        //noinspection SSBasedInspection
        testClass.getMethod(testMethod.getName(), new Class[0]);
        methodExists = true;
      } catch (NoSuchMethodException e2) {
        methodExists = false;
      }
      if (!methodExists) {
        logger.error("Trying to invoke a test method that does not exist: " + testClass.getName() + "." + testMethod.getName());
      }
      return null;
    } catch (InstantiationException e1) {
      logger.error(JUnit3TestRunner.class.getSimpleName() + " was unable to instantiate a new instance of the test class" + Util.getName(testClass));
    } catch (IllegalAccessException e1) {
      logger.error(JUnit3TestRunner.class.getSimpleName() + " was unable to instantiate a new instance of the test class" + Util.getName(testClass));
    } catch (InvocationTargetException e1) {
      logger.error(JUnit3TestRunner.class.getSimpleName() + " was unable to instantiate a new instance of the test class" + Util.getName(testClass));
    }
    return null;
  }

}
