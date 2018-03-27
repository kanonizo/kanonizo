package org.kanonizo.algorithms.heuristics;

import org.kanonizo.algorithms.AbstractSearchAlgorithm;
import org.kanonizo.algorithms.MutationSearchAlgorithm;
import org.kanonizo.algorithms.metaheuristics.fitness.FEPTotalFitnessFunction;
import org.kanonizo.algorithms.metaheuristics.fitness.FitnessFunction;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.mutation.Mutation;

public class TotalFEPAlgorithm extends AbstractSearchAlgorithm implements MutationSearchAlgorithm {

  @Override
  protected void generateSolution() {
    TestSuite opt = getCurrentOptimal();
    Mutation.initialise(opt);
  }

  @Override
  public FitnessFunction<SystemUnderTest> getFitnessFunction() {
    return new FEPTotalFitnessFunction(problem);
  }

  @Override
  public String readableName() {
    return "totalfep";
  }
}
