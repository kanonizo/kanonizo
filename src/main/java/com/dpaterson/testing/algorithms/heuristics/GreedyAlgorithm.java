package com.dpaterson.testing.algorithms.heuristics;

import java.util.Collections;
import java.util.List;

import com.dpaterson.testing.algorithms.AbstractSearchAlgorithm;
import com.dpaterson.testing.algorithms.metaheuristics.fitness.APFDFunction;
import com.dpaterson.testing.framework.TestCaseChromosome;

public class GreedyAlgorithm extends AbstractSearchAlgorithm {

  @Override
  public void generateSolution() {
    List<TestCaseChromosome> testCases = problem.getTestCases();
    Collections.sort(testCases, new FitnessComparator());
    problem.setTestCases(testCases);
    fitnessEvaluations++;
  }

  @Override
  public double getFitness(TestCaseChromosome chr) {
    return ((APFDFunction) problem.getFitnessFunction()).getCoveredGoals(chr).values().stream().mapToInt(List::size)
        .sum();
  }

}
