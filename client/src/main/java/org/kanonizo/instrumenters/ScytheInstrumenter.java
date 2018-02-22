package org.kanonizo.instrumenters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.Expose;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.scythe.instrumenter.InstrumentationProperties;
import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import com.scythe.instrumenter.analysis.ClassAnalyzer;
import com.scythe.instrumenter.instrumentation.ClassReplacementTransformer;
import com.scythe.instrumenter.instrumentation.ClassReplacementTransformer.ShouldInstrumentChecker;
import com.scythe.instrumenter.instrumentation.InstrumentingClassLoader;
import com.scythe.instrumenter.instrumentation.objectrepresentation.Branch;
import com.scythe.instrumenter.instrumentation.objectrepresentation.Line;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
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
import org.junit.runner.notification.Failure;
import org.kanonizo.commandline.ProgressBar;
import org.kanonizo.framework.ClassStore;
import org.kanonizo.framework.TestCaseStore;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.BranchStore;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.objects.Goal;
import org.kanonizo.framework.objects.LineStore;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.util.HashSetCollector;
import org.kanonizo.util.NullPrintStream;
import org.kanonizo.util.Util;

//
@org.kanonizo.annotations.Instrumenter(readableName = "Scythe")
public class ScytheInstrumenter implements Instrumenter {

  @Parameter(key = "scythe_write", description = "Whether to write coverage to a file. If true, "
      + "uses the values of SCYTHE_FILENAME and SCYTHE_OUTPUT_DIR to determine where the file"
      + "should be written", category = "Instrumentation")
  public static boolean SCYTHE_WRITE = false;
  @Parameter(key = "scythe_read", description =
      "Whether to write a coverage file after execution of"
          + "the test cases. If true, uses the values of SCYTHE_FILENAME and SCYTHE_OUTPUT_DIR to determine"
          + "where the file should be written", category = "Instrumentation")
  public static boolean SCYTHE_READ = false;
  @Parameter(key = "scythe_filename", description =
      "Name of the file to read if SCYTHE_READ is true or write"
          + "if SCYTHE_WRITE is true, used to contain coverage information", category = "Instrumentation")
  public static String SCYTHE_FILENAME = "scythe_coverage.json";
  @Parameter(key = "scythe_output_dir", description =
      "Directory containing scythe coverage files or directory"
          + "to write coverage file(s) to", category = "Instrumentation")
  public static String SCYTHE_OUTPUT_DIR = "";

  private TestSuite testSuite;
  private static Logger logger;
  @Expose
  private Map<TestCase, Set<org.kanonizo.framework.objects.Line>> linesCovered = new HashMap<>();
  @Expose
  private Map<TestCase, Set<org.kanonizo.framework.objects.Branch>> branchesCovered = new HashMap<>();

  static {
    logger = LogManager.getLogger(ScytheInstrumenter.class);

    InstrumentationProperties.INSTRUMENT_BRANCHES = false;
    ClassReplacementTransformer.addShouldInstrumentChecker(new ShouldInstrumentChecker() {

      private boolean isTestClass(String className) {

        try {
          Class<?> cl = ClassLoader.getSystemClassLoader()
              .loadClass(className.replaceAll("/", "."));
          return Util.isTestClass(cl);
        } catch (ClassNotFoundException e) {
          e.printStackTrace();
        }
        return false;
      }

      @Override
      public boolean shouldInstrument(String className) {
        return !isTestClass(className);
      }
    });
  }

  private static void reportException(Exception e) {
    logger.error(e);
  }

  private boolean validReadFile() {
    String path = SCYTHE_OUTPUT_DIR + File.separator + SCYTHE_FILENAME;
    File readFile = new File(path);
    return readFile.exists();
  }

  @Override
  public Class<?> loadClass(String className) throws ClassNotFoundException {
    if (SCYTHE_READ && validReadFile()) {
      return ClassLoader.getSystemClassLoader().loadClass(className);
    }
    return InstrumentingClassLoader.getInstance().loadClass(className);
  }

  @Override
  public void setTestSuite(TestSuite ts) {
    this.testSuite = ts;
  }

  private File getScytheFile() {
    StringBuilder sb = new StringBuilder();
    sb.append(SCYTHE_OUTPUT_DIR);
    if (!sb.toString().isEmpty()) {
      sb.append(File.separator);
    }
    sb.append(SCYTHE_FILENAME);
    String path = sb.toString();
    return new File(path);
  }

  @Override
  public void collectCoverage() {
    if (SCYTHE_READ) {
      File scytheCoverage = getScytheFile();
      if (scytheCoverage.exists()) {
        Gson gson = getGson();
        try {
          ScytheInstrumenter inst = gson
              .fromJson(new FileReader(scytheCoverage), ScytheInstrumenter.class);
          this.linesCovered = inst.linesCovered;
          this.branchesCovered = inst.branchesCovered;
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        }
      } else {
        throw new RuntimeException(
            "Scythe Coverage file is missing. Ensure that -Dscythe_output_dir exists and -Dscythe_filename exists within that directory");
      }
    } else {
      try {
        List<Failure> failures = new ArrayList<>();
        PrintStream defaultSysOut = java.lang.System.out;
        PrintStream defaultSysErr = java.lang.System.err;
        // ensure coverage data is collected
        java.lang.System.setOut(NullPrintStream.instance);
        java.lang.System.setErr(NullPrintStream.instance);
        ProgressBar bar = new ProgressBar(defaultSysOut);
        bar.setTitle("Running Test Cases");
        for (TestCase testCase : testSuite.getTestCases()) {
          try {
            testCase.run();
            // debug code to find out where/why failures are occurring. Use
            // breakpoints after execution to locate failures
            if (testCase.hasFailures()) {
              failures.addAll(testCase.getFailures());
            }
            bar.reportProgress((double) testSuite.getTestCases().indexOf(testCase) + 1,
                testSuite.getTestCases().size());
            ClassAnalyzer.collectHitCounters(true);
            linesCovered.put(testCase, collectLines(testCase));
            branchesCovered.put(testCase, collectBranches(testCase));
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
        java.lang.System.setOut(defaultSysOut);
        java.lang.System.setErr(defaultSysErr);
      } catch (final Exception e) {
        // runtime startup exception
        reportException(e);
      }
      if (!SCYTHE_READ && SCYTHE_WRITE) {
        File scytheCoverage = getScytheFile();
        if (!scytheCoverage.exists()) {
          try {
            scytheCoverage.createNewFile();
          } catch (IOException e) {
            logger.debug("Failed to create coverage file");
          }
        }
        Gson gson = getGson();
        String serialised = gson.toJson(this);
        try {
          BufferedOutputStream out = new BufferedOutputStream(
              new FileOutputStream(scytheCoverage, false));
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
    builder.excludeFieldsWithoutExposeAnnotation();
    builder.registerTypeAdapter(ScytheInstrumenter.class, new ScytheTypeWriter());
    return builder.create();
  }

  private Set<org.kanonizo.framework.objects.Line> collectLines(TestCase testCase) {
    Set<org.kanonizo.framework.objects.Line> covered = new HashSet<>();
    List<Class<?>> changedClasses = ClassAnalyzer.getChangedClasses();
    for (Class<?> cl : changedClasses) {
      if (ClassStore.get(cl.getName()) != null) {
        ClassUnderTest parent = ClassStore.get(cl.getName());
        List<Line> lines = ClassAnalyzer.getCoverableLines(cl.getName());
        Set<Line> linesCovered = lines.stream().filter(line -> line.getHits() > 0)
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
        List<Branch> branches = ClassAnalyzer.getCoverableBranches(cl.getName());
        Set<Branch> branchesCovered = branches.stream().filter(branch -> branch.getHits() > 0)
            .collect(Collectors.toSet());
        Set<org.kanonizo.framework.objects.Branch> kanBranches = branchesCovered
            .stream()
            .map(branch -> BranchStore.with(parent, branch.getLineNumber(), branch.getGoalId()))
            .collect(Collectors.toSet());
        covered.addAll(kanBranches);
      }
    }
    return covered;
  }

  @Override
  public Set<org.kanonizo.framework.objects.Line> getLinesCovered(TestCase testCase) {
    return linesCovered.get(testCase);
  }

  @Override
  public Set<org.kanonizo.framework.objects.Branch> getBranchesCovered(TestCase testCase) {
    return branchesCovered.get(testCase);
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
        .map(branch -> BranchStore.with(cut, branch.getLineNumber(), branch.getGoalId())).collect(Collectors.toSet());
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
  public Set<org.kanonizo.framework.objects.Line> getLinesCovered(ClassUnderTest cut) {
    return cut.getLines().stream().filter(line -> line.getCoveringTests().size() > 0).collect(Collectors.toSet());
  }

  @Override
  public Set<org.kanonizo.framework.objects.Line> getLinesCovered(SystemUnderTest sut) {
    return sut.getClassesUnderTest().stream().map(cut -> getLinesCovered(cut)).collect(new HashSetCollector<>());
  }

  @Override
  public Set<org.kanonizo.framework.objects.Branch> getBranchesCovered(ClassUnderTest cut) {
    return cut.getLines().stream().map(
        line -> line.getBranches().stream().filter(
            branch -> branch.getCoveringTests().size() > 0)
            .collect(Collectors.toSet()))
        .collect(new HashSetCollector<>());
  }

  @Override
  public Set<org.kanonizo.framework.objects.Branch> getBranchesCovered(SystemUnderTest sut) {
    return sut.getClassesUnderTest().stream().map(cut -> getBranchesCovered(cut)).collect(new HashSetCollector<>());
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
      out.endObject();
    }

    private <T extends Goal> void writeCoverage(JsonWriter out, Map<TestCase, Set<T>> coverage) throws IOException {
      out.beginObject();
      List<TestCase> orderedTestCases = new ArrayList<>(coverage.keySet());
      Collections.sort(orderedTestCases, Comparator.comparing(TestCase::getId));
      Iterator<TestCase> testCases = orderedTestCases.iterator();
      // tests
      while (testCases.hasNext()) {
        TestCase tc = testCases.next();
        out.name(Integer.toString(tc.getId()));
        out.beginArray();
        Iterator<T> goals = coverage.get(tc).iterator();
        while (goals.hasNext()) {
          out.beginObject();
          T l = goals.next();
          out.name(Integer.toString(l.getParent().getId()));
          out.beginArray();
          out.value(l.getLineNumber());
          if (l instanceof org.kanonizo.framework.objects.Branch) {
            out.value(((org.kanonizo.framework.objects.Branch) l).getBranchNumber());
          }
          out.endArray();
          out.endObject();
        }
        out.endArray();
      }
//      out.name("branchesCovered").value(inst.coveredString(inst.branchesCovered));
      out.endObject();
    }

    @Override
    public ScytheInstrumenter read(JsonReader in) throws IOException {
      final ScytheInstrumenter inst = new ScytheInstrumenter();
      in.beginObject();
      while (in.hasNext()) {
        switch (in.nextName()) {
          case "linesCovered":
            inst.linesCovered = readCoverage(in);
            break;
          case "branchesCovered":
            inst.branchesCovered = readCoverage(in);
            break;
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
        int testCaseId = Integer.parseInt(in.nextName());
        TestCase tc = TestCaseStore.get(testCaseId);
        Set<T> linesCovered = new HashSet<>();
        in.beginArray();
        // lines
        while (in.hasNext()) {
          in.beginObject();
          while (in.hasNext()) {
            int cutId = Integer.parseInt(in.nextName());
            in.beginArray();
            ClassUnderTest cut = ClassStore.get(cutId);
            int lineNumber = in.nextInt();
            if (in.hasNext()) {
              int branchNumber = in.nextInt();
              linesCovered.add((T) BranchStore.with(cut, lineNumber, branchNumber));
            } else {
              linesCovered.add((T) LineStore.with(cut, lineNumber));
            }
            in.endArray();
          }
          in.endObject();
        }
        in.endArray();
        returnMap.put(tc, linesCovered);
      }
      in.endObject();
      return returnMap;
    }
  }

}