package org.kanonizo.algorithms.metaheuristics.crossover;

import org.kanonizo.framework.Chromosome;
import org.kanonizo.util.RandomInstance;

public class SinglePointCrossover implements CrossoverFunction {

  @Override
  public void crossover(Chromosome parent1, Chromosome parent2) {
    if (parent1.size() < 2 || parent2.size() < 2) {
      return;
    }

    int point = RandomInstance.nextInt(Math.min(parent1.size(), parent2.size()) - 1) + 1;

    Chromosome t1 = parent1.clone();
    Chromosome t2 = parent2.clone();

    parent1.crossover(t2, point);
    parent2.crossover(t1, point);
  }

}
