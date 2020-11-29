package org.kanonizo.algorithms.heuristics;

import java.util.List;

import org.kanonizo.algorithms.TestCasePrioritiser;
import org.kanonizo.algorithms.heuristics.comparators.AdditionalGreedyComparator;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.display.Display;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.listeners.TestOrderChangedListener;

public class AdditionalGreedyAlgorithm extends TestCasePrioritiser
{
    private final AdditionalGreedyComparator additionalGreedyComparator = new AdditionalGreedyComparator();

    public AdditionalGreedyAlgorithm(
            KanonizoConfigurationModel configurationModel,
            List<TestOrderChangedListener> testOrderChangedListeners,
            Instrumenter instrumenter,
            Display display
    )
    {
        super(configurationModel, testOrderChangedListeners, instrumenter, display);
    }

    @Override
    public TestCase selectTestCase(List<TestCase> testCases)
    {
        testCases.sort(additionalGreedyComparator);
        return testCases.remove(0);
    }

    @Override
    public String readableName()
    {
        return "additionalgreedy";
    }
}
