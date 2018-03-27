package org.kanonizo;

import java.util.Collections;
import java.util.List;
import org.kanonizo.algorithms.AbstractSearchAlgorithm;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;

/**
 * Created by davidpaterson on 16/12/2016.
 */
@Algorithm
public class RandomAlgorithm extends AbstractSearchAlgorithm {

  @Override
  protected void generateSolution() {
    TestSuite suite = problem.clone().getTestSuite();
    List<TestCase> testCases = suite.getTestCases();
    Collections.shuffle(testCases);
    suite.setTestCases(testCases);
    setCurrentOptimal(suite);
    fitnessEvaluations++;
  }

  @Override
  public boolean needsFitnessFunction() {
    return false;
  }

  @Override
  public String readableName() {
    return "random";
  }
}
