package org.kanonizo.test;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.heuristics.GreedyAlgorithm;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.ClassStore;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.instrumenters.ScytheInstrumenter;

public class GreedyAlgorithmTest extends SearchAlgorithmTest {
  private Instrumenter scytheInst = new ScytheInstrumenter();
  public GreedyAlgorithmTest() {
    super(new GreedyAlgorithm());
    Framework.setInstrumenter(scytheInst);
  }

  @Test
  public void testOrdering() {
    List<TestCase> testCases = algorithm.getCurrentOptimal().getTestCases();
    ClassUnderTest cut = ClassStore.get("sample_classes.Stack");
    for (int i = 0; i < testCases.size() - 2; i++) {
      TestCase test1 = testCases.get(i);
      TestCase test2 = testCases.get(i + 1);
      int linesCovered1 = Framework.getInstrumenter().getLinesCovered(test1).size();
      int linesCovered2 = Framework.getInstrumenter().getLinesCovered(test2).size();
      assertTrue("Test Case: " + test1 + " has lower coverage than " + test2,
          linesCovered1 >= linesCovered2);
    }
  }

}
