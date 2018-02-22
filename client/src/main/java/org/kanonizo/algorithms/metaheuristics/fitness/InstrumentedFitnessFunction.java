package org.kanonizo.algorithms.metaheuristics.fitness;

import org.kanonizo.Framework;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestSuite;

public abstract class InstrumentedFitnessFunction implements FitnessFunction<SystemUnderTest> {

  public void instrument(TestSuite chrom) {
    Instrumenter inst = Framework.getInstrumenter();
    inst.setTestSuite(chrom);
    inst.collectCoverage();
    calculateTotalGoalsCovered();
  }

  protected abstract void calculateTotalGoalsCovered();
}
