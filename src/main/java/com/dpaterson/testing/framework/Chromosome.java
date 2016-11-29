package com.dpaterson.testing.framework;

/**
 * This class is intended to be a base class for the problem representation, including methods for guiding searches in the case of metaheuristic algorithms
 */
public abstract class Chromosome {

  public abstract Chromosome mutate();

  public abstract void crossover(Chromosome chr, int point1, int point2);

  public void crossover(Chromosome chr, int point1) {
    crossover(chr, point1, point1);
  }

  public double getFitness() {
    return 0.0;
  }

  public abstract int size();

  @Override
  public abstract Chromosome clone();
}
