package org.kanonizo;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.util.Util;

public class TestSuitePrioritisation {
  private static final String ALGORITHM_SHORT = "a";
  private static final String ALGORITHM_LONG = "algorithm";
  private static final String HELP_SHORT = "h";
  private static final String HELP_LONG = "help";
  private static final String PROPERTY = "D";
  private static final String SOURCE_FOLDER_SHORT = "s";
  private static final String SOURCE_FOLDER_LONG = "sourceFolder";
  private static final String TEST_FOLDER_SHORT = "t";
  private static final String TEST_FOLDER_LONG = "testFolder";
  private static final String LIB_FOLDER_SHORT = "l";
  private static final String LIB_FOLDER_LONG = "libFolder";
  public static final String GUI_SHORT = "g";
  public static final String GUI_LONG = "gui";
  public static final String ROOT_FOLDER_SHORT = "r";
  public static final String ROOT_FOLDER_LONG = "root";

  private static Logger logger = LogManager.getLogger(TestSuitePrioritisation.class);

  public static Options getCommandLineOptions() {
    Options options = new Options();
    Option al = Option.builder(ALGORITHM_SHORT).hasArg()
        .desc(
            "The choice of algorithm to use. Options are Greedy, AdditionalGreedy, KOptimal, Irreplaceability, EIrreplaceability, HillClimb, GeneticAlgorithm, HypervolumeGA, EpistaticGA. If not specified the default choice will be the Greedy Algorithm")
        .argName("algorithm").longOpt(ALGORITHM_LONG).build();
    options.addOption(al);
    Option help = Option.builder(HELP_SHORT).desc("Prints out help for using this tool").longOpt(HELP_LONG).build();
    options.addOption(help);
    Option property = Option.builder(PROPERTY).argName("property=value").hasArgs().valueSeparator()
        .desc("use value for given property").build();
    options.addOption(property);
    Option sourceFolder = Option.builder(SOURCE_FOLDER_SHORT)
        .desc(
            "Directory containing all source classes for the program. The content of these files will be used to measure the effectiveness of the test suite. Test classes should not be included")
        .hasArg().longOpt(SOURCE_FOLDER_LONG).build();
    options.addOption(sourceFolder);
    Option testFolder = Option.builder(TEST_FOLDER_SHORT)
        .desc(
            "Directory containing all test classes for the program. The source code will be instrumented while running the tests contained in these classes. Source classes should not be included")
        .hasArg().longOpt(TEST_FOLDER_LONG).build();
    options.addOption(testFolder);
    Option libFolder = Option.builder(LIB_FOLDER_SHORT)
        .desc(
            "Library of all jar files required in order to run the source or the tests. This is an optional parameter, in the case of this project being controlled by maven dependencies will be automatically resolved assuming a project structure of {project_root}/target/classes and {project_root}/target/tests")
        .hasArgs().longOpt(LIB_FOLDER_LONG).build();
    options.addOption(libFolder);
    Option noGui = Option.builder(GUI_SHORT).desc("Option to enable to gui").longOpt(GUI_LONG).build();
    options.addOption(noGui);
    Option root = Option.builder(ROOT_FOLDER_SHORT).desc("Root folder of the target project").longOpt(ROOT_FOLDER_LONG).hasArg().build();
    options.addOption(root);
    return options;
  }

  public static boolean hasHelpOption(CommandLine line) {
    return line.hasOption(HELP_SHORT);
  }

  public static void handleProperties(CommandLine line, Set<Field> parameters) {
    java.util.Properties properties = line.getOptionProperties(PROPERTY);
    if (properties != null) {
      for (String property : properties.stringPropertyNames()) {
        Optional<Field> f = parameters.stream().filter(field -> (field.getAnnotation(Parameter.class)).key().equals(property)).findFirst();
        if(!f.isPresent()){
          logger.info("Ignoring parameter "+property+ " because it could not be found in any class file");
          continue;
        }
        try {
          Util.setParameter(f.get(), properties.getProperty(property));
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
