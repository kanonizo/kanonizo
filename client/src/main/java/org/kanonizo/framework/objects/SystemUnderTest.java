package org.kanonizo.framework.objects;

import org.apache.commons.collections4.CollectionUtils;
import org.kanonizo.Properties;
import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.algorithms.metaheuristics.fitness.FitnessFunction;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.framework.instrumentation.Instrumenter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.kanonizo.Properties.COVERAGE_APPROACH;

public class SystemUnderTest implements Cloneable, TestCaseContainer
{
    private final List<ClassUnderTest> classesUnderTest = new ArrayList<>();
    private final List<Class<?>> extraClasses = new ArrayList<>();
    private final TestSuite testSuite;

    public SystemUnderTest(
            KanonizoConfigurationModel configModel,
            Instrumenter instrumenter,
            SearchAlgorithm algorithm
    )
    {
        this.testSuite = new TestSuite(this, getFitnessFunction(instrumenter, algorithm), configModel, instrumenter);
    }

    private SystemUnderTest(SystemUnderTest existing)
    {
        this.testSuite = TestSuite.copyOf(existing.testSuite);
        this.classesUnderTest.addAll(existing.classesUnderTest);
        this.extraClasses.addAll(existing.extraClasses);
    }

    protected FitnessFunction<SystemUnderTest> getFitnessFunction(Instrumenter instrumenter, SearchAlgorithm algorithm)
    {
        if (algorithm.providesFitnessFunction())
        {
            return algorithm.getFitnessFunction();
        }

        Properties.CoverageApproach.FitnessFunctionFactory<SystemUnderTest> fitnessFunctionFactory = COVERAGE_APPROACH.getFitnessFunctionFactory();
        return fitnessFunctionFactory.from(instrumenter, this);
    }

    public void addClass(ClassUnderTest cut)
    {
        cut.setParent(this);
        classesUnderTest.add(cut);
    }

    public TestSuite getTestSuite()
    {
        return testSuite;
    }

    public void addExtraClass(Class<?> extra)
    {
        this.extraClasses.add(extra);
    }

    public List<ClassUnderTest> getClassesUnderTest()
    {
        return Collections.unmodifiableList(classesUnderTest);
    }

    public int size()
    {
        return classesUnderTest.size();
    }

    public static SystemUnderTest copyOf(SystemUnderTest existing)
    {
        return new SystemUnderTest(existing);
    }

    @Override
    public int hashCode()
    {
        int result = classesUnderTest.hashCode();
        result = 31 * result + extraClasses.hashCode();
        result = 31 * result + testSuite.hashCode();
        return result;
    }

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
        if (other.getClass() != getClass())
        {
            return false;
        }
        SystemUnderTest otherSUT = (SystemUnderTest) other;
        List<TestCase> testCases = otherSUT.testSuite.getTestCases();
        return CollectionUtils.isEqualCollection(otherSUT.classesUnderTest, classesUnderTest) &&
                CollectionUtils.isEqualCollection(testCases, testSuite.getTestCases());

    }

    @Override
    public List<TestCase> getTestCases()
    {
        return getTestSuite().getTestCases();
    }
}
