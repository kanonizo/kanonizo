package org.kanonizo.algorithms.metaheuristics.fitness;

import org.kanonizo.Disposable;

public interface FitnessFunction<T> extends Disposable {

  double evaluateFitness();

  FitnessFunction<T> clone(T chr);

  T getSystem();

  default boolean isMaximisationFunction() {
    return false;
  }
}
