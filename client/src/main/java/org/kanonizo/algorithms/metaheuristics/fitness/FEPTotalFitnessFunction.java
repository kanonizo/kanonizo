package org.kanonizo.algorithms.metaheuristics.fitness;

import java.util.Comparator;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.mutation.Mutation;

public class FEPTotalFitnessFunction
    implements TestCaseFitnessFunction, Comparator<TestCase>, FitnessFunction<SystemUnderTest> {

  protected SystemUnderTest sut;

  public FEPTotalFitnessFunction(SystemUnderTest sut) {
    this.sut = sut;
  }

  @Override
  public double getTestCaseFitness(TestCase c) {
    return Mutation.getKillMap().get(c).size();
  }

  @Override
  public int compare(TestCase o1, TestCase o2) {
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
  public FitnessFunction<SystemUnderTest> clone(SystemUnderTest chr) {
    return null;
  }

  @Override
  public SystemUnderTest getSystem() {
    return sut;
  }


}
