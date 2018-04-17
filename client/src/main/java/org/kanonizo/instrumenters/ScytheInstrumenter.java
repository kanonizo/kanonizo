package org.kanonizo.instrumenters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.scythe.instrumenter.InstrumentationProperties;
import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import com.scythe.instrumenter.analysis.ClassAnalyzer;
import com.scythe.instrumenter.instrumentation.ClassReplacementTransformer;
import com.scythe.instrumenter.instrumentation.InstrumentingClassLoader;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.Framework;
import org.kanonizo.framework.ClassStore;
import org.kanonizo.framework.TestCaseStore;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Branch;
import org.kanonizo.framework.objects.BranchStore;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.objects.Goal;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.LineStore;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.util.HashSetCollector;
import org.kanonizo.util.Util;

//
@org.kanonizo.annotations.Instrumenter
public class ScytheInstrumenter implements Instrumenter {

  @Parameter(key = "scythe_write", description = "Whether to write coverage to a file. If true, "
      + "uses the values of SCYTHE_FILE and SCYTHE_OUTPUT_DIR to determine where the file"
      + "should be written", category = "Instrumentation")
  public static boolean SCYTHE_WRITE = false;
  @Parameter(key = "scythe_read", description =
      "Whether to write a coverage file after execution of"
          + "the test cases. If true, uses the values of SCYTHE_FILE and SCYTHE_OUTPUT_DIR to determine"
          + "where the file should be written", category = "Instrumentation")
  public static boolean SCYTHE_READ = false;
  @Parameter(key = "scythe_filename", description =
      "Name of the file to read if SCYTHE_READ is true or write"
          + "if SCYTHE_WRITE is true, used to contain coverage information", category = "Instrumentation")
  public static File SCYTHE_FILE = new File("scythe_coverage.json");

  private TestSuite testSuite;
  private static Logger logger = LogManager.getLogger(ScytheInstrumenter.class);;
  private Map<TestCase, Set<org.kanonizo.framework.objects.Line>> linesCovered = new HashMap<>();
  private Map<TestCase, Set<org.kanonizo.framework.objects.Branch>> branchesCovered = new HashMap<>();

  private static final String[] forbiddenPackages = new String[]{"org/kanonizo", "org/junit",
      "org/apache/commons/cli", "junit", "org/apache/bcel", "org/apache/logging/log4j",
      "org/objectweb/asm",
      "javax/swing", "javax/servlet", "org/xml", "org/hamcrest"};

  public ScytheInstrumenter() {
    Arrays.asList(forbiddenPackages).stream()
        .forEach(s -> ClassReplacementTransformer.addForbiddenPackage(s));
  }

  private static void reportException(Exception e) {
    logger.error(e);
  }

  private boolean validReadFile() {
    return SCYTHE_FILE.exists();
  }

  @Override
  public Class<?> loadClass(String className) throws ClassNotFoundException {
    return InstrumentingClassLoader.getInstance().loadClass(className);
  }

  @Override
  public void setTestSuite(TestSuite ts) {
    this.testSuite = ts;
  }


  @Override
  public void collectCoverage() {
    if (SCYTHE_READ) {
      if (SCYTHE_FILE.exists()) {
        Gson gson = getGson();
        try {
          Framework.getInstance().getDisplay().notifyTaskStart("Reading Coverage File", true);
          ScytheInstrumenter inst = gson
              .fromJson(new FileReader(SCYTHE_FILE), ScytheInstrumenter.class);
          // removing loading window
          Framework.getInstance().getDisplay().reportProgress(1, 1);
          this.linesCovered = inst.linesCovered;
          this.branchesCovered = inst.branchesCovered;
          this.testSuite = inst.testSuite;
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        }
      } else {
        throw new RuntimeException(
            "Scythe Coverage file is missing. Ensure that -Dscythe_output_dir exists and -Dscythe_filename exists within that directory");
      }
    } else {
      try {
        Util.suppressOutput();
        Framework.getInstance().getDisplay().notifyTaskStart("Running Test Cases", true);
        for (TestCase testCase : testSuite.getTestCases()) {
          try {
            testCase.run();
            // debug code to find out where/why failures are occurring. Use
            // breakpoints after execution to locate failures
            ClassAnalyzer.collectHitCounters(true);
            linesCovered.put(testCase, collectLines(testCase));
            branchesCovered.put(testCase, collectBranches(testCase));
            ClassAnalyzer.resetCoverage();
            Framework
                .getInstance().getDisplay()
                .reportProgress((double) testSuite.getTestCases().indexOf(testCase) + 1,
                    testSuite.getTestCases().size());
          } catch (Throwable e) {
            e.printStackTrace();
            // as much as I hate to catch throwables, it has to be done in this
            // instance because not all tests can be guaranteed to run at all
            // properly, and sometimes the Java API will
            // shutdown without this line
          }

        }
        Util.resumeOutput();
        System.out.println("");
        logger.info("Finished instrumentation");
      } catch (final Exception e) {
        // runtime startup exception
        reportException(e);
      }
      if (!SCYTHE_READ && SCYTHE_WRITE) {
        if (!SCYTHE_FILE.exists()) {
          try {
            SCYTHE_FILE.createNewFile();
          } catch (IOException e) {
            logger.debug("Failed to create coverage file");
          }
        }
        Gson gson = getGson();
        String serialised = gson.toJson(this);
        try {
          BufferedOutputStream out = new BufferedOutputStream(
              new FileOutputStream(SCYTHE_FILE, false));
          out.write(serialised.getBytes());
          out.flush();
        } catch (FileNotFoundException e) {
          logger.debug("Output file is missing!");
          e.printStackTrace();
        } catch (IOException e) {
          logger.debug("Failed to write output");
          e.printStackTrace();
        }
      }
    }
  }

  private Gson getGson() {
    GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(ScytheInstrumenter.class, new ScytheTypeWriter());
    return builder.create();
  }

  private Set<org.kanonizo.framework.objects.Line> collectLines(TestCase testCase) {
    Set<org.kanonizo.framework.objects.Line> covered = new HashSet<>();
    List<Class<?>> changedClasses = ClassAnalyzer.getChangedClasses();
    for (Class<?> cl : changedClasses) {
      if (ClassStore.get(cl.getName()) != null) {
        ClassUnderTest parent = ClassStore.get(cl.getName());
        List<com.scythe.instrumenter.instrumentation.objectrepresentation.Line> lines = ClassAnalyzer
            .getCoverableLines(cl.getName());
        Set<com.scythe.instrumenter.instrumentation.objectrepresentation.Line> linesCovered = lines
            .stream().filter(line -> line.getHits() > 0)
            .collect(Collectors.toSet());
        Set<org.kanonizo.framework.objects.Line> kanLines = linesCovered
            .stream()
            .map(line -> LineStore.with(parent, line.getLineNumber()))
            .collect(Collectors.toSet());
        covered.addAll(kanLines);
      }
    }
    return covered;
  }

  private Set<org.kanonizo.framework.objects.Branch> collectBranches(TestCase testCase) {
    Set<org.kanonizo.framework.objects.Branch> covered = new HashSet<>();
    List<Class<?>> changedClasses = ClassAnalyzer.getChangedClasses();
    for (Class<?> cl : changedClasses) {
      if (ClassStore.get(cl.getName()) != null) {
        ClassUnderTest parent = ClassStore.get(cl.getName());
        List<com.scythe.instrumenter.instrumentation.objectrepresentation.Branch> branches = ClassAnalyzer
            .getCoverableBranches(cl.getName());
        Set<com.scythe.instrumenter.instrumentation.objectrepresentation.Branch> branchesCovered = branches
            .stream().filter(branch -> branch.getHits() > 0)
            .collect(Collectors.toSet());
        Set<Branch> kanBranches = branchesCovered
            .stream()
            .map(branch -> BranchStore.with(parent, branch.getLineNumber(), branch.getGoalId()))
            .collect(Collectors.toSet());
        covered.addAll(kanBranches);
      }
    }
    return covered;
  }

  @Override
  public Set<Line> getLinesCovered(TestCase testCase) {
    return linesCovered.containsKey(testCase) ? linesCovered.get(testCase) : new HashSet<>();
  }

  @Override
  public Set<Branch> getBranchesCovered(TestCase testCase) {
    return branchesCovered.containsKey(testCase) ? branchesCovered.get(testCase) : new HashSet<>();
  }

  @Override
  public int getTotalLines(ClassUnderTest cut) {
    return ClassAnalyzer.getCoverableLines(cut.getCUT().getName()).size();
  }

  @Override
  public int getTotalBranches(ClassUnderTest cut) {
    return ClassAnalyzer.getCoverableBranches(cut.getCUT().getName()).size();
  }

  @Override
  public Set<org.kanonizo.framework.objects.Line> getLines(ClassUnderTest cut) {
    return ClassAnalyzer.getCoverableLines(cut.getCUT().getName()).stream()
        .map(line -> LineStore.with(cut, line.getLineNumber())).collect(Collectors.toSet());
  }

  @Override
  public Set<org.kanonizo.framework.objects.Branch> getBranches(ClassUnderTest cut) {
    return ClassAnalyzer.getCoverableBranches(cut.getCUT().getName()).stream()
        .map(branch -> BranchStore.with(cut, branch.getLineNumber(), branch.getGoalId()))
        .collect(Collectors.toSet());
  }

  @Override
  public int getTotalLines(SystemUnderTest sut) {
    return sut.getClassesUnderTest().stream()
        .mapToInt(cut -> ClassAnalyzer.getCoverableLines(cut.getCUT().getName()).size()).sum();
  }

  @Override
  public int getLinesCovered(TestSuite testSuite) {
    Set<org.kanonizo.framework.objects.Line> covered = new HashSet<>();
    testSuite.getTestCases().stream().forEach(testCase -> {
      covered.addAll(getLinesCovered(testCase));
    });
    return covered.size();
  }

  @Override
  public int getTotalBranches(SystemUnderTest sut) {
    return sut.getClassesUnderTest().stream()
        .mapToInt(cut -> ClassAnalyzer.getCoverableBranches(cut.getCUT().getName()).size())
        .sum();
  }

  @Override
  public int getBranchesCovered(TestSuite testSuite) {
    Set<org.kanonizo.framework.objects.Branch> covered = new HashSet<>();
    testSuite.getTestCases().stream().forEach(testCase -> {
      covered.addAll(getBranchesCovered(testCase));
    });
    return covered.size();
  }

  @Override
  public Set<Line> getLinesCovered(ClassUnderTest cut) {
    return linesCovered.values().stream().map(
        set -> set.stream().filter(line -> line.getParent().equals(cut))
            .collect(Collectors.toSet())).collect(new HashSetCollector<>());
  }

  @Override
  public Set<Line> getLinesCovered(SystemUnderTest sut) {
    return sut.getClassesUnderTest().stream().map(cut -> getLinesCovered(cut))
        .collect(new HashSetCollector<>());
  }

  @Override
  public Set<Branch> getBranchesCovered(ClassUnderTest cut) {
    return branchesCovered.values().stream().map(
        set -> set.stream().filter(line -> line.getParent().equals(cut))
            .collect(Collectors.toSet())).collect(new HashSetCollector<>());
  }

  @Override
  public Set<Branch> getBranchesCovered(SystemUnderTest sut) {
    return sut.getClassesUnderTest().stream().map(cut -> getBranchesCovered(cut))
        .collect(new HashSetCollector<>());
  }

  @Override
  public ClassLoader getClassLoader() {
    return InstrumentingClassLoader.getInstance();
  }

  @Override
  public String readableName() {
    return "Scythe";
  }

  private class ScytheTypeWriter extends TypeAdapter<ScytheInstrumenter> {

    @Override
    public void write(JsonWriter out, ScytheInstrumenter inst)
        throws IOException {
      out.beginObject();
      out.name("linesCovered");
      writeCoverage(out, inst.linesCovered);
      out.name("branchesCovered");
      writeCoverage(out, inst.branchesCovered);
      out.name("testSuite");
      out.beginArray();
      for (TestCase tc : inst.testSuite.getTestCases()) {
        out.value(tc.toString());
      }
      out.endArray();
      out.endObject();
    }

    private <T extends Goal> void writeCoverage(JsonWriter out, Map<TestCase, Set<T>> coverage)
        throws IOException {
      out.beginObject();
      List<TestCase> orderedTestCases = new ArrayList<>(coverage.keySet());
      Collections.sort(orderedTestCases, Comparator.comparing(TestCase::getId));
      Iterator<TestCase> testCases = orderedTestCases.iterator();
      // tests
      while (testCases.hasNext()) {
        TestCase tc = testCases.next();
        out.name(tc.toString());
        Set<ClassUnderTest> classesCovered = coverage.get(tc).stream().map(goal -> goal.getParent())
            .collect(Collectors.toSet());
        out.beginObject();
        for (ClassUnderTest cut : classesCovered) {
          out.name(Integer.toString(cut.getId()));
          out.beginArray();
          Set<Goal> goalsCovered = coverage.get(tc).stream()
              .filter(goal -> goal.getParent().equals(cut)).collect(Collectors.toSet());
          for (Goal g : goalsCovered) {
            if (g instanceof Line) {
              out.value(g.getLineNumber());
            } else if (g instanceof Branch) {
              Double value = Double
                  .parseDouble(((Branch) g).getLineNumber() + "." + ((Branch) g).getBranchNumber());
              out.value(value);
            }
          }
          out.endArray();
        }
        out.endObject();
      }
      out.endObject();
    }

    @Override
    public ScytheInstrumenter read(JsonReader in) throws IOException {
      final ScytheInstrumenter inst = new ScytheInstrumenter();
      inst.testSuite = new TestSuite();
      in.beginObject();
      while (in.hasNext()) {
        switch (in.nextName()) {
          case "linesCovered":
            inst.linesCovered = readCoverage(in);
            break;
          case "branchesCovered":
            inst.branchesCovered = readCoverage(in);
            break;
          case "testSuite":
            in.beginArray();
            while (in.hasNext()) {
              String testString = in.nextString();
              TestCase test = TestCaseStore.with(testString);
              inst.testSuite.addTestCase(test);
              if (test == null) {
                logger.debug("Error deserialising test case " + testString + ".");
              }
            }
            in.endArray();
        }
      }
      in.endObject();
      return inst;
    }

    private <T extends Goal> Map<TestCase, Set<T>> readCoverage(
        JsonReader in) throws IOException {
      HashMap<TestCase, Set<T>> returnMap = new HashMap<>();

      // test cases
      in.beginObject();
      while (in.hasNext()) {
        String testString = in.nextName();
        TestCase tc = TestCaseStore.with(testString);
        if (tc == null) {
          logger.debug("Error deserialising test case " + testString + ".");
        }
        Set<T> linesCovered = new HashSet<>();
        // classes
        in.beginObject();
        while (in.hasNext()) {
          int cutId = Integer.parseInt(in.nextName());
          in.beginArray();
          while (in.hasNext()) {
            ClassUnderTest cut = ClassStore.get(cutId);
            double goalNumber = in.nextDouble();
            if (goalNumber == (int) goalNumber) {
              linesCovered.add((T) LineStore.with(cut, (int) goalNumber));
            } else {
              int lineNumber = (int) goalNumber;
              int branchNumber = (int) (goalNumber - lineNumber);
              linesCovered.add((T) BranchStore.with(cut, lineNumber, branchNumber));
            }
          }
          in.endArray();
        }
        in.endObject();
        returnMap.put(tc, linesCovered);
      }
      in.endObject();
      return returnMap;
    }
  }

}