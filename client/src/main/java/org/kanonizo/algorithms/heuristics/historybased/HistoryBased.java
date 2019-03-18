package org.kanonizo.algorithms.heuristics.historybased;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.algorithms.TestCasePrioritiser;
import org.kanonizo.exception.SystemConfigurationException;
import org.kanonizo.framework.TestCaseStore;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.util.Util;

public abstract class HistoryBased extends TestCasePrioritiser {

  private static final Logger logger = LogManager.getLogger(HistoryBased.class);


  @Parameter(key = "history_file", description = "For history based techniques we must provide a readable file containing the history of the test cases so that we can calculate the number of failures, time since last failure etc",
      category = "history")
  public static File HISTORY_FILE = null;

  private Map<TestCase, List<Execution>> historyData = new HashMap<>();

  private long maxExecutionTime = -1;

  private int maxExecutions = -1;

  public void init(List<TestCase> testCases) {
    if (HISTORY_FILE == null || !HISTORY_FILE.exists()) {
      throw new SystemConfigurationException(
          "In order to execute a history based technique you must provide a -Dhistory_file option in the command line");
    }
    readHistoryFile();
  }

  private void readHistoryFile() {
    try {
      InputStream inputFS = new FileInputStream(HISTORY_FILE);
      BufferedReader br = new BufferedReader(new InputStreamReader(inputFS));
      Pattern csvFormat = Pattern.compile(
          "[A-Za-z]+,\\d+[bf],\\d+,-?(\\d+),([a-zA-Z0-9\\.\\$\\_]+)::([[a-zA-Z0-9\\[\\]_]+]+),(\\d+),(pass|fail),(([a-zA-Z.@$]*).*$)");
      try {
        int numLines = Util.runIntSystemCommand("cat "+ HISTORY_FILE.getAbsolutePath() + " | wc -l");
        logger.debug("Lines to read: " + numLines);
      } catch (IOException e) {
        e.printStackTrace();
      }
      int count = 0;
      // skips the header
      br.lines().skip(1).forEach((line) -> {
        Matcher m = csvFormat.matcher(line);
        if (m.matches()) {
          String testClass = m.group(2);
          String testMethod = m.group(3);
          String testString = testMethod + "(" + testClass + ")";
          TestCase tc = TestCaseStore.with(testString);
          // we have a historical test case that has no current equivalent - move on
          if (tc == null) {
            return;
          }
          // skip 0th execution since this is the "current" state, and we shouldn't know this information at runtime
          int ind = Integer.parseInt(m.group(1)) - 1;
          if (ind == -1) {
            return;
          }
          if (!historyData.containsKey(tc)) {
            historyData.put(tc, new LinkedList<>());
          }
          long executionTime = Long.parseLong(m.group(4));
          if (executionTime > maxExecutionTime) {
            maxExecutionTime = executionTime;
          }
          boolean passed = "pass".equals(m.group(5));
          Throwable cause = null;
          // if test failed, try to work out why
          if (!passed) {
            String trace = m.group(6);
            if (!"".equals(trace)) {
              // parse throwable into object here
              String exceptionClass = m.group(7);
              try {
                Class<?> cl = Class.forName(exceptionClass);
                if (Util.getConstructor(cl) != null) {
                  cause = (Throwable) cl.newInstance();
                } else {
                  List<Constructor> constructors = Arrays.asList(cl.getConstructors());
                  // find constructor with only primitive or string arguments and use it
                  Optional<Constructor> optCon = constructors.stream()
                      .filter(c -> areAllPrimitive(c.getParameterTypes())).findFirst();
                  if (optCon.isPresent()) {
                    Constructor con = optCon.get();
                    // get array of default arguments
                    Object[] args = Arrays.stream(con.getParameterTypes()).map(this::mapArgument)
                        .toArray();
                    cause = (Throwable) con.newInstance(args);
                  } else {
                    // there is no exception we can create an instance of
                    cause = new Exception();
                  }
                }

              } catch (ClassNotFoundException e) {
                // if the exception class does not exist, we will just use a generic exception
                cause = new Exception();
              } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
              }
            }
            if (cause == null) {
              // really worst case, we didn't find anything we can use, but the cause must not be null
              cause = new Exception();
            }
          }
          List<Execution> testHistory = historyData.get(tc);
          int numExecutions = testHistory.size();
          // necessary check to ensure that test cases that only existed in previous versions are not considered "current"
          // for example if a test case exists in the current version, the first element of its history data should be considered
          // the most recent execution of all test cases, but if we discover a test case later that
          if (ind > numExecutions) {
            for (int i = numExecutions; i < ind; i++) {
              testHistory.add(i, Execution.NULL_EXECUTION);
            }
          }
          // if we have padded test matrix with null executions and now we find an execution that should "slot in" to a location
          // this should never happen with the current history file structure, since we systematically move backwards in executions
          // but this guards against the case where we find execution -10, pad 9 NULL_EXECUTION objects and then find version -5, for example
          if (testHistory.size() > 0 && testHistory.size() > ind
              && testHistory.get(ind) == Execution.NULL_EXECUTION) {
            testHistory.remove(ind);
          }
          testHistory.add(ind, new Execution(executionTime, passed, cause));
          if (testHistory.size() > maxExecutions) {
            maxExecutions = testHistory.size();
          }
        } else {
          System.out.println("Line " + line + " does not match regex");
        }
      });
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  private static List<Class> primitives = Arrays
      .asList(new Class[]{Integer.class, int.class, Float.class,
          float.class, Double.class, double.class,
          Short.class, short.class,
          Long.class, long.class,
          Byte.class, byte.class,
          Character.class, char.class,
          Boolean.class, boolean.class, String.class});

  private boolean areAllPrimitive(Class[] types) {
    return primitives.containsAll(Arrays.asList(types));
  }

  private Object mapArgument(Class<?> type) {
    if (type.equals(String.class)) {
      return "";
    } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
      return false;
    } else {
      return 0;
    }
  }

  public int getNumExecutions(TestCase tc) {
    return !historyData.containsKey(tc) ? 0 : historyData.get(tc).size();
  }

  public int getNumFailures(TestCase tc) {
    return !historyData.containsKey(tc) ? 0
        : (int) historyData.get(tc).stream().filter(ex -> !ex.isPassed()).count();
  }

  public boolean hasFailed(TestCase tc) {
    return historyData.containsKey(tc) && historyData.get(tc).stream()
        .anyMatch(ex -> !ex.isPassed());
  }

  public int getNumberOfTestCases() {
    return historyData.size();
  }

  public int getMaxExecutions() {
    return maxExecutions;
  }

  public long getMaxExecutionTime() {
    return maxExecutionTime;
  }

  public List<Boolean> getResults(TestCase tc) {
    if (!historyData.containsKey(tc)) {
      return Collections.emptyList();
    }
    return historyData.get(tc).stream().map(Execution::isPassed).collect(Collectors.toList());
  }

  public List<Long> getRuntimes(TestCase tc) {
    if (!historyData.containsKey(tc)) {
      return Collections.emptyList();
    }
    return historyData.get(tc).stream().map(Execution::getExecutionTime)
        .collect(Collectors.toList());
  }

  public int getTimeSinceLastFailure(TestCase tc) {
    if (!hasFailed(tc)) {
      return Integer.MAX_VALUE;
    }
    return historyData.get(tc)
        .indexOf(historyData.get(tc).stream().filter(ex -> !ex.isPassed()).findFirst().get());
  }

  public int getTimeSinceLastFailure() {
    int executionNo = 0;
    while (!anyFail(executionNo)) {
      executionNo++;
    }
    return executionNo;
  }

  private boolean anyFail(int executionNo) {
    return !historyData.values().stream()
        .allMatch(l -> l.size() > executionNo && l.get(executionNo).isPassed());
  }

  public List<TestCase> getFailingTestCases(int executionNo) {
    return historyData.entrySet().stream().filter(
        entry -> entry.getValue().size() > executionNo && !entry.getValue().get(executionNo)
            .isPassed()).map(
        Entry::getKey).collect(Collectors.toList());
  }

  public Throwable getCause(TestCase testCase, int executionNo) {
    if (historyData.get(testCase).size() < executionNo || historyData.get(testCase).get(executionNo)
        .isPassed()) {
      return null;
    }
    return historyData.get(testCase).get(executionNo).getFailureCause();
  }


}

