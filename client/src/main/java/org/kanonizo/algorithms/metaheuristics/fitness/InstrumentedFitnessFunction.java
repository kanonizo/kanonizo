package org.kanonizo.algorithms.metaheuristics.fitness;

import org.kanonizo.Framework;
import org.kanonizo.framework.TestSuiteChromosome;
import org.kanonizo.framework.instrumentation.Instrumenter;

public abstract class InstrumentedFitnessFunction implements FitnessFunction<TestSuiteChromosome> {

  public void instrument(TestSuiteChromosome chrom) {
    Instrumenter inst = Framework.getInstrumenter();
    inst.setTestSuite(chrom);
    inst.collectCoverage();
    calculateTotalGoalsCovered();
  }

  protected abstract void calculateTotalGoalsCovered();
}
