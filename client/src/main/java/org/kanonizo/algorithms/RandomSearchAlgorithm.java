package org.kanonizo.algorithms;

import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.display.Display;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Population;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.listeners.TestOrderChangedListener;
import org.kanonizo.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

import static org.kanonizo.framework.objects.Population.singleton;
import static org.kanonizo.framework.objects.SystemUnderTest.copyOf;

public class RandomSearchAlgorithm extends TestSuitePrioritiser
{
    public RandomSearchAlgorithm(
            KanonizoConfigurationModel configurationModel,
            List<TestOrderChangedListener> testOrderChangedListeners,
            Instrumenter instrumenter,
            Display display
    )
    {
        super(
                configurationModel,
                testOrderChangedListeners,
                instrumenter,
                display
        );
    }

    @Override
    protected Population<TestSuite> generateInitialPopulation()
    {
        return singleton(problem.getTestSuite());
    }

    @Override
    protected Population<TestSuite> evolve()
    {
        TestSuite clone = copyOf(getCurrentOptimal().getParent()).getTestSuite();
        List<TestCase> testCases = clone.getTestCases();
        List<TestCase> randomOrdering = generateRandomOrder(testCases);
        clone.setTestCases(randomOrdering);
        if (clone.getFitness() > getCurrentOptimal().getFitness())
        {
            return Population.singleton(clone);
        }
        else
        {
            return Population.singleton(getCurrentOptimal());
        }
    }

    private List<TestCase> generateRandomOrder(List<TestCase> testCases)
    {
        List<TestCase> unorderedCases = new ArrayList<>(testCases);
        List<TestCase> orderedCases = new ArrayList<>();
        while (unorderedCases.size() > 0)
        {
            int index = RandomSource.nextInt(unorderedCases.size());
            TestCase chr = unorderedCases.get(index);
            orderedCases.add(chr);
            unorderedCases.remove(chr);
        }
        return orderedCases;
    }

    @Override
    public String readableName()
    {
        return "randomsearch";
    }
}
