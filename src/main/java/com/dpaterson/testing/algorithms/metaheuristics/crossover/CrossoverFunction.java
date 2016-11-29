package com.dpaterson.testing.algorithms.metaheuristics.crossover;

import com.dpaterson.testing.framework.Chromosome;

public interface CrossoverFunction {
  public void crossover(Chromosome parent1, Chromosome parent2);
}
