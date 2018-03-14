package org.kanonizo.algorithms.heuristics;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.TestCasePrioritiser;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.TestCase;

@Algorithm(readableName = "additionalgreedy")
public class AdditionalGreedyAlgorithm extends TestCasePrioritiser {
  private Set<Line> cache = new HashSet<>();
  private FitnessComparator comp = new FitnessComparator();
  @Override
  public TestCase selectTestCase(List<TestCase> testCases) {
    Collections.sort(testCases, comp);
    TestCase next = testCases.get(0);
    cache.addAll(inst.getLinesCovered(next));
    return next;
  }

  @Override
  public double getFitness(TestCase tc) {
    return inst.getLinesCovered(tc).stream().mapToInt(line -> cache.contains(line) ? 0 : 1).sum();
  }
}
