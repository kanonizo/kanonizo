package org.kanonizo.algorithms.heuristics;

import com.scythe.instrumenter.analysis.ClassAnalyzer;
import com.scythe.util.ClassUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.AbstractSearchAlgorithm;
import org.kanonizo.algorithms.metaheuristics.fitness.APFDFunction;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.commandline.ProgressBar;
import org.kanonizo.framework.CUTChromosome;
import org.kanonizo.framework.TestCaseChromosome;
@Algorithm(readableName = "additionalgreedy")
public class AdditionalGreedyAlgorithm extends AbstractSearchAlgorithm {
  Map<Integer, Set<Integer>> cache = new HashMap<>();
  int totalLines = 0;

  @Override
  public void generateSolution() {
    problem.getSUT().getClassesUnderTest().stream().forEach(cut -> {
      totalLines += Framework.getInstrumenter().getTotalLines(cut);
      if(ClassUtils.isInstrumented(cut.getCUT())){
        cache.put(cut.getId(), new HashSet<>());
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
      Map<CUTChromosome, List<Integer>> goals = ((APFDFunction) problem.getFitnessFunction())
          .getCoveredGoals(chr);
      goals.entrySet().stream().forEach(entry -> {
        int classId = entry.getKey().getId();
        cache.get(classId).addAll(entry.getValue());
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
          int classId = entry.getKey().getId();
          if (cache.containsKey(classId)) {
            return entry.getValue().stream().mapToInt(goal -> cache.get(classId).contains(goal) ? 0 : 1)
                .sum();
          } else {
            return entry.getValue().size();
          }
        }).sum();
    return newLines;
  }
}
