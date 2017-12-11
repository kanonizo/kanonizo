package org.kanonizo.algorithms;

import org.kanonizo.algorithms.metaheuristics.fitness.FitnessFunction;
import org.kanonizo.framework.TestSuiteChromosome;

public interface MutationSearchAlgorithm {
  public FitnessFunction<TestSuiteChromosome> getFitnessFunction();
}
