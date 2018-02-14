package org.kanonizo.algorithms.metaheuristics.fitness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.kanonizo.framework.CUTChromosome;
import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestSuiteChromosome;

public class APBCFunction extends APFDFunction {

  public APBCFunction(TestSuiteChromosome chrom) {
    super(chrom);
  }

  @Override
  public Map<CUTChromosome, List<Integer>> getCoveredGoals(TestCaseChromosome tc) {
    Map<CUTChromosome, List<Integer>> returnMap = new HashMap<>();
    tc.getAllBranchesCovered().entrySet().forEach(entry -> {
      List<Integer> goals = new ArrayList<>();
      goals.addAll(entry.getValue());
      returnMap.put(entry.getKey(), goals);
    });
    return returnMap;
  }

  @Override
  protected List<Integer> getGoals() {
    return chrom.getSUT().getClassesUnderTest().stream().map(CUTChromosome::getCoverableBranches).flatMap(List::stream)
        .collect(Collectors.toList());
  }

  @Override
  public FitnessFunction<TestSuiteChromosome> clone(TestSuiteChromosome chr) {
    APBCFunction clone = new APBCFunction(chr);
    clone.coveredGoals = coveredGoals;
    return clone;
  }

  @Override
  protected void calculateTotalGoalsCovered() {
    coveredGoals = chrom.getCoveredBranches() / 2;
  }

}
