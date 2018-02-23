package org.kanonizo.framework;

import java.util.HashMap;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.framework.objects.TestCase;

public class TestCaseStore {

  private static HashMap<Integer, TestCase> testCases = new HashMap<>();
  private static final Logger logger = LogManager.getLogger(TestCaseStore.class);

  public static void register(int id, TestCase testCase) {
    testCases.put(id, testCase);
  }

  public static TestCase get(int id) {
    if (testCases.containsKey(id)) {
      return testCases.get(id);
    }
    logger.error("Trying to retrieve test case from store that doesn't exist!");
    return null;
  }

  public static TestCase with(String testString){
    Optional<TestCase> test = testCases.values().stream().filter(t -> t.toString().equals(testString)).findFirst();
    if(test.isPresent()){
      return test.get();
    } else {
      logger.debug("Test case with identifier "+testString + " does not exist");
      return null;
    }
  }
}
