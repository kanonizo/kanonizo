package com.dpaterson.testing.algorithms.metaheuristics.fitness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dpaterson.testing.framework.CUTChromosome;
import com.dpaterson.testing.framework.TestCaseChromosome;
import com.dpaterson.testing.framework.TestSuiteChromosome;
import com.sheffield.instrumenter.analysis.ClassAnalyzer;
import com.sheffield.instrumenter.instrumentation.objectrepresentation.CoverableGoal;

public abstract class APFDFunction extends InstrumentedFitnessFunction {
  protected List<? extends CoverableGoal> totalGoals;
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
      Map<CUTChromosome, List<CoverableGoal>> goalsCovered = getCoveredGoals(tc);
      goalsCovered.forEach((cut, goals) -> {
        int classId = ClassAnalyzer.getClassId(cut.getCUT().getName());
        if (goalMap.containsKey(classId)) {
          Map<Integer, Integer> cov = goalMap.get(classId);
          for (CoverableGoal goal : goals) {
            if (!cov.containsKey(goal.getGoalId())) {
              cov.put(goal.getGoalId(), ind + 1);
            }
          }
        } else {
          Map<Integer, Integer> cov = new HashMap<>();
          for (CoverableGoal goal : goals) {
            cov.put(goal.getGoalId(), ind + 1);
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

  public abstract Map<CUTChromosome, List<CoverableGoal>> getCoveredGoals(TestCaseChromosome tc);

  protected abstract List<? extends CoverableGoal> getGoals();

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
