package org.kanonizo.algorithms.metaheuristics.crossover;

import org.kanonizo.framework.Chromosome;

public interface CrossoverFunction {
  public void crossover(Chromosome parent1, Chromosome parent2);
}
