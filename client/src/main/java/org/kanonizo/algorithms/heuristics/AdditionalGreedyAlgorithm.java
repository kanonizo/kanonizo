package org.kanonizo.algorithms.heuristics;

import java.util.Collections;
import java.util.List;
import org.kanonizo.algorithms.TestCasePrioritiser;
import org.kanonizo.algorithms.heuristics.comparators.AdditionalComparator;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.framework.objects.TestCase;

@Algorithm
public class AdditionalGreedyAlgorithm extends TestCasePrioritiser {
  private AdditionalComparator comp = new AdditionalComparator();
  @Override
  public TestCase selectTestCase(List<TestCase> testCases) {
    Collections.sort(testCases, comp);
    TestCase next = testCases.get(0);
    return next;
  }

  @Override
  public String readableName() {
    return "additionalgreedy";
  }
}
