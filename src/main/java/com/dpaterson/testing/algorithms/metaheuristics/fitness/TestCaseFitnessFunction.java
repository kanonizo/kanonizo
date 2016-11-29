package com.dpaterson.testing.algorithms.metaheuristics.fitness;

import com.dpaterson.testing.framework.TestCaseChromosome;

public interface TestCaseFitnessFunction {
  double getTestCaseFitness(TestCaseChromosome c);
}
