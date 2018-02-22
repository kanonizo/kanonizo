package org.kanonizo.algorithms.heuristics;

import com.scythe.instrumenter.analysis.ClassAnalyzer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.AbstractSearchAlgorithm;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.commandline.ProgressBar;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;

@Algorithm(readableName = "additionalgreedy")
public class AdditionalGreedyAlgorithm extends AbstractSearchAlgorithm {
  Set<Line> cache = new HashSet<>();
  int totalLines = 0;

  @Override
  public void generateSolution() {
    totalLines = Framework.getInstrumenter().getTotalLines(problem);
    TestSuite suite = problem.clone().getTestSuite();
    List<TestCase> testCases = suite.getTestCases();
    List<TestCase> newOrder = new ArrayList<TestCase>();
    FitnessComparator comp = new FitnessComparator();
    ProgressBar bar = new ProgressBar(ClassAnalyzer.out);
    bar.setTitle("Performing Additional Greedy sorting algorithm");
    while (!testCases.isEmpty() && !shouldFinish()) {
      age++;
      Collections.sort(testCases, comp);
      TestCase tc = testCases.get(0);
      totalLines = (int) getFitness(tc);
      newOrder.add(tc);
      testCases.remove(tc);
      cache.addAll(Framework.getInstrumenter().getLinesCovered(tc));
      bar.reportProgress(newOrder.size(), (newOrder.size() + testCases.size()));
    }
    // if we ran out of time for stopping conditions, add all remaining in the original order
    if (!testCases.isEmpty()) {
      newOrder.addAll(testCases);
    }
    bar.complete();
    suite.setTestCases(newOrder);
    setCurrentOptimal(suite);
    fitnessEvaluations++;
  }

  @Override
  public double getFitness(TestCase tc) {
    int newLines = Framework.getInstrumenter().getLinesCovered(tc).stream().mapToInt(line -> cache.contains(line) ? 0 : 1).sum();
    return newLines;
  }
}
