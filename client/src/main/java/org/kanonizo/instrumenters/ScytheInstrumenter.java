package org.kanonizo.instrumenters;

import com.scythe.instrumenter.InstrumentationProperties;
import com.scythe.instrumenter.analysis.ClassAnalyzer;
import com.scythe.instrumenter.instrumentation.ClassReplacementTransformer;
import com.scythe.instrumenter.instrumentation.ClassReplacementTransformer.ShouldInstrumentChecker;
import com.scythe.instrumenter.instrumentation.InstrumentingClassLoader;
import com.scythe.instrumenter.instrumentation.objectrepresentation.Branch;
import com.scythe.instrumenter.instrumentation.objectrepresentation.Line;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.notification.Failure;
import org.kanonizo.commandline.ProgressBar;
import org.kanonizo.framework.CUTChromosome;
import org.kanonizo.framework.CUTChromosomeStore;
import org.kanonizo.framework.SUTChromosome;
import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestSuiteChromosome;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.util.NullPrintStream;

//
@org.kanonizo.annotations.Instrumenter(readableName = "Scythe")
public class ScytheInstrumenter implements Instrumenter {
  private TestSuiteChromosome testSuite;
  private static PrintStream defaultSysOut = System.out;
  private static PrintStream defaultSysErr = System.err;
  private static final PrintStream NULL_OUT;
  private static Logger logger;
  private Map<TestCaseChromosome, Map<CUTChromosome, Set<Integer>>> linesCovered = new HashMap<>();
  private Map<TestCaseChromosome, Map<CUTChromosome, Set<Integer>>> branchesCovered = new HashMap<>();

  static {
    logger = LogManager.getLogger(ScytheInstrumenter.class);

    NULL_OUT = NullPrintStream.instance;
    InstrumentationProperties.INSTRUMENT_BRANCHES = false;
    ClassReplacementTransformer.addShouldInstrumentChecker(new ShouldInstrumentChecker() {

      private boolean isTestClass(String className) {
        try {
          if(className.contains("Test")){
            return true;
          }
          Class<?> cl = ClassLoader.getSystemClassLoader().loadClass(className.replaceAll("/", "."));
          List<Method> methods = Arrays.asList(cl.getDeclaredMethods());
          if(cl.isMemberClass() && isTestClass(cl.getEnclosingClass().getName())){
            return true;
          }
          if(methods.stream().anyMatch(method -> method.getName().startsWith("test"))){
            return true;
          }
          if(methods.stream().anyMatch(method -> Arrays.asList(method.getAnnotations()).contains(Test.class))){
            return true;
          }
        } catch (ClassNotFoundException e) {
          e.printStackTrace();
        }
        return false;
      }

      @Override
      public boolean shouldInstrument(String className) {
        return !isTestClass(className);
      }
    });
  }

  private static ProgressBar bar = new ProgressBar(defaultSysOut);

  public static PrintStream getNullOut() {
    return NULL_OUT;
  }

  private static void reportException(Exception e) {
    logger.error(e);
  }

  @Override
  public Class<?> loadClass(String className) throws ClassNotFoundException {
    return InstrumentingClassLoader.getInstance().loadClass(className);
  }

  @Override
  public void setTestSuite(TestSuiteChromosome ts) {
    this.testSuite = ts;
  }

  @Override
  public void collectCoverage() {
    try {
      List<Failure> failures = new ArrayList<>();
      bar.setTitle("Running Test Cases");
      // ensure coverage data is collected
      System.setOut(NULL_OUT);
      System.setErr(NULL_OUT);
      for (TestCaseChromosome testCase : testSuite.getRunnableTestCases()) {
        try {
          ClassAnalyzer.setActiveTestCase(testCase.getTestCase());
          testCase.run();
          // debug code to find out where/why failures are occurring. Use
          // breakpoints after execution to locate failures
          if (testCase.hasFailures()) {
            failures.addAll(testCase.getFailures());
          }
          bar.reportProgress((double) testSuite.getRunnableTestCases().indexOf(testCase) + 1,
              testSuite.getRunnableTestCases().size());
          ClassAnalyzer.collectHitCounters(true);
          linesCovered.put(testCase, collectLines(testCase));
          branchesCovered.put(testCase, collectBranches(testCase));
          ClassAnalyzer.resetCoverage();
        } catch (Throwable e) {
          e.printStackTrace(defaultSysErr);
          defaultSysErr.println(e.getMessage());
          // as much as I hate to catch throwables, it has to be done in this
          // instance because not all tests can be guaranteed to run at all
          // properly, and sometimes the Java API will
          // shutdown without this line
        }

      }
      bar.complete();
      logger.info("Finished instrumentation");
      System.setOut(defaultSysOut);
      System.setErr(defaultSysErr);
    } catch (final Exception e) {
      // runtime startup exception
      reportException(e);
    }
  }

  private Map<CUTChromosome, Set<Integer>> collectLines(TestCaseChromosome testCase){
    Map<CUTChromosome, Set<Integer>> covered = new HashMap<>();
    List<Class<?>> changedClasses = ClassAnalyzer.getChangedClasses();
    for (Class<?> cl : changedClasses) {
      if (CUTChromosomeStore.get(cl.getName()) != null) {
        List<Line> lines = ClassAnalyzer.getCoverableLines(cl.getName());
        Set<Integer> linesCovered = lines.stream().filter(line -> line.getHits() > 0).map(line -> line.getGoalId()).collect(Collectors.toSet());
        covered.put(CUTChromosomeStore.get(cl.getName()), linesCovered);
      }
    }
    return covered;
  }

  private Map<CUTChromosome, Set<Integer>> collectBranches(TestCaseChromosome testCase){
    Map<CUTChromosome, Set<Integer>> covered = new HashMap<>();
    List<Class<?>> changedClasses = ClassAnalyzer.getChangedClasses();
    for (Class<?> cl : changedClasses) {
      if (CUTChromosomeStore.get(cl.getName()) != null) {
        List<Branch> branches = ClassAnalyzer.getCoverableBranches(cl.getName());
        Set<Integer> branchesCovered = branches.stream().filter(branch -> branch.getHits() > 0).map(branch -> branch.getGoalId()).collect(Collectors.toSet());
        covered.put(CUTChromosomeStore.get(cl.getName()), branchesCovered);
      }
    }
    return covered;
  }

  @Override
  public Map<CUTChromosome, Set<Integer>> getLinesCovered(TestCaseChromosome testCase) {
    return linesCovered.get(testCase);
  }

  @Override
  public Map<CUTChromosome, Set<Integer>> getBranchesCovered(TestCaseChromosome testCase) {
    return branchesCovered.get(testCase);
  }

  @Override
  public int getTotalLines(CUTChromosome cut) {
    return ClassAnalyzer.getCoverableLines(cut.getCUT().getName()).size();
  }

  @Override
  public int getTotalBranches(CUTChromosome cut) {
    return ClassAnalyzer.getCoverableBranches(cut.getCUT().getName()).size();
  }

  @Override
  public Set<Integer> getLines(CUTChromosome cut) {
    return ClassAnalyzer.getCoverableLines(cut.getCUT().getName()).stream().map(line -> line.getLineNumber()).collect(Collectors.toSet());
  }

  @Override
  public Set<Integer> getBranches(CUTChromosome cut) {
    return ClassAnalyzer.getCoverableBranches(cut.getCUT().getName()).stream().map(line -> line.getLineNumber()).collect(Collectors.toSet());
  }

  @Override
  public int getTotalLines(SUTChromosome sut) {
    return sut.getClassesUnderTest().stream().mapToInt(cut -> ClassAnalyzer.getCoverableLines(cut.getCUT().getName()).size()).sum();
  }

  @Override
  public int getLinesCovered(TestSuiteChromosome testSuite) {
    return testSuite.getTestCases().stream().mapToInt(testCase -> getLinesCovered(testCase).entrySet().stream().mapToInt(entry -> entry.getValue().size()).sum()).sum();
  }

  @Override
  public int getTotalBranches(SUTChromosome sut) {
    return sut.getClassesUnderTest().stream().mapToInt(cut -> ClassAnalyzer.getCoverableBranches(cut.getCUT().getName()).size()).sum();
  }

  @Override
  public int getBranchesCovered(TestSuiteChromosome testSuite) {
    return testSuite.getTestCases().stream().mapToInt(testCase -> getBranchesCovered(testCase).entrySet().stream().mapToInt(entry -> entry.getValue().size()).sum()).sum();
  }

}
