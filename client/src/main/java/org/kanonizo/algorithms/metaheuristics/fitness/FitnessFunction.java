package org.kanonizo.algorithms.metaheuristics.fitness;

import org.kanonizo.Disposable;
import org.kanonizo.framework.Chromosome;

public interface FitnessFunction<T extends Chromosome> extends Disposable {

  double evaluateFitness();

  FitnessFunction<T> clone(T chr);

  T getChromosome();

  default boolean isMaximisationFunction() {
    return false;
  }
}
