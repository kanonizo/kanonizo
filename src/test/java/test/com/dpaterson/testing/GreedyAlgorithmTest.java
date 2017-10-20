package test.com.dpaterson.testing;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import org.kanonizo.algorithms.heuristics.GreedyAlgorithm;
import org.kanonizo.framework.CUTChromosome;
import org.kanonizo.framework.CUTChromosomeStore;
import org.kanonizo.framework.TestCaseChromosome;

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
