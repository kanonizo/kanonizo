package org.kanonizo.algorithms.metaheuristics.fitness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.kanonizo.framework.CUTChromosome;
import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestSuiteChromosome;

public abstract class APFDFunction extends InstrumentedFitnessFunction {
  protected List<Integer> totalGoals;
  protected double coveredGoals = 0;
  protected TestSuiteChromosome chrom;

  public APFDFunction(TestSuiteChromosome chrom) {
    this.chrom = chrom;
    totalGoals = getGoals();
  }

  @Override
  protected abstract void calculateTotalGoalsCovered();

  protected Map<Integer, Map<Integer, Integer>> getGoalMap() {
    Map<Integer, Map<Integer, Integer>> goalMap = new HashMap<>();
    List<TestCaseChromosome> testCases = chrom.getTestCases();
    for (int i = 0; i < testCases.size(); i++) {
      final int ind = i;
      TestCaseChromosome tc = testCases.get(i);
      Map<CUTChromosome, List<Integer>> goalsCovered = getCoveredGoals(tc);
      goalsCovered.forEach((cut, goals) -> {
        int classId = cut.getId();
        if (goalMap.containsKey(classId)) {
          Map<Integer, Integer> cov = goalMap.get(classId);
          for (Integer goal : goals) {
            if (!cov.containsKey(goal)) {
              cov.put(goal, ind + 1);
            }
          }
        } else {
          Map<Integer, Integer> cov = new HashMap<>();
          for (Integer goal : goals) {
            cov.put(goal, ind + 1);
          }
          goalMap.put(classId, cov);
        }
      });
    }
    return goalMap;

  }

  protected double calculateTestCaseIndices() {
    Map<Integer, Map<Integer, Integer>> goalMap = getGoalMap();
    return goalMap.values().stream().mapToDouble(value -> value.values().stream().mapToInt(Integer::intValue).sum())
        .sum();
  }

  @Override
  public double evaluateFitness() {
    double apfd = calculateTestCaseIndices();
    double p = getP();
    double n = chrom.getTestCases().size();
    double m = totalGoals.size();
    m = Math.max(m, 1);
    return 1 - (p - apfd / (m * n) + p / (2 * n));
  }

  protected double getP() {
    return totalGoals.size() == 0 ? 0 : coveredGoals / totalGoals.size();
  }

  public abstract Map<CUTChromosome, List<Integer>> getCoveredGoals(TestCaseChromosome tc);

  protected abstract List<Integer> getGoals();

  @Override
  public void dispose() {
    totalGoals.clear();
    totalGoals = null;
  }

  @Override
  public TestSuiteChromosome getChromosome() {
    return chrom;
  }

}
