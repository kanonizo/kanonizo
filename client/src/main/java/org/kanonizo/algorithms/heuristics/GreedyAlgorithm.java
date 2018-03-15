package org.kanonizo.algorithms.heuristics;

import java.util.Collections;
import java.util.List;
import org.kanonizo.algorithms.TestCasePrioritiser;
import org.kanonizo.algorithms.heuristics.comparators.GreedyComparator;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.framework.objects.TestCase;

@Algorithm(readableName = "greedy")
public class GreedyAlgorithm extends TestCasePrioritiser {
  private GreedyComparator comp = new GreedyComparator();
  @Override
  public void init(List<TestCase> testCases){
    super.init(testCases);
    Collections.sort(testCases, comp);
  }
  @Override
  public TestCase selectTestCase(List<TestCase> testCases) {
    return testCases.get(0);
  }


}
