package com.dpaterson.testing.algorithms.heuristics;

import com.dpaterson.testing.algorithms.AbstractSearchAlgorithm;
import com.dpaterson.testing.algorithms.metaheuristics.fitness.APFDFunction;
import com.dpaterson.testing.commandline.ProgressBar;
import com.dpaterson.testing.framework.CUTChromosome;
import com.dpaterson.testing.framework.TestCaseChromosome;
import com.sheffield.instrumenter.analysis.ClassAnalyzer;
import com.sheffield.instrumenter.instrumentation.objectrepresentation.CoverableGoal;
import com.sheffield.util.ClassUtils;

import java.util.*;
import java.util.stream.Collectors;

public class AdditionalGreedyAlgorithm extends AbstractSearchAlgorithm {
  Map<Integer, Set<Integer>> cache = new HashMap<>();
  int totalLines = 0;

  @Override
  public void generateSolution() {
    problem.getSUT().getClassesUnderTest().stream().forEach(cut -> {
      totalLines += cut.getTotalLines();
      if(ClassUtils.isInstrumented(cut.getCUT())){
        cache.put(ClassAnalyzer.getClassId(cut.getCUT().getName()), new HashSet<>());
      }

    });
    List<TestCaseChromosome> testCases = problem.getTestCases();
    List<TestCaseChromosome> newOrder = new ArrayList<TestCaseChromosome>();
    FitnessComparator comp = new FitnessComparator();
    ProgressBar bar = new ProgressBar(ClassAnalyzer.out);
    bar.setTitle("Performing Additional Greedy sorting algorithm");
    while (!testCases.isEmpty() && !shouldFinish()) {
      age++;
      Collections.sort(testCases, comp);
      TestCaseChromosome chr = testCases.get(0);
      totalLines = (int) getFitness(chr);
      newOrder.add(chr);
      testCases.remove(chr);
      Map<CUTChromosome, List<CoverableGoal>> goals = ((APFDFunction) problem.getFitnessFunction())
          .getCoveredGoals(chr);
      goals.entrySet().stream().forEach(entry -> {
        int classId = ClassAnalyzer.getClassId(entry.getKey().getCUT().getName());
        cache.get(classId).addAll(entry.getValue().stream().map(goal -> goal.getGoalId()).collect(Collectors.toList()));
      });
      bar.reportProgress(newOrder.size(), (newOrder.size() + testCases.size()));
    }
    // if we ran out of time for stopping conditions, add all remaining in the original order
    if (!testCases.isEmpty()) {
      newOrder.addAll(testCases);
    }
    bar.complete();
    problem.setTestCases(newOrder);
    fitnessEvaluations++;
  }

  @Override
  public double getFitness(TestCaseChromosome chr) {
    int newLines = ((APFDFunction) problem.getFitnessFunction()).getCoveredGoals(chr).entrySet().stream()
        .mapToInt(entry -> {
          int classId = ClassAnalyzer.getClassId(entry.getKey().getCUT().getName());
          if (cache.containsKey(classId)) {
            return entry.getValue().stream().mapToInt(goal -> cache.get(classId).contains(goal.getGoalId()) ? 0 : 1)
                .sum();
          } else {
            return entry.getValue().size();
          }
        }).sum();
    return newLines;
  }
}
