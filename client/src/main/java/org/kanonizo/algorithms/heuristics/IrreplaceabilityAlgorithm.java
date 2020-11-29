package org.kanonizo.algorithms.heuristics;

import org.kanonizo.algorithms.AbstractSearchAlgorithm;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.display.Display;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.listeners.TestOrderChangedListener;

import java.util.List;

public class IrreplaceabilityAlgorithm extends AbstractSearchAlgorithm
{
    public IrreplaceabilityAlgorithm(
            KanonizoConfigurationModel configurationModel,
            List<TestOrderChangedListener> testOrderChangedListeners,
            Instrumenter instrumenter,
            Display display
    )
    {
        super(configurationModel, testOrderChangedListeners, instrumenter, display);
    }

    @Override
    public TestSuite generateSolution()
    {
        throw new UnsupportedOperationException();
    }

    public String readableName()
    {
        return "irreplaceable";
    }
}
