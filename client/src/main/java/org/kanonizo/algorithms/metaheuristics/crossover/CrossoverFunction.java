package org.kanonizo.algorithms.metaheuristics.crossover;

import org.kanonizo.framework.objects.TestSuite;

public interface CrossoverFunction {
  public void crossover(TestSuite parent1, TestSuite parent2);
}
