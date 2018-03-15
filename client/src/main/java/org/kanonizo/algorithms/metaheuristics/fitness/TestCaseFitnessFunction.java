package org.kanonizo.algorithms.metaheuristics.fitness;

import org.kanonizo.framework.objects.TestCase;

public interface TestCaseFitnessFunction {
  double getTestCaseFitness(TestCase c);
}
