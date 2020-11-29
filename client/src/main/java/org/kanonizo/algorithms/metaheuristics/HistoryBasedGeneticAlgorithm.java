package org.kanonizo.algorithms.metaheuristics;

import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.display.Display;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Population;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.listeners.TestOrderChangedListener;

import java.util.List;

public class HistoryBasedGeneticAlgorithm extends GeneticAlgorithm
{
    public HistoryBasedGeneticAlgorithm(
            KanonizoConfigurationModel configurationModel,
            List<TestOrderChangedListener> testOrderChangedListeners,
            Instrumenter instrumenter,
            Display display
    )
    {
        super(configurationModel, testOrderChangedListeners, instrumenter, display);
    }

    @Override
    protected Population<TestSuite> generateInitialPopulation()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Population<TestSuite> evolve()
    {
        throw new UnsupportedOperationException();
    }

    public String readableName()
    {
        return "historyga";
    }
}
