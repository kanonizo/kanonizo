package org.kanonizo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.kanonizo.util.Util;

public class MavenAnalyser {
  public static boolean isMavenProject(File root) {
    if (root == null) {
      return false;
    }
    if (root.list() == null) {
      return false;
    }
    if(!testMavenInstallation()){
      return false;
    }
    return Arrays.asList(root.list()).stream().anyMatch(name -> name != null && name.equals("pom.xml"));
  }

  public static void addMavenDependencies(File root) {
    File outputFile = new File(root.getAbsolutePath() + "/deps");
    if (!outputFile.exists()) {
      outputFile.mkdir();
    }
    List<String> commands = new ArrayList<>();
    commands.add("mvn");
    commands.add("dependency:copy-dependencies");
    commands.add("-DoutputDirectory=./deps");
    try {
      ProcessBuilder mvnBuilder = new ProcessBuilder(commands);
      mvnBuilder.directory(root);
      mvnBuilder.inheritIO();
      Process mvnProc = mvnBuilder.start();
      mvnProc.waitFor();
      if (outputFile.exists() && outputFile.listFiles().length > 0) {
        Arrays.asList(outputFile.listFiles()).forEach(jar -> {
          try {
            Util.addToClassPath(jar);
          } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        });
      }
    } catch (final IOException | InterruptedException e) {
      throw new RuntimeException("Could not gather Maven Dependencies.");
    }
  }

  private static boolean testMavenInstallation() {
    try {
      // test maven installation. If maven isn't installed we can't run the tool
      ProcessBuilder builder = new ProcessBuilder(new String[] { "mvn", "-v" });
      Process proc = builder.start();
      proc.waitFor();
      return true;
    } catch (final IOException | InterruptedException e) {
      //e.printStackTrace();
    }
    return false;
  }
}
