package org.kanonizo.instrumenters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;
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
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.notification.Failure;
import org.kanonizo.commandline.ProgressBar;
import org.kanonizo.framework.CUTChromosome;
import org.kanonizo.framework.CUTChromosomeStore;
import org.kanonizo.framework.SUTChromosome;
import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestCaseChromosomeStore;
import org.kanonizo.framework.TestSuiteChromosome;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.util.NullPrintStream;

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

  private TestSuiteChromosome testSuite;
  private static Logger logger;
  @Expose
  private Map<TestCaseChromosome, Map<CUTChromosome, Set<Integer>>> linesCovered = new HashMap<>();
  @Expose
  private Map<TestCaseChromosome, Map<CUTChromosome, Set<Integer>>> branchesCovered = new HashMap<>();

  static {
    logger = LogManager.getLogger(ScytheInstrumenter.class);

    InstrumentationProperties.INSTRUMENT_BRANCHES = false;
    ClassReplacementTransformer.addShouldInstrumentChecker(new ShouldInstrumentChecker() {

      private boolean isTestClass(String className) {
        try {
          if (className.contains("Test")) {
            return true;
          }
          Class<?> cl = ClassLoader.getSystemClassLoader()
              .loadClass(className.replaceAll("/", "."));
          List<Method> methods = Arrays.asList(cl.getDeclaredMethods());
          if (cl.isMemberClass() && isTestClass(cl.getEnclosingClass().getName())) {
            return true;
          }
          if (methods.stream().anyMatch(method -> method.getName().startsWith("test"))) {
            return true;
          }
          if (methods.stream()
              .anyMatch(method -> Arrays.asList(method.getAnnotations()).contains(Test.class))) {
            return true;
          }
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
  public void setTestSuite(TestSuiteChromosome ts) {
    this.testSuite = ts;
  }

  @Override
  public void collectCoverage() {
    if (SCYTHE_READ) {
      File scytheCoverage = new File(SCYTHE_OUTPUT_DIR + File.separator + SCYTHE_FILENAME);
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
        PrintStream defaultSysOut = System.out;
        PrintStream defaultSysErr = System.err;
        // ensure coverage data is collected
        System.setOut(NullPrintStream.instance);
        System.setErr(NullPrintStream.instance);
        ProgressBar bar = new ProgressBar(defaultSysOut);
        bar.setTitle("Running Test Cases");
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
        System.setOut(defaultSysOut);
        System.setErr(defaultSysErr);
      } catch (final Exception e) {
        // runtime startup exception
        reportException(e);
      }
      if (!SCYTHE_READ && SCYTHE_WRITE) {
        File scytheCoverage = new File(SCYTHE_OUTPUT_DIR + File.separator + SCYTHE_FILENAME);
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

  private Map<CUTChromosome, Set<Integer>> collectLines(TestCaseChromosome testCase) {
    Map<CUTChromosome, Set<Integer>> covered = new HashMap<>();
    List<Class<?>> changedClasses = ClassAnalyzer.getChangedClasses();
    for (Class<?> cl : changedClasses) {
      if (CUTChromosomeStore.get(cl.getName()) != null) {
        List<Line> lines = ClassAnalyzer.getCoverableLines(cl.getName());
        Set<Integer> linesCovered = lines.stream().filter(line -> line.getHits() > 0)
            .map(line -> line.getGoalId()).collect(Collectors.toSet());
        covered.put(CUTChromosomeStore.get(cl.getName()), linesCovered);
      }
    }
    return covered;
  }

  private Map<CUTChromosome, Set<Integer>> collectBranches(TestCaseChromosome testCase) {
    Map<CUTChromosome, Set<Integer>> covered = new HashMap<>();
    List<Class<?>> changedClasses = ClassAnalyzer.getChangedClasses();
    for (Class<?> cl : changedClasses) {
      if (CUTChromosomeStore.get(cl.getName()) != null) {
        List<Branch> branches = ClassAnalyzer.getCoverableBranches(cl.getName());
        Set<Integer> branchesCovered = branches.stream().filter(branch -> branch.getHits() > 0)
            .map(branch -> branch.getGoalId()).collect(Collectors.toSet());
        covered.put(CUTChromosomeStore.get(cl.getName()), branchesCovered);
      }
    }
    return covered;
  }

  @Override
  public Map<CUTChromosome, Set<Integer>> getLinesCovered(TestCaseChromosome testCase) {
    return linesCovered.get(testCase);
  }

  @Override
  public Map<CUTChromosome, Set<Integer>> getBranchesCovered(TestCaseChromosome testCase) {
    return branchesCovered.get(testCase);
  }

  @Override
  public int getTotalLines(CUTChromosome cut) {
    return ClassAnalyzer.getCoverableLines(cut.getCUT().getName()).size();
  }

  @Override
  public int getTotalBranches(CUTChromosome cut) {
    return ClassAnalyzer.getCoverableBranches(cut.getCUT().getName()).size();
  }

  @Override
  public Set<Integer> getLines(CUTChromosome cut) {
    return ClassAnalyzer.getCoverableLines(cut.getCUT().getName()).stream()
        .map(line -> line.getLineNumber()).collect(Collectors.toSet());
  }

  @Override
  public Set<Integer> getBranches(CUTChromosome cut) {
    return ClassAnalyzer.getCoverableBranches(cut.getCUT().getName()).stream()
        .map(line -> line.getLineNumber()).collect(Collectors.toSet());
  }

  @Override
  public int getTotalLines(SUTChromosome sut) {
    return sut.getClassesUnderTest().stream()
        .mapToInt(cut -> ClassAnalyzer.getCoverableLines(cut.getCUT().getName()).size()).sum();
  }

  @Override
  public int getLinesCovered(TestSuiteChromosome testSuite) {
    Map<CUTChromosome, Set<Integer>> covered = new HashMap<>();
    testSuite.getTestCases().stream().forEach(testCase -> {
      Map<CUTChromosome, Set<Integer>> linesCovered = getLinesCovered(testCase);
      linesCovered.entrySet().forEach(entry -> {
        if (covered.containsKey(entry.getKey())) {
          covered.get(entry.getKey()).addAll(entry.getValue());
        } else {
          covered.put(entry.getKey(), entry.getValue());
        }
      });
    });
    return covered.values().stream().mapToInt(Set::size).sum();
  }

  @Override
  public int getTotalBranches(SUTChromosome sut) {
    return sut.getClassesUnderTest().stream()
        .mapToInt(cut -> ClassAnalyzer.getCoverableBranches(cut.getCUT().getName()).size())
        .sum();
  }

  @Override
  public int getBranchesCovered(TestSuiteChromosome testSuite) {
    return testSuite.getTestCases().stream().mapToInt(
        testCase -> getBranchesCovered(testCase).entrySet().stream()
            .mapToInt(entry -> entry.getValue().size()).sum()).sum();
  }

  private String coveredString
      (Map<TestCaseChromosome, Map<CUTChromosome, Set<Integer>>> covered) {
    StringBuilder sb = new StringBuilder();
    Iterator<TestCaseChromosome> tests = covered.keySet().iterator();
    while (tests.hasNext()) {
      TestCaseChromosome tc = tests.next();
      sb.append(tc.getId());
      sb.append(":{");
      Iterator<CUTChromosome> cuts = covered.get(tc).keySet().iterator();
      while (cuts.hasNext()) {
        CUTChromosome cut = cuts.next();
        sb.append(cut.getId());
        sb.append(":[");
        Set<Integer> lines = covered.get(tc).get(cut);
        Iterator<Integer> it = lines.iterator();
        while (it.hasNext()) {
          sb.append(it.next());
          if (it.hasNext()) {
            sb.append(",");
          }
        }
        sb.append("]");
        if (cuts.hasNext()) {
          sb.append(",");
        }
      }
      sb.append("}");
      if (tests.hasNext()) {
        sb.append(",");
      }
    }
    return sb.toString();
  }

  private class ScytheTypeWriter extends TypeAdapter<ScytheInstrumenter> {

    @Override
    public void write(JsonWriter out, ScytheInstrumenter inst)
        throws IOException {
      out.beginObject();
      out.name("linesCovered");

      out.beginObject();
      Iterator<TestCaseChromosome> testCases = inst.linesCovered.keySet().iterator();
      while (testCases.hasNext()) {
        TestCaseChromosome tc = testCases.next();
        out.name(Integer.toString(tc.getId()));
        out.beginObject();
        Iterator<CUTChromosome> cuts = inst.linesCovered.get(tc).keySet().iterator();
        while (cuts.hasNext()) {
          CUTChromosome cut = cuts.next();
          out.name(Integer.toString(cut.getId()));
          out.beginArray();
          Set<Integer> lines = inst.linesCovered.get(tc).get(cut);
          for (Integer line : lines) {
            out.value(line);
          }
          out.endArray();
        }
        out.endObject();
      }
//      out.name("branchesCovered").value(inst.coveredString(inst.branchesCovered));
      out.endObject();
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

    private Map<TestCaseChromosome, Map<CUTChromosome, Set<Integer>>> readCoverage(
        JsonReader in) throws IOException {
      HashMap<TestCaseChromosome, Map<CUTChromosome, Set<Integer>>> returnMap = new HashMap<>();
      // test cases
      in.beginObject();
      while (in.hasNext()) {
        int testCaseId = Integer.parseInt(in.nextName());
        TestCaseChromosome tc = TestCaseChromosomeStore.get(testCaseId);
        Map<CUTChromosome, Set<Integer>> linesCovered = new HashMap<>();
        in.beginObject();
        // cuts
        while (in.hasNext()) {
          int cutId = Integer.parseInt(in.nextName());
          CUTChromosome cut = CUTChromosomeStore.get(cutId);
          Set<Integer> linesCoveredInCut = new HashSet<>();
          in.beginArray();
          // lines in CUT
          while (in.hasNext()) {
            linesCoveredInCut.add(in.nextInt());
          }
          in.endArray();
          linesCovered.put(cut, linesCoveredInCut);
        }
        in.endObject();
        returnMap.put(tc, linesCovered);
      }
      in.endObject();
      return returnMap;
    }
  }

}