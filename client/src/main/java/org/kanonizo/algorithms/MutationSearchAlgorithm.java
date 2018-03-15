package org.kanonizo.algorithms;

import org.kanonizo.algorithms.metaheuristics.fitness.FitnessFunction;
import org.kanonizo.framework.objects.SystemUnderTest;

public interface MutationSearchAlgorithm {
  public FitnessFunction<SystemUnderTest> getFitnessFunction();
}
