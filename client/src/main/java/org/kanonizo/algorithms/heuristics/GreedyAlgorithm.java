package org.kanonizo.algorithms.heuristics;

import java.util.Collections;
import java.util.List;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.AbstractSearchAlgorithm;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;

@Algorithm(readableName = "greedy")
public class GreedyAlgorithm extends AbstractSearchAlgorithm {

  @Override
  public void generateSolution() {
    TestSuite suite = problem.clone().getTestSuite();
    List<TestCase> testCases = suite.getTestCases();
    Collections.sort(testCases, new FitnessComparator());
    suite.setTestCases(testCases);
    setCurrentOptimal(suite);
    fitnessEvaluations++;
  }

  @Override
  public double getFitness(TestCase tc) {
    return (Framework.getInstrumenter().getLinesCovered(tc).size());
  }

}
