package org.kanonizo.junit.runners;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AssumptionViolatedException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.internal.requests.ClassRequest;
import org.junit.internal.runners.ErrorReportingRunner;
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
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.kanonizo.framework.objects.ParameterisedTestCase;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.junit.KanonizoTestFailure;
import org.kanonizo.junit.KanonizoTestResult;
import org.kanonizo.util.Util;

public class JUnit4TestRunner implements KanonizoTestRunner
{
    private static final List<TestClass> initialisedClasses = new ArrayList<>();
    private final Logger logger = LogManager.getLogger(JUnit4TestRunner.class);
    private final JUnitCore runner = new JUnitCore();
    private final TestCase testCase;

    public JUnit4TestRunner(TestCase tc)
    {
        this.testCase = tc;
    }

    @Override
    public KanonizoTestResult runTest()
    {
        Request request = getRequest(testCase);
        Runner testRunner = request.getRunner();
        Result testResult = runner.run(testRunner);
        List<KanonizoTestFailure> failures = testResult.getFailures().stream()
                .map(failure -> new KanonizoTestFailure(failure.getException(), failure.getTrace()))
                .collect(Collectors.toList());
        return new KanonizoTestResult(
                testCase.getTestClass().getName(),
                testCase.getMethod().getName(),
                testResult.wasSuccessful(),
                failures,
                testResult.getRunTime()
        );
    }

    private Request getRequest(TestCase tc)
    {
        Class<?> testClass = tc.getTestClass();
        Method testMethod = tc.getMethod();
        try
        {
            RunWith runWith = testClass.getAnnotation(RunWith.class);
            if (runWith != null)
            {
                Class<? extends Runner> runnerClass = runWith.value();
                if (runnerClass.isAssignableFrom(Parameterized.class))
                {
                    try
                    {
                        if (tc instanceof ParameterisedTestCase)
                        {
                            ParameterisedTestCase ptc = (ParameterisedTestCase) tc;
                            Class.forName(
                                    "org.junit.runners.BlockJUnit4ClassRunner"); //ignore IgnoreIgnored for junit4.4 and <
                            return Request.runner(new ParameterizedMethodRunner(
                                    testClass,
                                    testMethod.getName(),
                                    ptc.getParameters()
                            ));
                        }

                    }
                    catch (Throwable thrown)
                    {
                        logger.error(thrown);
                    }
                }
                else
                {
                    Optional<? extends Constructor<? extends Runner>> con = Util.getConstructorWithParameterTypes(
                            runnerClass,
                            Class.class
                    );
                    if (con.isPresent())
                    {
                        try
                        {
                            Runner runner = con.get().newInstance(testClass);
                            return Request.runner(runner).filterWith(Description.createTestDescription(
                                    testClass,
                                    testMethod.getName()
                            ));
                        }
                        catch (InvocationTargetException ignored)
                        {

                        }
                    }
                }
            }
            else
            {
                if (testMethod != null
                        && testMethod.getAnnotation(Ignore.class) != null)
                { //override ignored case only
                    Request classRequest = new ClassRequest(testClass)
                    {
                        public Runner getRunner()
                        {
                            try
                            {
                                return new IgnoreIgnoredTestJUnit4ClassRunner(testClass);
                            }
                            catch (Exception ignored)
                            {
                            }
                            return super.getRunner();
                        }
                    };
                    return classRequest
                            .filterWith(Description.createTestDescription(testClass, testMethod.getName()));
                }
            }
        }
        catch (Exception ignored)
        {
            logger.error(ignored);
        }
        return new Request()
        {
            @Override
            public Runner getRunner()
            {
                try
                {
                    return new BlockJUnit4ClassRunner(testCase.getTestClass())
                    {
                        @Override
                        protected Statement withBeforeClasses(Statement statement)
                        {
                            List<FrameworkMethod> beforeClass = getTestClass().getAnnotatedMethods(BeforeClass.class);
                            TestClass testClass = getTestClass();
                            if (beforeClass.size() > 0 && initialisedClasses.stream().noneMatch(tc -> tc.getJavaClass().equals(
                                    testClass.getJavaClass())))
                            {
                                initialisedClasses.add(testClass);
                                return super.withBeforeClasses(statement);
                            }
                            else
                            {
                                return statement;
                            }
                        }
                    };
                }
                catch (InitializationError e)
                {
                    return new ErrorReportingRunner(tc.getTestClass(), e);
                }
            }
        }.filterWith(Description.createTestDescription(testClass, testMethod.getName()));
    }


    private static class IgnoreIgnoredTestJUnit4ClassRunner extends BlockJUnit4ClassRunner
    {
        public IgnoreIgnoredTestJUnit4ClassRunner(Class clazz) throws Exception
        {
            super(clazz);
        }

        protected void runChild(FrameworkMethod method, RunNotifier notifier)
        {
            final Description description = describeChild(method);
            final EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);
            eachNotifier.fireTestStarted();
            try
            {
                methodBlock(method).evaluate();
            }
            catch (AssumptionViolatedException e)
            {
                eachNotifier.addFailedAssumption(e);
            }
            catch (Throwable e)
            {
                eachNotifier.addFailure(e);
            }
            finally
            {
                eachNotifier.fireTestFinished();
            }
        }
    }


    private static class ParameterizedMethodRunner extends Parameterized
    {
        private final String myMethodName;
        private final Object[] parameters;
        private static final Logger logger = LogManager.getLogger(ParameterizedMethodRunner.class);

        public ParameterizedMethodRunner(Class clazz, String methodName, Object[] parameters) throws Throwable
        {
            super(clazz);
            myMethodName = methodName;
            this.parameters = parameters;
        }

        protected List<Runner> getChildren()
        {
            List<Runner> children = new ArrayList<>(super.getChildren());
            for (Iterator<Runner> it = children.iterator(); it.hasNext(); )
            {
                try
                {
                    Object[] testParameters;
                    Runner child = it.next();
                    Class<?> childRunner = child.getClass();
                    Field parameterField = null;
                    try
                    {
                        parameterField = childRunner.getDeclaredField("fParameters");
                    }
                    catch (NoSuchFieldException e)
                    {
                        try
                        {
                            parameterField = childRunner.getDeclaredField("parameters");
                        }
                        catch (NoSuchFieldException e1)
                        {
                            logger.error("Couldn't find field fParameters or parameters in parameterised test class");
                        }
                    }
                    parameterField.setAccessible(true);
                    testParameters = (Object[]) parameterField.get(child);


                    if (!Arrays.deepEquals(testParameters, parameters))
                    {
                        it.remove();
                    }
                    ((ParentRunner) child).filter(new Filter()
                    {

                        @Override
                        public boolean shouldRun(Description description)
                        {
                            String methodName = description.getMethodName();
                            methodName = methodName.substring(0, methodName.indexOf('['));
                            return methodName.equals(myMethodName);
                        }

                        @Override
                        public String describe()
                        {
                            return getDescription().getDisplayName();
                        }

                    });
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            return children;
        }
    }
}
