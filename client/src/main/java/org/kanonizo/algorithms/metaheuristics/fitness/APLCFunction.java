package org.kanonizo.algorithms.metaheuristics.fitness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.kanonizo.Framework;
import org.kanonizo.framework.CUTChromosome;
import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestSuiteChromosome;

public class APLCFunction extends APFDFunction {

  public APLCFunction(TestSuiteChromosome chr) {
    super(chr);
  }

  @Override
  public Map<CUTChromosome, List<Integer>> getCoveredGoals(TestCaseChromosome tc) {
    Map<CUTChromosome, List<Integer>> returnMap = new HashMap<>();
    Framework.getInstrumenter().getLinesCovered(tc).entrySet().forEach(entry -> {
      List<Integer> goals = new ArrayList<>();
      goals.addAll(entry.getValue());
      returnMap.put(entry.getKey(), goals);
    });
    return returnMap;
  }

  @Override
  protected List<Integer> getGoals() {
    int totalLines = chrom.getSUT().getClassesUnderTest().stream().mapToInt(cut -> Framework.getInstrumenter().getTotalLines(cut)).sum();
    return IntStream.range(1, totalLines).boxed().collect(Collectors.toList());
  }

  @Override
  public FitnessFunction<TestSuiteChromosome> clone(TestSuiteChromosome chr) {
    APLCFunction clone = new APLCFunction(chr);
    clone.coveredGoals = coveredGoals;
    return clone;
  }

  @Override
  protected void calculateTotalGoalsCovered() {
    coveredGoals = Framework.getInstrumenter().getLinesCovered(chrom);
  }
}