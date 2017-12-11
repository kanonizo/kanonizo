package org.kanonizo.algorithms.metaheuristics.fitness;

import org.kanonizo.framework.TestCaseChromosome;

public interface TestCaseFitnessFunction {
  double getTestCaseFitness(TestCaseChromosome c);
}
