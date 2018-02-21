package org.kanonizo.algorithms.heuristics;

import org.kanonizo.Framework;
import org.kanonizo.algorithms.AbstractSearchAlgorithm;
import org.kanonizo.algorithms.metaheuristics.fitness.APFDFunction;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.framework.TestCaseChromosome;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Algorithm(readableName = "greedy")
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
    return (Framework.getInstrumenter().getLinesCovered(chr).values().stream().mapToInt(Set::size)
        .sum());
  }

}
