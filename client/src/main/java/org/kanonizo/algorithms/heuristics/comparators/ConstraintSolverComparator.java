package org.kanonizo.algorithms.heuristics.comparators;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.Framework;
import org.kanonizo.framework.ObjectiveFunction;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.util.RandomInstance;

public class ConstraintSolverComparator implements ObjectiveFunction {

  private static final Logger logger = LogManager.getLogger(ConstraintSolverComparator.class);

  private Instrumenter inst;

  private static final Framework fw = Framework.getInstance();

  private final String minionExec;

  private static String MINION_INPUT_FILE;

  private static String MINION_OUPUT_FILE;

  static {
    try {
      File in = File.createTempFile("minion-input-file", ".txt");
      in.deleteOnExit();
      MINION_INPUT_FILE = in.getAbsolutePath();
      logger.debug("Minion input file: " + MINION_INPUT_FILE);

      File out = File.createTempFile("minion-output-file", ".txt");
      out.deleteOnExit();
      MINION_OUPUT_FILE = out.getAbsolutePath();
      logger.debug("Minion output file: " + MINION_OUPUT_FILE);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public ConstraintSolverComparator(final String minionExec) {
    this.minionExec = minionExec;
    if (minionExec == null || !(new File(this.minionExec).exists())) {
      throw new RuntimeException(String.format("File '%s' does not exist", this.minionExec));
    }

    this.inst = fw.getInstrumenter();
    fw.addPropertyChangeListener(Framework.INSTRUMENTER_PROPERTY_NAME, (e) -> {
      this.inst = (Instrumenter) e.getNewValue();
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String readableName() {
    return "constraint_solver";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<TestCase> sort(List<TestCase> testCases) {
    if (testCases.isEmpty()) {
      return testCases;
    }

    /**
     * Prepare input for MINION
     */

    try {
      if (!this.createMinionInput(testCases)) {
        return testCases;
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Creation of Minion's input has failed");
    }

    /**
     * Run MINION
     */

    try {
      this.runMinion();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Execution of Minion has failed");
    }

    /**
     * Analyse MINION's output
     */

    try {
      List<List<TestCase>> solutions = this.analyseMinionOutput(testCases);
      int index = RandomInstance.nextInt(solutions.size());
      logger.debug("Selected solution '" + index + "' out of " + solutions.size());
      return solutions.get(index);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("It failed to analyse the output of Minion");
    }
  }

  /**
   * Construct an input file for the constraint solver.
   * 
   * @param testCases
   * @return
   * @throws Exception
   */
  private boolean createMinionInput(List<TestCase> testCases) throws Exception {
    if (testCases.isEmpty()) {
      return false;
    }

    Set<Line> allCoveredLines = new LinkedHashSet<Line>();

    StringBuilder str = new StringBuilder();
    str.append("MINION 3\n\n");
    str.append("**VARIABLES**\n\n");

    List<String> allTs = new ArrayList<String>();
    for (int i = 0; i < testCases.size(); i++) {
      String t = String.format("t%d", i);
      allTs.add(t);
      str.append(String.format("BOOL %s\n", t));

      allCoveredLines.addAll(this.inst.getLinesCovered(testCases.get(i)));
    }

    if (allCoveredLines.isEmpty()) {
      return false;
    }

    str.append("\n**SEARCH**\n\n");
    str.append(String.format("VARORDER [%s]\n\n", String.join(",", allTs)));
    str.append("PRINT ALL\n\n");
    str.append("**CONSTRAINTS**\n\n");

    for (Line line : allCoveredLines) {
      List<String> testsThatCoverLine = new ArrayList<String>();

      for (int i = 0; i < testCases.size(); i++) {
        Set<Line> coveredLines = this.inst.getLinesCovered(testCases.get(i));
        if (coveredLines.contains(line)) {
          testsThatCoverLine.add(String.format("eq(t%d,1)", i));
        }
      }

      if (!testsThatCoverLine.isEmpty()) {
        str.append(String.format("watched-or({%s})\n", String.join(",", testsThatCoverLine)));
      }
    }

    str.append("\n**EOF**");

    Writer output = new BufferedWriter(new FileWriter(MINION_INPUT_FILE, false));
    output.write(str.toString());
    output.close();

    return true;
  }

  /**
   * Run the constraint solver.
   * 
   * @throws Exception
   */
  private void runMinion() throws Exception {
    Runtime r = Runtime.getRuntime();
    Process p = null;

    String osName = System.getProperty("os.name").toLowerCase();

    File outputFile = new File(MINION_OUPUT_FILE);
    if (outputFile.exists()) {
      outputFile.delete();
    }

    if ((osName.contains("linux") == true) || (osName.contains("mac") == true)) {
      String cmd[] = {this.minionExec, "-noprintsols", "-noresume", MINION_INPUT_FILE, "-solsout",
          MINION_OUPUT_FILE};
      p = r.exec(cmd);
    } else if (osName.contains("windows") == true) {
      String cmd[] = {"cmd.exe", "/c", "start /min " + this.minionExec + " -noprintsols -noresume "
          + MINION_INPUT_FILE + " -solsout " + MINION_OUPUT_FILE};
      p = r.exec(cmd);

      BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
      BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
      String s = null;

      while ((s = stdInput.readLine()) != null) {
        logger.debug(s);
      }
      while ((s = stdError.readLine()) != null) {
        logger.debug(s);
      }
    }

    if (p.waitFor() != 0) {
      throw new InterruptedException("Wrong arguments?");
    }
  }

  /**
   * Analyse the output of the constraint solver.
   * 
   * @param testCases
   * @return
   * @throws Exception
   */
  private List<List<TestCase>> analyseMinionOutput(List<TestCase> testCases) throws Exception {
    BufferedReader minionsOutputFile = new BufferedReader(new FileReader(MINION_OUPUT_FILE));
    String line;

    List<List<TestCase>> solutions = new ArrayList<List<TestCase>>();

    while ((line = minionsOutputFile.readLine()) != null) {
      List<TestCase> solution = new ArrayList<TestCase>();
      StringBuilder strie = new StringBuilder();

      String[] split = line.split(" ");
      for (int i = 0; i < split.length; i++) {
        if (split[i].compareTo("1") == 0) {
          // test i has to be considered
          strie.append(String.format("%d,", i + 1));

          solution.add(testCases.get(i));
        }
      }

      if (!solution.isEmpty()) {
        // TODO it does not seem necessary, but we may want to use a Trie
        // (https://en.wikipedia.org/wiki/Trie) to discard subsumed solutions
        solutions.add(solution);
      }
    }

    minionsOutputFile.close();
    return solutions;
  }
}
