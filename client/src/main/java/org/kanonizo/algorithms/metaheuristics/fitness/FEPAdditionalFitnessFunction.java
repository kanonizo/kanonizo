package org.kanonizo.algorithms.metaheuristics.fitness;

import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;

public class FEPAdditionalFitnessFunction extends FEPTotalFitnessFunction
{

    public FEPAdditionalFitnessFunction(SystemUnderTest sut)
    {
        super(sut);
    }

    @Override
    public double getTestCaseFitness(TestCase c)
    {
        return 0;
    }

}
