package com.dpaterson.testing.algorithms.metaheuristics.fitness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dpaterson.testing.framework.CUTChromosome;
import com.dpaterson.testing.framework.TestCaseChromosome;
import com.dpaterson.testing.framework.TestSuiteChromosome;
import com.sheffield.instrumenter.instrumentation.objectrepresentation.CoverableGoal;

public class APLCFunction extends APFDFunction {

  public APLCFunction(TestSuiteChromosome chr) {
    super(chr);
  }

  @Override
  public Map<CUTChromosome, List<CoverableGoal>> getCoveredGoals(TestCaseChromosome tc) {
    Map<CUTChromosome, List<CoverableGoal>> returnMap = new HashMap<>();
    tc.getLineNumbersCovered().entrySet().forEach(entry -> {
      List<CoverableGoal> goals = new ArrayList<>();
      goals.addAll(entry.getValue());
      returnMap.put(entry.getKey(), goals);
    });
    return returnMap;
  }

  @Override
  protected List<? extends CoverableGoal> getGoals() {
    return chrom.getSUT().getClassesUnderTest().stream().map(CUTChromosome::getCoverableLines).flatMap(List::stream)
        .collect(Collectors.toList());
  }

  @Override
  public FitnessFunction<TestSuiteChromosome> clone(TestSuiteChromosome chr) {
    APLCFunction clone = new APLCFunction(chr);
    clone.coveredGoals = coveredGoals;
    return clone;
  }

  @Override
  protected void calculateTotalGoalsCovered() {
    coveredGoals = chrom.getCoveredLines();
  }
}