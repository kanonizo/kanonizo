package org.kanonizo.algorithms.metaheuristics.fitness;

import org.kanonizo.Disposable;
import org.kanonizo.framework.objects.SystemUnderTest;

public interface FitnessFunction<T> extends Disposable {

  double evaluateFitness();

  FitnessFunction<T> clone(SystemUnderTest sut);

  T getSystem();

  default boolean isMaximisationFunction() {
    return false;
  }
}
