package org.kanonizo.algorithms.heuristics;

import java.util.List;

import org.kanonizo.algorithms.TestCasePrioritiser;
import org.kanonizo.algorithms.heuristics.comparators.GreedyComparator;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.display.Display;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.listeners.TestOrderChangedListener;

public class GreedyAlgorithm extends TestCasePrioritiser
{
    private final GreedyComparator greedyComparator;

    public GreedyAlgorithm(
            KanonizoConfigurationModel configurationModel,
            List<TestOrderChangedListener> testOrderChangedListeners,
            Instrumenter instrumenter,
            Display display
    )
    {
        super(configurationModel, testOrderChangedListeners, instrumenter, display);
        greedyComparator = new GreedyComparator();
    }

    @Override
    public void init(List<TestCase> testCases)
    {
        super.init(testCases);
        testCases.sort(greedyComparator);
    }

    @Override
    public TestCase selectTestCase(List<TestCase> testCases)
    {
        return testCases.remove(0);
    }


    @Override
    public String readableName()
    {
        return "greedy";
    }
}
