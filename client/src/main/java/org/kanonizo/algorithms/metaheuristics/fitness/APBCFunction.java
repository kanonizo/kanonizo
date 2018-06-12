package org.kanonizo.algorithms.metaheuristics.fitness;

import java.util.Set;
import org.kanonizo.Framework;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Branch;
import org.kanonizo.framework.objects.Goal;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.util.HashSetCollector;

public class APBCFunction extends APFDFunction {

  private Instrumenter inst = Framework.getInstance().getInstrumenter();

  public APBCFunction(SystemUnderTest sut) {
    super(sut);
  }

  @Override
  public Set<? extends Goal> getCoveredGoals(TestCase tc) {
    return inst.getLinesCovered(tc);
  }

  @Override
  protected Set<? extends Goal> getGoals() {

    return sut.getClassesUnderTest().stream().map(cut -> inst.getBranches(cut)).collect(new HashSetCollector<Branch>());
  }

  @Override
  public FitnessFunction<SystemUnderTest> clone(SystemUnderTest sut) {
    APBCFunction clone = new APBCFunction(sut);
    clone.coveredGoals = coveredGoals;
    return clone;
  }

  @Override
  protected double calculateTotalGoalsCovered() {
    return inst.getBranchesCovered(sut).size();
  }

}
