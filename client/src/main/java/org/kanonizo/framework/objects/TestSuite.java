package org.kanonizo.framework.objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.Disposable;
import org.kanonizo.Properties;
import org.kanonizo.algorithms.metaheuristics.fitness.FitnessFunction;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.configuration.configurableoption.DoubleOption;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.util.HashSetCollector;
import org.kanonizo.util.RandomSource;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.kanonizo.configuration.configurableoption.DoubleOption.doubleOption;

public class TestSuite implements Comparable<TestSuite>, Disposable
{
    private static final DoubleOption REMOVAL_CHANCE_OPTION = doubleOption("removal_chance", 0d);
    private static final DoubleOption INSERTION_CHANCE_OPTION = doubleOption("insertion_chance", 0d);
    private static final DoubleOption REORDER_CHANCE_OPTION = doubleOption("reorder_chance", 1d);
    private static final Logger logger = LogManager.getLogger(TestSuite.class);

    private final SystemUnderTest parent;
    private final Instrumenter instrumenter;
    private final double removalChance;
    private final double insertionChance;
    private final double reorderChance;
    private final List<TestCase> testCases = new LinkedList<>();
    private final List<TestCase> removedTestCases = new ArrayList<>();
    private final FitnessFunction<SystemUnderTest> fitnessFunction;

    private double fitness;
    private int fitnessEvaluations;
    private boolean changed = false;

    public TestSuite(
            SystemUnderTest parent,
            FitnessFunction<SystemUnderTest> fitnessFunction,
            KanonizoConfigurationModel configurationModel,
            Instrumenter instrumenter
    )
    {
        this.parent = parent;
        this.fitnessFunction = fitnessFunction;
        this.removalChance = configurationModel.getDoubleOption(REMOVAL_CHANCE_OPTION);
        this.insertionChance = configurationModel.getDoubleOption(INSERTION_CHANCE_OPTION);
        this.reorderChance = configurationModel.getDoubleOption(REORDER_CHANCE_OPTION);
        this.instrumenter = instrumenter;
    }

    private TestSuite(TestSuite existing)
    {
        this.parent = existing.parent;
        this.removalChance = existing.removalChance;
        this.insertionChance = existing.insertionChance;
        this.reorderChance = existing.reorderChance;
        this.instrumenter = existing.instrumenter;
        this.testCases.addAll(existing.testCases);
        this.fitness = existing.fitness;
        this.fitnessEvaluations = existing.fitnessEvaluations;
        this.changed = existing.changed;
        this.fitnessFunction = existing.fitnessFunction.clone();
        this.disposed = existing.disposed;
    }

    public SystemUnderTest getParent()
    {
        return parent;
    }

    public int size()
    {
        return testCases.size();
    }

    public List<TestCase> getTestCases()
    {
        return new ArrayList<>(testCases);
    }

    public void clear()
    {
        testCases.clear();
    }

    public List<Integer> getIds()
    {
        return testCases.stream().map(TestCase::getId).collect(toList());
    }


    public void addAll(List<TestCase> testCases)
    {
        testCases.forEach(this::addTestCase);
    }

    public void addTestCase(TestCase tc)
    {
        if (Modifier.isAbstract(tc.getMethod().getModifiers()))
        {
            logger.debug("Not adding " + tc + " because it is not runnable");
        }
        else
        {
            this.testCases.add(tc);
        }
    }

    protected void setChanged(boolean changed)
    {
        this.changed = changed;
    }

    /**
     * This method will overwrite the current ordering of the test cases in the class and as such should only be used from within the algorithm to define a new order of test cases. Usage of this method
     * can cause erroneous behaviour
     */
    public void setTestCases(List<TestCase> testCases)
    {
        this.testCases.clear();
        this.testCases.addAll(testCases);
        evaluateFitness();
    }

    public TestSuite mutate()
    {
        long startTime = java.lang.System.currentTimeMillis();
        TestSuite clone = SystemUnderTest.copyOf(parent).getTestSuite();
        if (RandomSource.nextDouble() < removalChance)
        {
            clone.removeTestCase();
        }
        if (RandomSource.nextDouble() < insertionChance)
        {
            clone.insertTestCase();
        }
        if (RandomSource.nextDouble() < reorderChance)
        {
            clone.reorderTestCase();
        }
        clone.setChanged(true);
        if (Properties.PROFILE)
        {
            logger.info("Mutation completed in: %s" + (java.lang.System.currentTimeMillis() - startTime) + "ms");
        }
        return clone;
    }

    private void removeTestCase()
    {
        int numCases = testCases.size();
        // cases are removed with probability 1/n where n is the number of
        // cases.
        List<TestCase> removedCases = testCases.stream().filter(tc -> RandomSource.nextDouble() < 1d / numCases)
                .collect(toList());
        // ensure we aren't removing all cases
        if (testCases.size() - removedCases.size() > 0)
        {
            removedTestCases.addAll(removedCases);
            testCases.removeAll(removedCases);
        }
    }

    private void insertTestCase()
    {
        if (removedTestCases.size() > 0)
        {
            int numRemovedCases = removedTestCases.size();
            // cases are added with probability 1/n where n is the number of
            // missing cases
            List<TestCase> addedCases = removedTestCases.stream()
                    .filter(tc -> RandomSource.nextDouble() < 1d / numRemovedCases).collect(toList());
            testCases.addAll(addedCases);
            removedTestCases.removeAll(addedCases);
        }
    }

    private void reorderTestCase()
    {
        List<Integer> points = new ArrayList<>();
        double mutationChance = 2 * Properties.NUMBER_OF_MUTATIONS / (double) testCases.size();
        for (int i = 0; i < testCases.size(); i++)
        {
            if (RandomSource.nextDouble() <= mutationChance)
            {
                points.add(i);
            }
        }
        for (int i = 0; i < points.size() - 1; i += 2)
        {
            if (!(points.size() > i + 1))
            {
                break;
            }
            int point1 = points.get(i);
            int point2 = points.get(i + 1);
            TestCase tc1 = testCases.get(point1);
            TestCase tc2 = testCases.get(point2);
            testCases.remove(tc1);
            testCases.remove(tc2);
            testCases.add(point1, tc2);
            testCases.add(point2, tc1);
        }

    }

    public void crossover(TestSuite toBeCrossedWith, int point1, int point2)
    {
        long startTime = java.lang.System.currentTimeMillis();
        // crossover ordering according to Antonial et al
        List<TestCase> toBeRetained = testCases.subList(0, point1);
        testCases.clear();
        testCases.addAll(toBeRetained);
        List<TestCase> toBeAdded = toBeCrossedWith.testCases.stream().filter(testCase -> !testCases.contains(testCase)).collect(toList());
        testCases.addAll(toBeAdded);
        setChanged(true);
        if (Properties.PROFILE)
        {
            logger.info("Crossover completed in: %s ms", + (java.lang.System.currentTimeMillis() - startTime));
        }
    }

    public double getFitness()
    {
        return fitness;
    }

    void setFitness(double fitness)
    {
        this.fitness = fitness;
    }

    public void evolutionComplete()
    {
        if (changed)
        {
            evaluateFitness();
        }
    }

    /**
     * Used to set what should be stored in this classes {@link#fitness} variable, which will be returned when {@link#getFitness()} is called. This method by default delegates a call to the
     * {@link#getFitnessFunction()} method
     */
    protected void evaluateFitness()
    {
        long startTime = java.lang.System.currentTimeMillis();
        fitness = fitnessFunction.evaluateFitness();
        fitnessEvaluations++;
        setChanged(false);
        if (Properties.PROFILE)
        {
            java.lang.System.out.println("Fitness evaluation completed in: " + (java.lang.System.currentTimeMillis() - startTime) + "ms");
        }
    }

    public boolean contains(TestCase tc)
    {
        return testCases.stream().anyMatch(tc2 -> tc2.equals(tc));
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\n-------------------------------------------\nMAXIMUM FITNESS: ")
                .append(String.format("%.4f", getFitness()))
                .append("\n-------------------------------------------\n");
        Set<Line> covered = testCases.stream().map(instrumenter::getLinesCovered).collect(new HashSetCollector<>());
        int coveredBranches = covered.size();
        int totalBranches = parent.getClassesUnderTest().stream().mapToInt(instrumenter::getTotalLines).sum();
        sb.append("Line Coverage: ").append((double) coveredBranches / (double) totalBranches);
        sb.append("\n-------------------------------------------\nMaximum fitness found by ")
                .append(fitnessFunction.getClass().getSimpleName())
                .append("\n-------------------------------------------\n");
        return sb.toString();
    }


    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        TestSuite testSuite = (TestSuite) o;
        return Objects.equals(testCases, testSuite.testCases);
    }

    @Override
    public int hashCode()
    {
        int result = 0;
        for (TestCase t : testCases)
        {
            //independent of order
            result += t.hashCode();
        }
        return result;
    }

    @Override
    public int compareTo(TestSuite other)
    {
        if (fitness == other.fitness)
        {
            return 0;
        }
        return Double.compare(fitness, other.fitness);
    }

    public boolean isDisposed()
    {
        return disposed;
    }

    private boolean disposed = false;

    @Override
    public void dispose()
    {
        if (!disposed)
        {
            disposed = true;
            fitnessFunction.dispose();
            testCases.clear();
        }
    }

    public static TestSuite copyOf(TestSuite existing)
    {
        return new TestSuite(existing);
    }
}
