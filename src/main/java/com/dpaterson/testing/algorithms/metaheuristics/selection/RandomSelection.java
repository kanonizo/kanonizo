package com.dpaterson.testing.algorithms.metaheuristics.selection;

import java.util.List;

import com.dpaterson.testing.util.RandomInstance;

public class RandomSelection<T> implements SelectionFunction<T> {

  @Override
  public int getIndex(List<T> population) {
    return RandomInstance.nextInt(population.size());
  }
}
