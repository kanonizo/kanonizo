package org.kanonizo.algorithms.metaheuristics.fitness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.kanonizo.framework.objects.Goal;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;

public abstract class APFDFunction implements FitnessFunction {
  protected static HashMap<SystemUnderTest, Set<? extends Goal>> totalGoals = new HashMap<>();
  protected SystemUnderTest sut;
  protected static HashMap<SystemUnderTest, Double> coveredGoals = new HashMap<>();
  public APFDFunction(SystemUnderTest sut) {
    this.sut = sut;
    if(!coveredGoals.containsKey(sut)){
      coveredGoals.put(sut, calculateTotalGoalsCovered());
      totalGoals.put(sut, getGoals());
    }
  }

  protected abstract double calculateTotalGoalsCovered();

  protected Map<Goal, Integer> getGoalMap() {
    Map<Goal, Integer> goalMap = new HashMap<>();
    List<TestCase> testCases = sut.getTestSuite().getTestCases();
    for (int i = 0; i < testCases.size(); i++) {
      final int ind = i;
      TestCase tc = testCases.get(i);
      Set<? extends Goal> goalsCovered = getCoveredGoals(tc);
      goalsCovered.forEach(goal -> {
        if (!goalMap.containsKey(goal)) {
          goalMap.put(goal, ind + 1);
        }
      });
    }
    return goalMap;

  }

  protected double calculateTestCaseIndices() {
    Map<Goal, Integer> goalMap = getGoalMap();
    return goalMap.values().stream().mapToDouble(Integer::intValue).sum();
  }

  @Override
  public double evaluateFitness() {
    double apfd = calculateTestCaseIndices();
    double p = getP();
    double n = sut.getTestSuite().getTestCases().size();
    double m = totalGoals.get(sut).size();
    m = Math.max(m, 1);
    return 1 - (p - apfd / (m * n) + p / (2 * n));
  }

  protected double getP() {
    return (!totalGoals.containsKey(sut)) || totalGoals.get(sut).size() == 0 ? 0 : coveredGoals.get(sut) / totalGoals.get(sut).size();
  }

  public abstract Set<? extends Goal> getCoveredGoals(TestCase tc);

  protected abstract Set<? extends Goal> getGoals();

  @Override
  public void dispose() {
    totalGoals.clear();
    totalGoals = null;
  }

  @Override
  public SystemUnderTest getSystem() {
    return sut;
  }

}
