package org.kanonizo;

import com.scythe.instrumenter.analysis.ClassAnalyzer;
import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.runners.Parameterized.Parameters;
import org.kanonizo.algorithms.MutationSearchAlgorithm;
import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.algorithms.metaheuristics.fitness.APBCFunction;
import org.kanonizo.algorithms.metaheuristics.fitness.APFDFunction;
import org.kanonizo.algorithms.metaheuristics.fitness.APLCFunction;
import org.kanonizo.algorithms.metaheuristics.fitness.FitnessFunction;
import org.kanonizo.algorithms.metaheuristics.fitness.InstrumentedFitnessFunction;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.objects.ParameterisedTestCase;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.instrumenters.NullInstrumenter;
import org.kanonizo.junit.TestingUtils;
import org.kanonizo.reporting.CoverageWriter;
import org.kanonizo.reporting.CsvWriter;
import org.kanonizo.reporting.MiscStatsWriter;
import org.kanonizo.reporting.TestCaseOrderingWriter;
import org.kanonizo.util.Util;
import org.reflections.Reflections;

public class Framework {
  private File sourceFolder;
  private File testFolder;
  private List<File> libFolders = new ArrayList<>();
  private List<CsvWriter> writers = new ArrayList<>();
  private static final Logger logger = LogManager.getLogger(Framework.class);
  private SystemUnderTest sut;
  private SearchAlgorithm algorithm;
  private static Instrumenter inst = new NullInstrumenter();

  public static Instrumenter getInstrumenter() {
    return inst;
  }

  public static void setInstrumenter(Instrumenter inst) {
    Framework.inst = inst;
  }


  public Framework() {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

      @Override
      public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace(ClassAnalyzer.out);
      }

    });
  }

  public void setAlgorithm(SearchAlgorithm algorithm) {
    this.algorithm = algorithm;
  }

  public void addWriter(CsvWriter writer) {
    writers.add(writer);
  }

  public void setSourceFolder(File sourceFolder) {
    Util.addToClassPath(sourceFolder);
    this.sourceFolder = sourceFolder;
  }

  public void setTestFolder(File testFolder) {
    Util.addToClassPath(testFolder);
    this.testFolder = testFolder;
  }

  List<File> getLibFolders() {
    return libFolders;
  }

  /**
   * Add a library folder containing jar files to the framework. This is particularly important for java applications that aren't built on Maven. Maven applications will automatically add their
   * dependencies through using maven as a system tool
   *
   * @param libFolder - a library folder containing JAR files that are required for the test cases to execute properly
   */
  public void addLibFolder(File libFolder) {
    libFolders.add(libFolder);
    Arrays.asList(libFolder.listFiles(file -> file.getName().endsWith(".jar"))).forEach(jar -> Util.addToClassPath(jar));
  }


  protected List<File> findClasses(File folder) throws ClassNotFoundException {
    List<File> classes = new ArrayList<>();
    if (folder.isDirectory()) {
      File[] files = folder.listFiles(file -> file.getName().endsWith(".class") || file.isDirectory());
      for (File file : files) {
        if (file.isDirectory()) {
          classes.addAll(findClasses(file));
        } else {
          classes.add(file);
        }
      }
    } else if (folder.isFile()) {
      classes.add(folder);
    }
    return classes;
  }


  public void loadClasses() throws ClassNotFoundException {
    sut = new SystemUnderTest();
    List<File> sourceFiles = findClasses(sourceFolder);
    for (File file : sourceFiles) {
      Class<?> cl = loadClassFromFile(file);
      sut.addClass(new ClassUnderTest(cl));
      logger.info("Added class " + cl.getName());
    }
    List<File> testFiles = findClasses(testFolder);
    for (File file : testFiles) {
      Class<?> cl = loadClassFromFile(file);
      if (Util.isTestClass(cl)) {
        List<Method> testMethods = TestingUtils.getTestMethods(cl);
        logger.info("Adding " + testMethods.size() + " test methods from " + cl.getName());
        for (Method m : testMethods) {
          if (TestingUtils.isParameterizedTest(cl, m)) {
            Optional<Method> parameterMethod = Arrays.asList(cl.getMethods()).stream().filter(method -> method.getAnnotation(Parameters.class) != null).findFirst();
            if (parameterMethod.isPresent()) {
              try {
                Iterable<Object[]> parameters = (Iterable<Object[]>) parameterMethod.get().invoke(null,new Object[]{});
                for(Object[] inst : parameters){
                  ParameterisedTestCase ptc = new ParameterisedTestCase(cl, m, inst);
                  sut.addTestCase(ptc);
                }
              } catch (IllegalAccessException e) {
                logger.error(e);
              } catch (InvocationTargetException e) {
                logger.error(e);
              }
            } else {
              logger.error("Trying to create parameterized test case that has no parameter method");
            }
          } else {
            TestCase t = new TestCase(cl, m);
            sut.addTestCase(t);
          }

        }
      } else {
        sut.addExtraClass(cl);
        logger.info("Adding supporting test class " + cl.getName());
      }
    }
    logger.info("Finished adding source and test files. Total " + sut.getClassesUnderTest().size() + " classes and " + sut.getTestSuite().size() + " test cases");
  }

  private Class<?> loadClassFromFile(File file) {
    Class<?> cl = null;
    try {
      ClassParser parser = new ClassParser(file.getAbsolutePath());
      JavaClass jcl = parser.parse();
      cl = getInstrumenter().loadClass(jcl.getClassName());

    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      logger.error(e);
    } catch (IOException e) {
      logger.error(e);
    }
    return cl;
  }

  /**
   * Create the instance of the fitness function that will be used to guide a metaheuristic search. It is assumed that the Fitness Function has been set by the main method and is contained in the
   * {@link Properties#FITNESS_FUNC} object. Default case is an {@link APFDFunction}, and other options include {@link APLCFunction} at present. More will be added soon
   */
  protected void setupFitnessFunction() {
    FitnessFunction<SystemUnderTest> func;
    switch (Properties.COVERAGE_APPROACH) {
      case LINE:
        func = new APLCFunction(sut);
        break;
      case BRANCH:
        func = new APBCFunction(sut);
        break;
      default:
        func = new APLCFunction(sut);
    }
    if (algorithm instanceof MutationSearchAlgorithm) {
      func = ((MutationSearchAlgorithm) algorithm).getFitnessFunction();
    }
    sut.getTestSuite().setFitnessFunction(func);
    Properties.INSTRUMENT = func instanceof InstrumentedFitnessFunction;
  }

  /**
   * Method that can decide how to display results once the algorithm has finished executing.
   *
   * @param algorithm - the Search Algorithm that was used to find a solution
   */
  protected void reportResults(SearchAlgorithm algorithm) {
    StringBuilder sb = new StringBuilder();
    if (algorithm != null) {
      sb.append("The solution found by the ");
      String algorithmName = algorithm.getClass().getName();
      algorithmName = algorithmName.substring(algorithmName.lastIndexOf(".") + 1);
      String[] algorithmSplit = algorithmName.split("(?=\\p{Upper})");
      for (String name : algorithmSplit) {
        sb.append(name + " ");
      }
      sb.append("for the problem ");
      sb.append("is:\n");
      sb.append(algorithm.getCurrentOptimal());

    }
    logger.info(sb.toString());
    for (CsvWriter writer : writers) {
      writer.write();
    }
    logger.info("Results complete");
  }

  public void run() throws ClassNotFoundException {
    loadClasses();
    algorithm.setSearchProblem(sut);
    if (algorithm.needsFitnessFunction()) {
      setupFitnessFunction();
    }
    if (Properties.PRIORITISE) {
      algorithm.start();
    }
    addWriter(new CoverageWriter(sut));
    addWriter(new TestCaseOrderingWriter(algorithm));
    addWriter(new MiscStatsWriter(algorithm));
    reportResults(algorithm);

  }

  public static List<SearchAlgorithm> getAvailableAlgorithms() throws InstantiationException, IllegalAccessException{
    Reflections r = Util.getReflections();
    Set<Class<?>> algorithms = r.getTypesAnnotatedWith(Algorithm.class);
    List<SearchAlgorithm> algorithmsInst = algorithms.stream().map(cl -> {
      try {
        return (SearchAlgorithm) cl.newInstance();
      } catch (InstantiationException e) {
      } catch (IllegalAccessException e) {
      }
      throw new RuntimeException("Could not instantiate one of more search algorithms");
    }).collect(Collectors.toList());
    return algorithmsInst;
  }
}
