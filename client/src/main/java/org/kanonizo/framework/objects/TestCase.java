package org.kanonizo.framework.objects;

import com.scythe.instrumenter.InstrumentationProperties;
import com.scythe.instrumenter.analysis.task.AbstractTask;
import com.scythe.instrumenter.analysis.task.Task;
import com.scythe.instrumenter.analysis.task.TaskTimer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.runner.Description;
import org.kanonizo.Framework;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.configuration.configurableoption.BooleanOption;
import org.kanonizo.configuration.configurableoption.IntOption;
import org.kanonizo.framework.TestCaseStore;
import org.kanonizo.junit.KanonizoTestFailure;
import org.kanonizo.junit.KanonizoTestResult;
import org.kanonizo.junit.runners.JUnit3TestRunner;
import org.kanonizo.junit.runners.JUnit4TestRunner;
import org.kanonizo.junit.runners.KanonizoTestRunner;
import org.kanonizo.util.Lazy;

import java.io.File;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.kanonizo.configuration.configurableoption.BooleanOption.booleanOption;
import static org.kanonizo.configuration.configurableoption.IntOption.intOption;
import static org.kanonizo.junit.TestingUtils.isJUnit4Class;

public class TestCase
{
    private static final IntOption TIMEOUT_OPTION = intOption("timeout", 100000);
    private static final BooleanOption USE_TEST_TIMEOUT_OPTION = booleanOption("use_timeout", true);
    private static final BooleanOption EXECUTE_IN_ROOT_FOLDER_OPTION = booleanOption("execute_in_root_folder", false);

    private static final ExecutorService TEST_CASE_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Logger logger = LogManager.getLogger(TestCase.class);
    private static final AtomicInteger TEST_ID_SUPPLIER = new AtomicInteger();

    private final Duration testTimeout;
    private final boolean executeTestsInRootFolder;
    private final Class<?> testClass;
    private final Method testMethod;
    private final int id;
    private final Lazy<KanonizoTestResult> resultSupplier;

    public TestCase(Class<?> testClass, Method testMethod, KanonizoConfigurationModel configurationModel)
    {
        if (testClass == null || testMethod == null)
        {
            throw new IllegalArgumentException("Test Class and Test Method must not be null!");
        }
        this.testClass = testClass;
        this.testMethod = testMethod;
        this.testTimeout = configurationModel.getBooleanOption(USE_TEST_TIMEOUT_OPTION) ?
                Duration.ofMillis(configurationModel.getIntOption(TIMEOUT_OPTION)) :
                Duration.ZERO;
        this.executeTestsInRootFolder = configurationModel.getBooleanOption(EXECUTE_IN_ROOT_FOLDER_OPTION);
        this.resultSupplier = Lazy.of(this::run);

        this.id = TEST_ID_SUPPLIER.incrementAndGet();
        TestCaseStore.register(id, this);
    }

    private TestCase(TestCase existing)
    {
        this.testClass = existing.testClass;
        this.testMethod = existing.testMethod;
        this.id = existing.id;
        this.testTimeout = existing.testTimeout;
        this.executeTestsInRootFolder = existing.executeTestsInRootFolder;
        this.resultSupplier = Lazy.of(this::run);
    }

    public TestCase(String className, String methodName, KanonizoTestResult testResult) throws ClassNotFoundException, NoSuchMethodException
    {
        this.testClass = Class.forName(className);
        this.testMethod = testClass.getMethod(methodName);
        this.id = TEST_ID_SUPPLIER.incrementAndGet();
        this.testTimeout = Duration.ZERO;
        this.executeTestsInRootFolder = false;
        this.resultSupplier = Lazy.of(() -> testResult);
    }

    public int getId()
    {
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
    public KanonizoTestResult run()
    {
        long startTime = System.currentTimeMillis();
        Task timerTask = new TestCaseExecutionTimer(testClass.getName(), testMethod.getName());
        if (InstrumentationProperties.LOG)
        {
            TaskTimer.taskStart(timerTask);
        }
        File rootFolder;
        if (executeTestsInRootFolder && (rootFolder = Framework.getInstance().getRootFolder()) != null)
        {
            System.setProperty("user.dir", rootFolder.getAbsolutePath());
        }
        KanonizoTestRunner testCaseRunner = isJUnit4Class(testClass) ?
                new JUnit4TestRunner(this) :
                new JUnit3TestRunner(this);
        Future<KanonizoTestResult> res = TEST_CASE_EXECUTOR.submit(testCaseRunner::runTest);
        try
        {
            return testTimeout.isZero() ?
                    res.get() :
                    res.get(testTimeout.toMillis(), MILLISECONDS);
        }
        catch (TimeoutException e)
        {
            logger.debug("Test " + testMethod.getName() + " timed out.");
        }
        catch (InterruptedException | ExecutionException e)
        {
            logger.error(e);
        }
        if (InstrumentationProperties.LOG)
        {
            TaskTimer.taskEnd(timerTask);
        }
        return null;
    }

    public boolean hasFailures()
    {
        return resultSupplier.get().getFailures().size() > 0;
    }

    public boolean wasSuccessful()
    {
        return resultSupplier.get().wasSuccessful();
    }

    public List<KanonizoTestFailure> getFailures()
    {
        if (resultSupplier.get() == null)
        {
            logger.info("No result for test case " + this);
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(resultSupplier.get().getFailures());
    }

    public long getExecutionTime()
    {
        if (resultSupplier.get() == null)
        {
            // deserialised test case maybe?
            return -1;
        }
        return resultSupplier.get().getExecutionTime();
    }

    public Class<?> getTestClass()
    {
        return testClass;
    }

    public Method getMethod()
    {
        return testMethod;
    }

    public String getMethodName()
    {
        return testMethod.getName();
    }

    @Override
    public TestCase clone()
    {
        return new TestCase(this);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (other == null)
        {
            return false;
        }
        if (getClass() != other.getClass())
        {
            return false;
        }
        TestCase otherTest = (TestCase) other;
        return testMethod.equals(otherTest.testMethod) && testClass.equals(otherTest.testClass);
    }

    public int hashCode()
    {
        int prime = 41;
        return prime * testClass.hashCode() * testMethod.hashCode();
    }

    @Override
    public String toString()
    {
        return Description.createTestDescription(testClass, testMethod.getName()).toString();
    }

    public static class Builder
    {
        private String className;
        private String methodName;
        private KanonizoTestResult testResult;

        public Builder withClassName(String className)
        {
            this.className = className;
            return this;
        }

        public Builder withMethodName(String methodName)
        {
            this.methodName = methodName;
            return this;
        }

        public Builder withTestResult(KanonizoTestResult testResult)
        {
            this.testResult = testResult;
            return this;
        }
        public TestCase build()
        {
            try
            {
                return new TestCase(className, methodName, testResult);
            }
            catch (ClassNotFoundException | NoSuchMethodException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        public static TestCase.Builder aTestCase()
        {
            return new TestCase.Builder();
        }
    }

    private static final class TestCaseExecutionTimer extends AbstractTask
    {
        private final String testClass;
        private final String testMethod;

        private TestCaseExecutionTimer(String testClass, String testMethod)
        {
            this.testClass = testClass;
            this.testMethod = testMethod;
        }

        @Override
        public String asString()
        {
            return "Executing " + testClass + "." + testMethod;
        }

    }

}
