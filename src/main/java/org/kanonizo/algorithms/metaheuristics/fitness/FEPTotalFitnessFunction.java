package org.kanonizo.algorithms.metaheuristics.fitness;

import java.util.Comparator;

import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestSuiteChromosome;
import org.kanonizo.mutation.Mutation;

public class FEPTotalFitnessFunction
    implements TestCaseFitnessFunction, Comparator<TestCaseChromosome>, FitnessFunction<TestSuiteChromosome> {

  protected TestSuiteChromosome tsc;

  public FEPTotalFitnessFunction(TestSuiteChromosome tsc) {
    this.tsc = tsc;
  }

  @Override
  public double getTestCaseFitness(TestCaseChromosome c) {
    return Mutation.getKillMap().get(c).size();
  }

  @Override
  public int compare(TestCaseChromosome o1, TestCaseChromosome o2) {
    return Double.compare(getTestCaseFitness(o1), getTestCaseFitness(o2));
  }

  @Override
  public void dispose() {
  }

  @Override
  public double evaluateFitness() {
    return 0;
  }

  @Override
  public FitnessFunction<TestSuiteChromosome> clone(TestSuiteChromosome chr) {
    return null;
  }

  @Override
  public TestSuiteChromosome getChromosome() {
    return tsc;
  }

}
