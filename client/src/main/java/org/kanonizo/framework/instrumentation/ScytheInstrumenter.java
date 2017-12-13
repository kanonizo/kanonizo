package org.kanonizo.framework.instrumentation;

import com.scythe.instrumenter.analysis.ClassAnalyzer;
import com.scythe.instrumenter.instrumentation.InstrumentingClassLoader;
import com.scythe.instrumenter.instrumentation.objectrepresentation.Branch;
import com.scythe.instrumenter.instrumentation.objectrepresentation.Line;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.runner.notification.Failure;
import org.kanonizo.commandline.ProgressBar;
import org.kanonizo.framework.CUTChromosome;
import org.kanonizo.framework.CUTChromosomeStore;
import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestSuiteChromosome;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

//
public class ScytheInstrumenter implements Instrumenter {
  private TestSuiteChromosome testSuite;
  private static PrintStream defaultSysOut = System.out;
  private static PrintStream defaultSysErr = System.err;
  private static final PrintStream NULL_OUT;

  private static Logger logger;

  static {
    logger = LogManager.getLogger(ScytheInstrumenter.class);

    NULL_OUT = new NullPrintStream();
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
  public void runTestCases() {
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
          testCase.instrumentationFinished();
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

  @Override
  public Map<String, Set<Integer>> getLinesCovered(TestCaseChromosome testCase) {
    Map<String, Set<Integer>> covered = new HashMap<>();
    List<Class<?>> changedClasses = ClassAnalyzer.getChangedClasses();
    for (Class<?> cl : changedClasses) {
      if (CUTChromosomeStore.get(cl.getName()) != null) {
        List<Line> lines = ClassAnalyzer.getCoverableLines(cl.getName());
        Set<Integer> linesCovered = lines.stream().filter(line -> line.getHits() > 0).map(line -> line.getGoalId()).collect(Collectors.toSet());
        covered.put(cl.getName(), linesCovered);
      }
    }
    return covered;
  }

  @Override
  public Map<String, Set<Integer>> getBranchesCovered(TestCaseChromosome testCase) {
    Map<String, Set<Integer>> covered = new HashMap<>();
    List<Class<?>> changedClasses = ClassAnalyzer.getChangedClasses();
    for (Class<?> cl : changedClasses) {
      if (CUTChromosomeStore.get(cl.getName()) != null) {
        List<Branch> branches = ClassAnalyzer.getCoverableBranches(cl.getName());
        Set<Integer> branchesCovered = branches.stream().filter(branch -> branch.getHits() > 0).map(branch -> branch.getGoalId()).collect(Collectors.toSet());
        covered.put(cl.getName(), branchesCovered);
      }
    }
    return covered;
  }

  @Override
  public double getLineCoverage(CUTChromosome cut) {
    return ClassAnalyzer.getLineCoverage(cut.getCUT().getName());
  }

  @Override
  public double getBranchCoverage(CUTChromosome cut) {
    return ClassAnalyzer.getBranchCoverage(cut.getCUT().getName());
  }

  @Override
  public List<Class<?>> getAffectedClasses() {
    return ClassAnalyzer.getChangedClasses();
  }

  private static final class NullPrintStream extends PrintStream {

    public NullPrintStream() {
      super(new OutputStream() {

        @Override
        public void write(int b) throws IOException {
          // do nothing, we don't want execution code from the program
          // runtime being dumped into console
        }

      });
    }

  }
}
