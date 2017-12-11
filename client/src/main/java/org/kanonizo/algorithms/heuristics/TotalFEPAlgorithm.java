package org.kanonizo.algorithms.heuristics;

import org.kanonizo.algorithms.AbstractSearchAlgorithm;
import org.kanonizo.algorithms.MutationSearchAlgorithm;
import org.kanonizo.algorithms.metaheuristics.fitness.FEPTotalFitnessFunction;
import org.kanonizo.algorithms.metaheuristics.fitness.FitnessFunction;
import org.kanonizo.framework.TestSuiteChromosome;
import org.kanonizo.mutation.Mutation;

public class TotalFEPAlgorithm extends AbstractSearchAlgorithm implements MutationSearchAlgorithm {

  @Override
  protected void generateSolution() {
    TestSuiteChromosome opt = getCurrentOptimal();
    Mutation.initialise(opt);
  }

  @Override
  public FitnessFunction<TestSuiteChromosome> getFitnessFunction() {
    return new FEPTotalFitnessFunction(getCurrentOptimal());
  }

}
