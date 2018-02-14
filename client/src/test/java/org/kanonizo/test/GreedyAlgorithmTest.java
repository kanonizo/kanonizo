package org.kanonizo.test;

import org.junit.Test;
import org.kanonizo.algorithms.heuristics.GreedyAlgorithm;
import org.kanonizo.framework.CUTChromosome;
import org.kanonizo.framework.CUTChromosomeStore;
import org.kanonizo.framework.TestCaseChromosome;

import java.util.List;

<<<<<<< HEAD:client/src/test/java/org/kanonizo/test/GreedyAlgorithmTest.java
import static org.junit.Assert.assertTrue;
=======
import org.junit.Test;

import org.kanonizo.algorithms.heuristics.GreedyAlgorithm;
import org.kanonizo.framework.CUTChromosome;
import org.kanonizo.framework.CUTChromosomeStore;
import org.kanonizo.framework.TestCaseChromosome;
>>>>>>> 6640b020c437f087863f27ea82489c01f4d92759:src/test/java/test/com/dpaterson/testing/GreedyAlgorithmTest.java

public class GreedyAlgorithmTest extends SearchAlgorithmTest {

  public GreedyAlgorithmTest() {
    super(new GreedyAlgorithm());
  }

  @Test
  public void testOrdering() {
    List<TestCaseChromosome> testCases = algorithm.getCurrentOptimal().getTestCases();
    CUTChromosome cut = CUTChromosomeStore.get("sample_classes.Stack");
    for (int i = 0; i < testCases.size() - 2; i++) {
      TestCaseChromosome test1 = testCases.get(i);
      TestCaseChromosome test2 = testCases.get(i + 1);
      assertTrue("Test Case: " + test1 + " has lower coverage than " + test2,
          test1.getLinesCovered(cut) >= test2.getLinesCovered(cut));
    }
  }

}
