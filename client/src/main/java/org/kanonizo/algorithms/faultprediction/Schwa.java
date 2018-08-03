package org.kanonizo.algorithms.faultprediction;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.TestCasePrioritiser;
import org.kanonizo.algorithms.heuristics.comparators.AdditionalGreedyComparator;
import org.kanonizo.algorithms.heuristics.comparators.GreedyComparator;
import org.kanonizo.algorithms.heuristics.comparators.RandomComparator;
import org.kanonizo.algorithms.heuristics.comparators.ConstraintSolverComparator;
import org.kanonizo.algorithms.heuristics.comparators.SimilarityComparator;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.annotations.ConditionalParameter;
import org.kanonizo.annotations.OptionProvider;
import org.kanonizo.annotations.Prerequisite;
import org.kanonizo.framework.ClassStore;
import org.kanonizo.framework.ObjectiveFunction;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.util.Util;

@Algorithm
public class Schwa extends TestCasePrioritiser {

  private static Logger logger = LogManager.getLogger(Schwa.class);

  @Parameter(key = "schwa_revisions_weight", description = "How much influence the number of revisions to a file should have over its likelihood of containing a fault", category = "schwa")
  @ConditionalParameter(condition = "Schwa.SCHWA_FILE == null", listensTo = "SCHWA_FILE")
  public static double REVISIONS_WEIGHT = 0.3;

  @Parameter(key = "schwa_authors_weight", description = "How much influence the number of authors= who have committed to a file should have over its likelihood of containing a fault", category = "schwa")
  @ConditionalParameter(condition = "Schwa.SCHWA_FILE == null", listensTo = "SCHWA_FILE")
  public static double AUTHORS_WEIGHT = 0.2;

  @Parameter(key = "schwa_fixes_weight", description = "How much influence the number of times a file has been associated with a \"fix\" should have over its likelihood of containing a fault", category = "schwa")
  @ConditionalParameter(condition = "Schwa.SCHWA_FILE == null", listensTo = "SCHWA_FILE")
  public static double FIXES_WEIGHT = 0.5;

  @Parameter(key = "schwa_secondary_objective", description = "Since Schwa tells us the likelihood of each class/method containing a fault, we discover the test cases that execute that area of code. However, a secondary objective can allow us to prioritise test cases within the set of test cases that cover a faulty objective", category = "schwa", hasOptions = true)
  public static ObjectiveFunction secondaryObjective;

  @Parameter(key = "classes_per_group", description = "Schwa tells us the likelihood of classes containing a fault - prioritising using this information involves finding all tests that execute a faulty class. This variable controls how many classes to \"group\" together when finding test cases to prioritise", category = "schwa")
  public static int CLASSES_PER_GROUP = 1;

  @Parameter(key = "use_percentage_classes", description = "Whether to select a raw number of classes or a percentage of classes using -Dclasses_per_group", category = "Schwa")
  public static boolean usePercentageClasses = true;

  @Parameter(key = "schwa_file", description = "Running schwa creates a json file containing the probabilities of faults in every class. If this has already been created then it can be used instead of running Schwa from Kanonizo", category = "schwa")
  public static File SCHWA_FILE = null;

  private List<SchwaClass> classes;
  private List<SchwaClass> active = new ArrayList<>();
  private List<TestCase> testCasesForActive = new ArrayList<>();
  private int totalClasses;

  private static boolean validSchwaFile() {
    return SCHWA_FILE != null && SCHWA_FILE.exists() && SCHWA_FILE.getAbsolutePath()
        .endsWith(".json");
  }

  public void init(List<TestCase> testCases) {
    super.init(testCases);
    // run schwa
    try {
      boolean createTemp = !validSchwaFile();
      if (createTemp) {
        SCHWA_FILE = File.createTempFile("schwa-json-output", ".tmp");
        Framework.getInstance().getDisplay().notifyTaskStart("Running Schwa", true);
        runProcess(SCHWA_FILE, "schwa", fw.getRootFolder().getAbsolutePath(), "-j");
        Framework.getInstance().getDisplay().reportProgress(1, 1);
      }
      Gson gson = new Gson();
      SchwaRoot root = gson.fromJson(new FileReader(SCHWA_FILE), SchwaRoot.class);
      classes = root.getChildren().stream().filter(
          cl -> cl.getPath().endsWith(".java") && getClassFile(cl.getPath()) != null
              && ClassStore.get(getClassName(getClassFile(cl.getPath()))) != null)
          .collect(Collectors.toList());
      totalClasses = classes.size();
      if (classes.isEmpty()) {
        logger.error(
            "No classes remaining. Is the project root set correctly so that we can identify java files from the Schwa output?");
      }
      // sort classes by probability of containing a fault
      Collections.sort(classes, Comparator.comparingDouble(o -> 1 - o.getProb()));
      if (createTemp) {
        SCHWA_FILE.delete();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public TestCase selectTestCase(List<TestCase> testCases) {
    while (testCasesForActive.size() == 0) {
      if (classes.size() > 0) {
        // pick next n classes to prioritise tests for, either CLASSES_PER_GROUP or all remaining classes
        active.clear();
        int classesToSelect = Math
            .min(usePercentageClasses ? totalClasses * CLASSES_PER_GROUP / 100 : CLASSES_PER_GROUP,
                classes.size());
        for (int i = 0; i < classesToSelect; i++) {
          active.add(classes.get(i));
        }
        if (secondaryObjective != null && secondaryObjective.needsTargetClass()) {
          secondaryObjective.setTargetClasses(
              active.stream().map(cl -> ClassStore.get(getClassName(getClassFile(cl.getPath()))).getCUT()).collect(Collectors.toList()));
        }
        classes.removeAll(active);
        // grab covering test cases
        testCasesForActive = getTestsCoveringClasses(testCases, active);
      } else {
        // if there are no more classes reported by Schwa, just add test cases in the order we found them
        testCasesForActive = testCases;
      }
      if (secondaryObjective != null) {
        // secondary objective sort for test cases
        testCasesForActive = secondaryObjective.sort(testCasesForActive);
      }
    }
    TestCase next = testCasesForActive.get(0);
    testCasesForActive.remove(next);
    return next;
  }

  private List<TestCase> getTestsCoveringClasses(List<TestCase> candidates,
      List<SchwaClass> classes) {
    Set<TestCase> tests = new HashSet<>();
    Iterator<SchwaClass> it = classes.iterator();
    while (it.hasNext()) {
      SchwaClass cl = it.next();
      String filePath = cl.getPath();
      tests.addAll(getTestsCoveringClass(candidates, filePath));
    }
    return new ArrayList<>(tests);
  }

  private File getClassFile(String filePath) {
    String fullPath = fw.getRootFolder().getAbsolutePath() + File.separator + filePath;
    File javaFile = new File(fullPath);
    if (!javaFile.exists()) {
      return null;
    }
    String className = getClassName(javaFile);
    ClassUnderTest cut = ClassStore.get(className);
    if (cut == null) {
      return null;
    }

    return javaFile;
  }

  private String getClassName(File javaFile) {
    try {
      List<String> lines = Files.readLines(javaFile, Charset.defaultCharset());
      Optional<String> pkgOpt = lines.stream().filter(line -> line.startsWith("package"))
          .findFirst();
      String pkg = pkgOpt.map(p -> p.substring("package".length() + 1, p.length() - 1)).orElse("");
      String className = (!pkg.isEmpty() ? pkg + "." : "") + (javaFile.getAbsolutePath()
          .substring(javaFile.getAbsolutePath().lastIndexOf(File.separatorChar) + 1,
              javaFile.getAbsolutePath().length() - ".java".length()));
      return className;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
  }

  private List<TestCase> getTestsCoveringClass(List<TestCase> candidates, String filePath) {
    if (getClassFile(filePath) == null) {
      return Collections.emptyList();
    }

    File javaFile = getClassFile(filePath);
    String className = getClassName(javaFile);
    ClassUnderTest cut = ClassStore.get(className);
    if (cut == null) {
      // test class
      return Collections.emptyList();
    }
    Set<Line> linesInCut = inst.getLines(cut);
    return candidates.stream()
        .filter(tc -> inst.getLinesCovered(tc).stream().anyMatch(l -> linesInCut.contains(l)))
        .collect(
            Collectors.toList());
  }

  @OptionProvider(paramKey = "schwa_secondary_objective")
  public static List<ObjectiveFunction> getOptions() {
    ArrayList<ObjectiveFunction> options = new ArrayList<>();
    options.add(new GreedyComparator());
    options.add(new AdditionalGreedyComparator());
    options.add(new RandomComparator());
    options.add(new ConstraintSolverComparator());
    options.add(new SimilarityComparator());
    return options;
  }

  @Prerequisite(failureMessage = "Feature weights do not add up to 1. -Dschwa_revisions_weight, -Dschwa_authors_weight and -Dschwa_fixes_weight should sum to 1")
  public static boolean checkWeights() {
    return validSchwaFile() || Util
        .doubleEquals(REVISIONS_WEIGHT + AUTHORS_WEIGHT + FIXES_WEIGHT, 1);
  }

  @Prerequisite(failureMessage = "Python3 is not installed on this system or is not executable on the system path. Please check your python3 installation.")
  public static boolean checkPythonInstallation() {
    if (validSchwaFile()) {
      return true;
    }
    int returnCode = runProcess("python3", "--version");
    return returnCode == 0;

  }

  @Prerequisite(failureMessage = "Schwa is not installed on this system, and Kanonizo failed to install it. Try again or visit Schwa on GitHub (https://github.com/andrefreitas/schwa) to manually install")
  public static boolean checkSchwaInstallation() {
    if (validSchwaFile()) {
      return true;
    }
    int returnCode = runProcess("schwa", "-h");
    if (returnCode != 0) {
      returnCode = runProcess("python3", "-m", "schwa", "-h");
    }
    //TODO install Schwa if not present on users system??
    return returnCode == 0;
  }

  @Prerequisite(failureMessage = "In order to use Schwa, project root must be set. The project root must be a git repository")
  public static boolean checkProjectRoot() {
    if (validSchwaFile()) {
      return true;
    }
    return Framework.getInstance().getRootFolder() != null && isGitRepository(
        Framework.getInstance().getRootFolder());
  }

  private static boolean isGitRepository(File root) {
    return root.listFiles((n) -> n != null && n.getName().equals(".git")).length > 0;
  }

  private static int runProcess(String... process) {
    return runProcess(null, process);
  }

  private static int runProcess(File output, String... process) {
    ProcessBuilder pb = new ProcessBuilder(process);
    int returnCode = -1;
    try {
      if (output != null) {
        pb.redirectOutput(output);
      }
      Process processRun = pb.start();
      returnCode = processRun.waitFor();
    } catch (IOException e) {
      return returnCode;
    } catch (InterruptedException e) {
      return returnCode;
    }
    return returnCode;
  }

  @Override
  public String readableName() {
    return "schwa";
  }
}
