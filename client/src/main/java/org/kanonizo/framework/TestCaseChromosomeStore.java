package org.kanonizo.framework;

import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestCaseChromosomeStore {

  private static HashMap<Integer, TestCaseChromosome> testCases = new HashMap<>();
  private static final Logger logger = LogManager.getLogger(TestCaseChromosomeStore.class);

  public static void register(int id, TestCaseChromosome testCase) {
    testCases.put(id, testCase);
  }

  public static TestCaseChromosome get(int id) {
    if (testCases.containsKey(id)) {
      return testCases.get(id);
    }
    logger.error("Trying to retrieve test case from store that doesn't exist!");
    return null;
  }
}
