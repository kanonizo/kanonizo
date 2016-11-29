package com.dpaterson.testing.algorithms;

import com.dpaterson.testing.algorithms.metaheuristics.fitness.FitnessFunction;
import com.dpaterson.testing.framework.TestSuiteChromosome;

public interface MutationSearchAlgorithm {
  public FitnessFunction<TestSuiteChromosome> getFitnessFunction();
}
