package org.kanonizo.junit.runners;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

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

public class JUnit3TestRunner extends TestRunner implements KanonizoTestRunner
{
    private static final Logger logger = LogManager.getLogger(JUnit3TestRunner.class);
    private final TestCase tc;

    public JUnit3TestRunner(TestCase tc)
    {
        super(NullPrintStream.instance);
        this.tc = tc;
    }

    @Override
    public KanonizoTestResult runTest()
    {
        Test methodSuite = createMethodSuite(tc.getTestClass(), tc.getMethod());
        if (methodSuite == null)
        {
            logger.error("Unable to create test case");
            throw new RuntimeException();
        }
        Instant startTime = Instant.now();
        TestResult result = doRun(methodSuite, false);
        Duration runTime = Duration.between(startTime, Instant.now());
        List<KanonizoTestFailure> failures = new ArrayList<>();
        Enumeration<TestFailure> testFailures = result.failures();
        Enumeration<TestFailure> errors = result.errors();
        List<TestFailure> allErrors = Util.combine(testFailures, errors);
        for (TestFailure f : allErrors)
        {
            failures.add(new KanonizoTestFailure(f.thrownException(), f.trace()));
        }
        return new KanonizoTestResult(
                tc.getTestClass().getName(),
                tc.getMethod().getName(),
                result.wasSuccessful(),
                failures,
                runTime.toMillis()
        );
    }

    private <T> Test createMethodSuite(Class<T> testClass, Method testMethod)
    {
        try
        {
            Optional<Constructor<T>> noArgsConstructor = Util.getConstructorWithParameterTypes(testClass);
            if (noArgsConstructor.isPresent())
            {
                return (Test) newInstance(noArgsConstructor.get()).orElseThrow(IllegalStateException::new);
            }
            Constructor<T> singleStringConstructor = Util.getConstructorWithParameterTypes(
                    testClass,
                    String.class
            ).orElseThrow(IllegalStateException::new);
            return (Test) newInstance(singleStringConstructor).orElseThrow(IllegalStateException::new);

        }
        catch (ClassCastException e1)
        {
            try
            {
                testClass.getMethod(testMethod.getName());
            }
            catch (NoSuchMethodException e2)
            {
                logger.error("Trying to invoke a test method that does not exist: " + testClass.getName() + "." + testMethod.getName());
            }

            return null;
        }
    }

    private <T> Optional<T> newInstance(Constructor<T> constructor, Object... args)
    {
        try
        {
            return Optional.of(constructor.newInstance(args));
        }
        catch (IllegalAccessException | InstantiationException | InvocationTargetException e)
        {
            e.printStackTrace();
        }
        return Optional.empty();
    }

}
