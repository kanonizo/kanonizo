package org.kanonizo.algorithms.heuristics;

import java.util.Collections;
import java.util.List;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.TestCasePrioritiser;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.framework.objects.TestCase;

@Algorithm(readableName = "greedy")
public class GreedyAlgorithm extends TestCasePrioritiser {
  private FitnessComparator comp = new FitnessComparator();
  private boolean first = true;
  @Override
  public TestCase selectTestCase(List<TestCase> testCases) {
    if(first){
      first = false;
      Collections.sort(testCases, comp);
    }
    return testCases.get(0);
  }

  @Override
  public double getFitness(TestCase tc) {
    return (inst.getLinesCovered(tc).size());
  }

}
