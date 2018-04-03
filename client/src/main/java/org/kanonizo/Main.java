package org.kanonizo;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import com.scythe.instrumenter.instrumentation.InstrumentingClassLoader;
import com.scythe.instrumenter.mutation.MutationProperties;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.annotations.Prerequisite;
import org.kanonizo.display.ConsoleDisplay;
import org.kanonizo.display.Display;
import org.kanonizo.display.fx.KanonizoFrame;
import org.kanonizo.exception.SystemConfigurationException;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.util.Util;
import org.reflections.Reflections;

public class Main {

  public static final Logger logger = LogManager.getLogger(Main.class);


  public static void main(String[] args) {
    Framework fw = Framework.getInstance();
    // org.evosuite.Properties.TT = true;
    Options options = TestSuitePrioritisation.getCommandLineOptions();
    CommandLine line = null;
    try {
      line = new DefaultParser().parse(options, args, false);
      if (TestSuitePrioritisation.hasHelpOption(line)) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Search Algorithms", options);
        return;
      }
      Reflections r = Util.getReflections();
      Set<Field> parameters = r.getFieldsAnnotatedWith(Parameter.class);
      TestSuitePrioritisation.handleProperties(line, parameters);
      if (MutationProperties.VISIT_MUTANTS) {
        InstrumentingClassLoader.getInstance().setVisitMutants(true);
      }
      setupFramework(line, fw);
    } catch (Exception e) {
      logger.error(e);
    }
    Display d = null;
    if (!line.hasOption(TestSuitePrioritisation.GUI_SHORT)) {
      d = new ConsoleDisplay();
      d.initialise();
      fw.setDisplay(d);
      fw.addSelectionListener((ConsoleDisplay) d);
      try {
        fw.run();
      } catch (Exception e) {
        logger.error(e);
        e.printStackTrace();
      }
      // necessary due to random thread creation during test cases (don't do
      // this ever again)
      java.lang.System.exit(0);
    } else {
      d = new KanonizoFrame();
      d.initialise();
    }
  }

  /**
   * Takes options from the {@link CommandLine} and creates a {@link TestSuite} instance containing
   * all of the test cases and a {@link SystemUnderTest} object containing all of the source
   * classes. The command line must contain a -s/--sourceFolder option for the source classes and a
   * -t/--testFolder option for the test cases. Both of these folders will be added to the
   * classpath, and all nested .class files will be loaded in as either source or test cases ready
   * for instrumentation. Currently, instrumentation and JUnit execution takes place in the
   * constructor of a {@link TestSuite}, but this may well be changed as fitness functions are
   * introduced that are not reliant on code coverage.
   *
   * @param line - the {@link CommandLine} instance which must contain -s and -t options for source
   * and test folders respectively
   * @return a {@link TestSuite} object containing a {@link SystemUnderTest} with all of the source
   * classes and a list of {@link TestCase} objects for all of the test cases contained within the
   * specified location
   */
  public static void setupFramework(CommandLine line, Framework fw) throws Exception {
    File file;
    String folder;
    String[] libFolders;
    if (!line.hasOption("s") || !line.hasOption("t")) {
      throw new MissingOptionException(
          "In order to run the test suite prioritisation, a source folder must be given using -s or --sourceFolder and a test folder must be given using -t or --testFolder");
    }
    if (line.hasOption("l")) {
      // lib folder
      libFolders = line.getOptionValues("l");
      for (String libFolder : libFolders) {
        file = Util.getFile(libFolder);
        fw.addLibrary(file);
      }
    }
    if (!MutationProperties.MAJOR_ROOT.equals("")) {
      File majorJar = Util.getFile(MutationProperties.MAJOR_ROOT + "/config/config.jar");
      Util.addToClassPath(majorJar);
    }
    folder = line.getOptionValue("s");
    // relative path
    file = Util.getFile(folder);
    fw.setSourceFolder(file);
    if(line.hasOption(TestSuitePrioritisation.ROOT_FOLDER_SHORT)){
      fw.setRootFolder(Util.getFile(line.getOptionValue(TestSuitePrioritisation.ROOT_FOLDER_SHORT)));
    }
    File root = fw.getRootFolder() != null ? fw.getRootFolder() : file.getParentFile().getParentFile();
//    if (MavenAnalyser.isMavenProject(root) && fw.getLibraries().isEmpty()) {
//      MavenAnalyser.addMavenDependencies(root);
//    }
    // test folder
    folder = line.getOptionValue("t");
    // relative path
    file = Util.getFile(folder);
    fw.setTestFolder(file);
    String algorithmChoice = line.hasOption("a") ? line.getOptionValue("a") : "";
    fw.setAlgorithm(getAlgorithm(algorithmChoice));
    String instrumenter = Properties.INSTRUMENTER;
    setInstrumenter(fw, instrumenter);
  }

  private static void setInstrumenter(Framework fw, String instrumenter) {
    Reflections r = Util.getReflections();
    Set<?> instrumenters = r
        .getTypesAnnotatedWith(org.kanonizo.annotations.Instrumenter.class).stream().map(cl -> {
          try {
            return cl.newInstance();
          } catch (IllegalAccessException | InstantiationException e) {
            logger.error(e);
          }
          return null;
        }).collect(Collectors.toSet());
    for (Object inst : instrumenters) {
      if (instrumenter
          .equals(((Instrumenter) inst).readableName())) {
        fw.setInstrumenter(
            (org.kanonizo.framework.instrumentation.Instrumenter) inst);

      }
    }
  }

  private static SearchAlgorithm getAlgorithm(String algorithmChoice)
      throws InstantiationException, IllegalAccessException {
    Reflections r = Util.getReflections();
    Set<Class<?>> algorithms = r.getTypesAnnotatedWith(Algorithm.class);
    Optional<?> algorithmClass = algorithms.stream()
        .map(cl -> {
          try {
            return cl.newInstance();
          } catch (IllegalAccessException | InstantiationException e) {
            logger.error(e);
          }
          return null;
        })
        .filter(obj -> ((SearchAlgorithm) obj).readableName().equals(algorithmChoice))
        .findFirst();
    if (algorithmClass.isPresent()) {
      SearchAlgorithm algorithm = (SearchAlgorithm) algorithmClass.get();
      List<Method> requirements = Framework.getPrerequisites(algorithm);
      boolean anyFail = false;
      for (Method requirement : requirements) {
        try {
          boolean passed = (boolean) requirement.invoke(null, null);
          if (!passed) {
            anyFail = true;
            String error = requirement.getAnnotation(Prerequisite.class).failureMessage();
            logger.error("System is improperly configured: " + error);
          }
        } catch (InvocationTargetException e) {
          logger.error(e);
        }
      }
      if (anyFail) {
        throw new SystemConfigurationException("");
      }
      return algorithm;
    }
    List<String> algorithmNames = Framework.getAvailableAlgorithms().stream()
        .map(alg -> alg.readableName())
        .collect(Collectors.toList());
    throw new RuntimeException(
        "Algorithm could not be created. The list of available algorithms is given as: "
            + algorithmNames.stream().reduce((s, s2) -> s + "\n" + s2));

  }

}
