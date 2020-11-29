package org.kanonizo;

import org.kanonizo.algorithms.AbstractSearchAlgorithm;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.display.Display;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.listeners.TestOrderChangedListener;

import java.util.Collections;
import java.util.List;

/**
 * Created by davidpaterson on 16/12/2016.
 */
public class RandomAlgorithm extends AbstractSearchAlgorithm
{
    public RandomAlgorithm(
            KanonizoConfigurationModel configurationModel,
            List<TestOrderChangedListener> testOrderChangedListeners,
            Instrumenter instrumenter,
            Display display
    )
    {
        super(configurationModel, testOrderChangedListeners, instrumenter, display);
    }

    @Override
    protected TestSuite generateSolution()
    {
        TestSuite suite = problem.clone().getTestSuite();
        List<TestCase> testCases = suite.getTestCases();
        Collections.shuffle(testCases);
        suite.setTestCases(testCases);
        setCurrentOptimal(suite);
        fitnessEvaluations++;
        return suite;
    }

    @Override
    public boolean needsFitnessFunction()
    {
        return false;
    }

    @Override
    public String readableName()
    {
        return "random";
    }
}
