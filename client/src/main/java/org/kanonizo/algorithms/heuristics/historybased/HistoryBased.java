package org.kanonizo.algorithms.heuristics.historybased;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.kanonizo.algorithms.TestCasePrioritiser;
import org.kanonizo.commandline.ProgressBar;
import org.kanonizo.exception.SystemConfigurationException;
import org.kanonizo.framework.TestCaseStore;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.util.Util;

public abstract class HistoryBased extends TestCasePrioritiser {

  private static final String PROJECT_ID = "project_id";
  private static final String VERSION_ID = "version_id";
  private static final String NUM_REVISIONS = "num_revisions";
  private static final String REVISION_ID = "revision_id";
  private static final String TEST_NAME = "test_name";
  private static final String TEST_RUNTIME = "test_runtime";
  private static final String TEST_OUTCOME = "test_outcome";
  private static final String TEST_STACK_TRACE = "test_stack_strace";


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
      CSVParser parser = new CSVParser(new FileReader(HISTORY_FILE), CSVFormat.DEFAULT
          .withHeader(PROJECT_ID, VERSION_ID, NUM_REVISIONS, REVISION_ID, TEST_NAME, TEST_RUNTIME,
              TEST_OUTCOME, TEST_STACK_TRACE).withSkipHeaderRecord(true));
      int numRecords = (int) Files.lines(Paths.get(HISTORY_FILE.getAbsolutePath())).count();
      int count = 0;
      Iterator<CSVRecord> it = parser.iterator();
      ProgressBar bar = new ProgressBar(System.out);
      bar.setTitle("Reading history file");
      while(it.hasNext()){
        bar.reportProgress(count++, numRecords);
        CSVRecord next = it.next();
        String testCaseName = next.get(TEST_NAME);
        String testClass = testCaseName.split("::")[0];
        String testMethod = testCaseName.split("::")[1];
        String testString = testMethod + "(" + testClass + ")";
        TestCase tc = TestCaseStore.with(testString);
        // skip 0th execution since this is the "current" state, and we shouldn't know this information at runtime
        int ind = Math.abs(Integer.parseInt(next.get(REVISION_ID)));
        if (ind == 0) {
          continue;
        }
        if (!historyData.containsKey(tc)) {
          historyData.put(tc, new ArrayList<>());
        }
        Throwable cause = null;
        if (!next.get(TEST_STACK_TRACE).equals("")) {
          // parse throwable into object here
          String trace = next.get(TEST_STACK_TRACE);
          String exceptionClass = trace.split("[^a-zA-Z.@$]")[0];
          try {
            Class<?> cl = Class.forName(exceptionClass);
            if (Util.getConstructor(cl) != null) {
              cause = (Throwable) cl.newInstance();
            } else {
              List<Constructor> constructors = Arrays.asList(cl.getConstructors());
              // find constructor with only primitive or string arguments and use it
              Optional<Constructor> optCon = constructors.stream()
                  .filter(c -> areAllPrimitive(c.getParameterTypes())).findFirst();
              if(optCon.isPresent()){
                Constructor con = optCon.get();
                // get array of default arguments
                Object[] args = Arrays.stream(con.getParameterTypes()).map(this::mapArgument).toArray();
                cause = (Throwable) con.newInstance(args);
              }
            }

          } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
          }
        }
        long executionTime = Long.parseLong(next.get(TEST_RUNTIME));
        if (executionTime > maxExecutionTime) {
          maxExecutionTime = executionTime;
        }
        int numExecutions = historyData.get(tc).size();
        // necessary check to ensure that test cases that only existed in previous versions are not considered "current"
        // for example if a test case exists in the current version, the first element of its history data should be considered
        // the most recent execution of all test cases, but if we discover a test case later that
        if(ind > numExecutions){
          for(int i = numExecutions; i < ind; i++){
            historyData.get(tc).add(i, Execution.NULL_EXECUTION);
          }
        }
        // if we have padded test matrix with null executions and now we find an execution that should "slot in" to a location
        // this should never happen with the current history file structure, since we systematically move backwards in executions
        // but this guards against the case where we find execution -10, pad 9 NULL_EXECUTION objects and then find version -5, for example
        if(historyData.get(tc).size() > ind && historyData.get(tc).get(ind) == Execution.NULL_EXECUTION){
          historyData.get(tc).remove(ind);
        }
        historyData.get(tc).add(ind, new Execution(executionTime, next.get(TEST_OUTCOME).equals("pass"), cause));
        if (historyData.get(tc).size() > maxExecutions) {
          maxExecutions = historyData.get(tc).size();
        }
      }
      bar.complete();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static List<Class> primitives = Arrays.asList(new Class[]{Integer.class, int.class, Float.class,
      float.class, Double.class, double.class,
      Short.class, short.class,
      Long.class, long.class,
      Byte.class, byte.class,
      Character.class, char.class,
      Boolean.class, boolean.class, String.class});

  private boolean areAllPrimitive(Class[] types) {
    return primitives.containsAll(Arrays.asList(types));
  }

  private Object mapArgument(Class<?> type){
    if(type.equals(String.class)){
      return "";
    } else if(type.equals(Boolean.class) || type.equals(boolean.class)){
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
    if(!historyData.containsKey(tc)){
      return Collections.emptyList();
    }
    return historyData.get(tc).stream().map(Execution::isPassed).collect(Collectors.toList());
  }

  public List<Long> getRuntimes(TestCase tc) {
    if(!historyData.containsKey(tc)){
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

  public int getTimeSinceLastFailure(){
    int executionNo = 0;
    while(!anyFail(executionNo)){
      executionNo++;
    }
    return executionNo;
  }

  private boolean anyFail(int executionNo){
    return !historyData.values().stream().allMatch(l -> l.size() > executionNo && l.get(executionNo).isPassed());
  }

  public List<TestCase> getFailingTestCases(int executionNo){
    return historyData.entrySet().stream().filter(entry -> entry.getValue().size() > executionNo && !entry.getValue().get(executionNo).isPassed()).map(
        Entry::getKey).collect(Collectors.toList());
  }

  public Throwable getCause(TestCase testCase, int executionNo){
    if(historyData.get(testCase).size() < executionNo || historyData.get(testCase).get(executionNo).isPassed()){
      return null;
    }
    return historyData.get(testCase).get(executionNo).getFailureCause();
  }


}

