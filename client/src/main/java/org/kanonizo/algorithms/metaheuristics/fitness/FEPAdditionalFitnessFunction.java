package org.kanonizo.algorithms.metaheuristics.fitness;

import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestSuiteChromosome;

public class FEPAdditionalFitnessFunction extends FEPTotalFitnessFunction {

  public FEPAdditionalFitnessFunction(TestSuiteChromosome tsc) {
    super(tsc);
  }

  @Override
  public double getTestCaseFitness(TestCaseChromosome c) {
    return 0;
  }

}
