package org.kanonizo.algorithms.heuristics;

import org.kanonizo.algorithms.AbstractSearchAlgorithm;
import org.kanonizo.algorithms.metaheuristics.fitness.FEPTotalFitnessFunction;
import org.kanonizo.algorithms.metaheuristics.fitness.FitnessFunction;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.display.Display;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.listeners.TestOrderChangedListener;
import org.kanonizo.mutation.Mutation;

import java.util.List;

public class TotalFEPAlgorithm extends AbstractSearchAlgorithm
{

    public TotalFEPAlgorithm(
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
        TestSuite opt = getCurrentOptimal();
        Mutation.initialise(opt);
        return opt;
    }

    @Override
    public boolean providesFitnessFunction()
    {
        return true;
    }

    public FitnessFunction<SystemUnderTest> getFitnessFunction()
    {
        return new FEPTotalFitnessFunction(problem);
    }

    @Override
    public String readableName()
    {
        return "totalfep";
    }
}
