package com.dpaterson.testing.algorithms.metaheuristics.fitness;

import com.dpaterson.testing.framework.TestSuiteChromosome;
import com.dpaterson.testing.framework.instrumentation.Instrumenter;

public abstract class InstrumentedFitnessFunction implements FitnessFunction<TestSuiteChromosome> {

  public void instrument(TestSuiteChromosome chrom) {
    Instrumenter.runTestCases(chrom.getSUT(), chrom);
    calculateTotalGoalsCovered();
  }

  protected abstract void calculateTotalGoalsCovered();
}
