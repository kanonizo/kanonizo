package com.dpaterson.testing.algorithms.metaheuristics.fitness;

import com.dpaterson.testing.framework.TestCaseChromosome;
import com.dpaterson.testing.framework.TestSuiteChromosome;

public class FEPAdditionalFitnessFunction extends FEPTotalFitnessFunction {

  public FEPAdditionalFitnessFunction(TestSuiteChromosome tsc) {
    super(tsc);
  }

  @Override
  public double getTestCaseFitness(TestCaseChromosome c) {
    return 0;
  }

}
