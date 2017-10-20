package org.kanonizo.framework.instrumentation;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.runner.notification.Failure;

import org.kanonizo.commandline.ProgressBar;
import org.kanonizo.framework.SUTChromosome;
import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestSuiteChromosome;
import com.scythe.instrumenter.analysis.ClassAnalyzer;

//
public class Instrumenter {
  private static PrintStream defaultSysOut = System.out;
  private static PrintStream defaultSysErr = System.err;
  private static final PrintStream NULL_OUT;

  private static Logger logger;

  static {
    logger = LogManager.getLogger(Instrumenter.class);

    NULL_OUT = new NullPrintStream();
  }

  private static ProgressBar bar = new ProgressBar(defaultSysOut);

  public static void runTestCases(SUTChromosome sut, TestSuiteChromosome testSuite) {
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

  public static PrintStream getNullOut() {
    return NULL_OUT;
  }

  private static void reportException(Exception e) {
    logger.error(e);
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
