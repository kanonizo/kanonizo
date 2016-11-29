package com.dpaterson.testing.algorithms.metaheuristics.fitness;

import com.dpaterson.testing.Disposable;
import com.dpaterson.testing.framework.Chromosome;

public interface FitnessFunction<T extends Chromosome> extends Disposable {

  double evaluateFitness();

  FitnessFunction<T> clone(T chr);

  T getChromosome();

  default boolean isMaximisationFunction() {
    return false;
  }
}
