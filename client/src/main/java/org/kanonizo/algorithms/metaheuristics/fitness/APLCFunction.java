package org.kanonizo.algorithms.metaheuristics.fitness;

import java.util.Set;
import org.kanonizo.Framework;
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
    return Framework.getInstrumenter().getLinesCovered(tc);
  }

  @Override
  protected Set<? extends Goal> getGoals() {
    return sut.getClassesUnderTest().stream().map(cut -> Framework.getInstrumenter().getLines(cut)).collect(new HashSetCollector<Line>());
  }

  @Override
  public FitnessFunction<SystemUnderTest> clone(SystemUnderTest sut) {
    APLCFunction clone = new APLCFunction(sut);
    clone.coveredGoals = coveredGoals;
    return clone;
  }

  @Override
  protected void calculateTotalGoalsCovered() {
    coveredGoals = Framework.getInstrumenter().getLinesCovered(sut).size();
  }
}