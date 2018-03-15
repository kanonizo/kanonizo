package org.kanonizo.algorithms.metaheuristics.selection;

import java.util.List;

import org.kanonizo.util.RandomInstance;

public class RandomSelection<T> implements SelectionFunction<T> {

  @Override
  public int getIndex(List<T> population) {
    return RandomInstance.nextInt(population.size());
  }
}
