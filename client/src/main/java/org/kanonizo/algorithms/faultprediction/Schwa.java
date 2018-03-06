package org.kanonizo.algorithms.faultprediction;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.algorithms.AbstractSearchAlgorithm;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.annotations.Prerequisite;
import org.kanonizo.util.Util;

@Algorithm(readableName = "schwa")
public class Schwa extends AbstractSearchAlgorithm {

  private static Logger logger = LogManager.getLogger(Schwa.class);

  @Parameter(key = "schwa_revisions_weight", description = "How much influence the number of revisions to a file should have over its likelihood of containing a fault", category = "schwa")
  private static double REVISIONS_WEIGHT = 0.3;

  @Parameter(key = "schwa_authors_weight", description = "How much influence the number of authors= who have committed to a file should have over its likelihood of containing a fault", category = "schwa")
  private static double AUTHORS_WEIGHT = 0.2;

  @Parameter(key = "schwa_fixes_weight", description = "How much influence the number of times a file has been associated with a \"fix\" should have over its likelihood of containing a fault", category = "schwa")
  private static double FIXES_WEIGHT = 0.5;

  @Override
  protected void generateSolution() {

  }

  @Prerequisite(failureMessage = "Feature weights do not add up to 1. -Dschwa_revisions_weight, -Dschwa_authors_weight and -Dschwa_fixes_weight should sum to 1")
  public static boolean checkWeights(){
    return Util.doubleEquals(REVISIONS_WEIGHT + AUTHORS_WEIGHT + FIXES_WEIGHT, 1);
  }

  @Prerequisite(failureMessage = "Python3 is not installed on this system or is not executable on the system path. Please check your python3 installation.")
  public static boolean checkPythonInstallation() {
    int returnCode = runProcess("python3", "--version");
    return returnCode == 0;

  }

  @Prerequisite(failureMessage = "Schwa is not installed on this system, and Kanonizo failed to install it. Try again or visit Schwa on GitHub (https://github.com/andrefreitas/schwa) to manually install")
  public static boolean checkSchwaInstallation() {
    int returnCode = runProcess("schwa", "-h");
    if(returnCode != 0){
      returnCode = runProcess("python3", "-m", "schwa", "-h");
    }
    //TODO install Schwa if not present on users system??
    return returnCode == 0;
  }

  private static int runProcess(String... process){
    ProcessBuilder pb = new ProcessBuilder(process);
    int returnCode = -1;
    try {
      Process processRun = pb.start();
      returnCode = processRun.waitFor();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return returnCode;
  }
}
