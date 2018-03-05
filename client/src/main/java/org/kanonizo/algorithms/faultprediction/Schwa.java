package org.kanonizo.algorithms.faultprediction;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.algorithms.AbstractSearchAlgorithm;
import org.kanonizo.annotations.Algorithm;

@Algorithm(readableName = "schwa")
public class Schwa extends AbstractSearchAlgorithm {

  private static Logger logger = LogManager.getLogger(Schwa.class);

  @Parameter(key = "schwa_revisions_weight", description = "How much influence the number of revisions to a file should have over its likelihood of containing a fault", category="schwa")
  private static double REVISIONS_WEIGHT= 0.3;

  @Parameter(key = "schwa_authors_weight", description = "How much influence the number of authors= who have committed to a file should have over its likelihood of containing a fault", category="schwa")
  private static double AUTHORS_WEIGHT= 0.2;

  @Parameter(key = "schwa_fixes_weight", description = "How much influence the number of times a file has been associated with a \"fix\" should have over its likelihood of containing a fault", category="schwa")
  private static double FIXES_WEIGHT= 0.5;

  @Override
  protected void generateSolution() {
    if(!checkPythonInstallation()){
      return;
    }
    if(checkPythonVersion() == -1){
      return;
    }

  }

  private boolean checkPythonInstallation(){
    ProcessBuilder pb = new ProcessBuilder("python", "--version");
    int returnCode = -1;
    try{
      Process pythonCheck = pb.start();
      returnCode = pythonCheck.waitFor();
      if(returnCode != 0){
        logger.error("There is no python installation on the current system. Python is required to run Schwa!");
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return returnCode == 0;
  }

  private double checkPythonVersion(){
    //test other versions (for linux systems where multiple python versions may be installed
    double[] versions = new double[]{3, 3.2, 3.4, 3.5};
    for(double version : versions){
      ProcessBuilder pb = new ProcessBuilder("python"+version, "--version");
      try{
        pb.inheritIO();
        Process pythonCheck = pb.start();
        if(pythonCheck.waitFor() == 0){
          return version;
        }
      } catch (IOException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    return -1;
  }

}
