package com.dpaterson.testing.algorithms.heuristics;

import com.dpaterson.testing.algorithms.AbstractSearchAlgorithm;
import com.dpaterson.testing.algorithms.MutationSearchAlgorithm;
import com.dpaterson.testing.algorithms.metaheuristics.fitness.FEPTotalFitnessFunction;
import com.dpaterson.testing.algorithms.metaheuristics.fitness.FitnessFunction;
import com.dpaterson.testing.framework.TestSuiteChromosome;
import com.dpaterson.testing.mutation.Mutation;

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
