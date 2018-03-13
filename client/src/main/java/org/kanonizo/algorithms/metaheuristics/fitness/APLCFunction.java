package org.kanonizo.algorithms.metaheuristics.fitness;

import java.util.Set;
import org.kanonizo.Framework;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Goal;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.util.HashSetCollector;

public class APLCFunction extends APFDFunction {

  public APLCFunction(SystemUnderTest sut) {
    super(sut);
  }

  @Override
  public Set<? extends Goal> getCoveredGoals(TestCase tc) {
    return Framework.getInstance().getInstrumenter().getLinesCovered(tc);
  }

  @Override
  protected Set<? extends Goal> getGoals() {
    Instrumenter inst = Framework.getInstance().getInstrumenter();
    return sut.getClassesUnderTest().stream().map(cut -> inst.getLines(cut)).collect(new HashSetCollector<Line>());
  }

  @Override
  public FitnessFunction<SystemUnderTest> clone(SystemUnderTest sut) {
    APLCFunction clone = new APLCFunction(sut);
    clone.coveredGoals = coveredGoals;
    return clone;
  }

  @Override
  protected void calculateTotalGoalsCovered() {
    coveredGoals = Framework.getInstance().getInstrumenter().getLinesCovered(sut).size();
  }
}