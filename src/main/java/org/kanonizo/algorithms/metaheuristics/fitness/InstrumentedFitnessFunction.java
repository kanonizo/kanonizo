package org.kanonizo.algorithms.metaheuristics.fitness;

import org.kanonizo.framework.TestSuiteChromosome;
import org.kanonizo.framework.instrumentation.Instrumenter;

public abstract class InstrumentedFitnessFunction implements FitnessFunction<TestSuiteChromosome> {

  public void instrument(TestSuiteChromosome chrom) {
    Instrumenter.runTestCases(chrom.getSUT(), chrom);
    calculateTotalGoalsCovered();
  }

  protected abstract void calculateTotalGoalsCovered();
}
