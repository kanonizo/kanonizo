package com.dpaterson.testing;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.dpaterson.testing.algorithms.MutationSearchAlgorithm;
import com.dpaterson.testing.algorithms.SearchAlgorithm;
import com.dpaterson.testing.algorithms.metaheuristics.fitness.APBCFunction;
import com.dpaterson.testing.algorithms.metaheuristics.fitness.APFDFunction;
import com.dpaterson.testing.algorithms.metaheuristics.fitness.APLCFunction;
import com.dpaterson.testing.algorithms.metaheuristics.fitness.FitnessFunction;
import com.dpaterson.testing.algorithms.metaheuristics.fitness.InstrumentedFitnessFunction;
import com.dpaterson.testing.framework.CUTChromosome;
import com.dpaterson.testing.framework.Chromosome;
import com.dpaterson.testing.framework.SUTChromosome;
import com.dpaterson.testing.framework.TestCaseChromosome;
import com.dpaterson.testing.framework.TestSuiteChromosome;
import com.dpaterson.testing.framework.instrumentation.Instrumenter;
import com.dpaterson.testing.reporting.CoverageWriter;
import com.dpaterson.testing.reporting.CsvWriter;
import com.dpaterson.testing.reporting.MiscStatsWriter;
import com.dpaterson.testing.reporting.TestCaseOrderingWriter;
import com.dpaterson.testing.util.Util;
import com.sheffield.instrumenter.analysis.ClassAnalyzer;
import com.sheffield.instrumenter.instrumentation.InstrumentingClassLoader;

public class Framework {
    private File sourceFolder;
    private File testFolder;
    private List<File> libFolders = new ArrayList<>();
    private List<CsvWriter> writers = new ArrayList<>();
    private static final Logger logger = LogManager.getLogger(Framework.class);
    private SUTChromosome sut;
    private TestSuiteChromosome testSuite;
    private SearchAlgorithm algorithm;

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

    /**
     * Add a library folder containing jar files to the framework. This is particularly important for java applications that aren't built on Maven. Maven applications will automatically add their
     * dependencies through using maven as a system tool
     *
     * @param libFolder
     *            - a library folder containing JAR files that are required for the test cases to execute properly
     */
    public void addLibFolder(File libFolder) {
        libFolders.add(libFolder);
        Arrays.asList(libFolder.listFiles()).stream().filter(file -> file.getName().endsWith(".jar"))
                .forEach(jar -> Util.addToClassPath(jar));
    }

    /**
     * Recursive method to iterate through directories and subdirectories, ensuring all classes from the source folder are included
     *
     * @param classes
     *            - a list of {@link CUTChromosome} objects, which may be empty but not {@code null}
     * @param folder
     *            - either a directory or a single file to be added to the classes list
     */
    protected void collectClasses(List<CUTChromosome> classes, File folder) throws ClassNotFoundException {
        InstrumentingClassLoader.getInstance().setShouldInstrument(true);
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    collectClasses(classes, file);
                } else {
                    addClass(classes, file);
                }
            }
        } else if (folder.isFile()) {
            addClass(classes, folder);
        }
    }

    /**
     * Recursive method to iterate through directories and subdirectories, ensuring all test cases from the test folder are included
     *
     * @param classes
     *            - a list of {@link CUTChromosome} objects, which may be empty but not {@code null}
     * @param extraClasses
     *            - a list of Classes that do not contain any test cases but are still required for instrumentation. See {@link #addTestClass(List, List, File)} for a detailed desription of why this
     *            is necessary
     * @param folder
     *            - either a directory or a single file to be added to the classes list
     * @see #addTestClass(List, List, File)
     */
    protected void collectTestClasses(List<TestCaseChromosome> classes, List<Class<?>> extraClasses, File folder) {
        InstrumentingClassLoader.getInstance().setShouldInstrument(false);
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    collectTestClasses(classes, extraClasses, file);
                } else {
                    addTestClass(classes, extraClasses, file);
                }
            }
        } else if (folder.isFile()) {
            addTestClass(classes, extraClasses, folder);
        }
    }

    /**
     * Add a Test to the TestSuite. This method is used for adding Test Cases, and covers the case of anonymous inner classes within tests as well. It is important that we remember to include extra
     * classes so that any attempts to access them during JUnit execution don't result in classloader problems. E.G
     *
     * <pre>
     * <code>
     * import some.package.X;
     * public class TestXYZ{
     *   {@literal @}Test
     *   public void testX(){
     *     X x = new {@code TestX()};
     *     assertNotNull("x must not be null", x);
     *   }
     *
     *   private static final class TestX extends X{
     *     private TestX(){
     *       super();
     *     }
     *   }
     * }
     * </code>
     * </pre>
     *
     * When instrumented, the {@link ClassLoader} for the class TestXYZ will be changed to return the instrumented version including probes for detecting line execution.<br/>
     * The static inner class TestX contains no test cases but is still instantiated from the testing class, meaning when we come to execute JUnit, if TestX is using a different class loader from
     * either its superclass X or even from TestXYZ, there will be an error while running JUnit.
     *
     * @param classes
     *            - a list of Test Case Chromosomes (which may be empty but not null) in which to collect source classes
     * @param extraClasses
     *            - a list of Classes which do not contain any test cases but still require instrumentation in order for correct JUnit execution. May be {@link List#isEmpty()} but never {@code null}
     * @param file
     *            - the current file for inspection
     */
    protected void addTestClass(List<TestCaseChromosome> classes, List<Class<?>> extraClasses, File file) {
        if (!file.getName().endsWith(".class")) {
            return;
        }
        PrintStream sysOut = System.out;
        System.setOut(Instrumenter.getNullOut());
        Class<?> cl = null;
        try {
            ClassParser parser = new ClassParser(file.getAbsolutePath());
            JavaClass jcl = parser.parse();
            cl = InstrumentingClassLoader.getInstance().loadClass(jcl.getClassName());

        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        }
        final Class<?> finalCl = cl;
        Method[] methods = finalCl.getMethods();
        // if any methods declare the @Test annotation, then they are test
        // methods, hence it is assumed that they are part of a test class.
        // if a method starts with test but has no @Test annotation then it
        // is a junit 3 style test
        List<Method> testMethods = Arrays.asList(methods).stream()
                .filter(method -> method.getAnnotation(Test.class) != null || method.getName().startsWith("test"))
                .collect(Collectors.toList());
        if (testMethods.size() > 0) {
            testMethods.stream().forEach(method -> {
                TestCaseChromosome tc = new TestCaseChromosome();
                tc.setMethod(method);
                tc.setTestClass(finalCl);
                classes.add(tc);
            });
        } else {
            extraClasses.add(finalCl);
        }
        System.setOut(sysOut);
        logger.info("Added class " + Util.getName(cl));
    }

    /**
     * Add a Class to the System Under Test. This method is used for adding source classes. The class must be visible to the ClassLoader
     *
     * @param classes
     *            - a list of Class Under Test Chromosomes (which may be empty but not null) in which to collect source classes
     * @param file
     *            - the current file for inspection
     */
    protected void addClass(List<CUTChromosome> classes, File file) throws ClassNotFoundException {
        if (!file.getName().endsWith(".class")) {
            return;
        }
        PrintStream sysOut = System.out;
        System.setOut(Instrumenter.getNullOut());
        Class<?> cl = null;
        try {
            ClassParser parser = new ClassParser(file.getAbsolutePath());
            JavaClass jcl = parser.parse();
            cl = InstrumentingClassLoader.getInstance().loadClass(jcl.getClassName());
        } catch (ClassNotFoundException | NoClassDefFoundError | IOException e) {
            logger.error(e);
            logger.error("Error while loading: " + file.getAbsolutePath());
            throw new ClassNotFoundException(e.getMessage());
        }

        CUTChromosome cut = new CUTChromosome(cl);
        classes.add(cut);
        System.setOut(sysOut);
        logger.info("Added class " + cl.getName());
    }

    public void generateTSC() throws ClassNotFoundException {
        File root = sourceFolder.getParentFile().getParentFile();
        if (MavenAnalyser.isMavenProject(root) && !libFolders.isEmpty()) {
            MavenAnalyser.addMavenDependencies(root);
        }
        List<CUTChromosome> cuts = new ArrayList<CUTChromosome>();
        collectClasses(cuts, sourceFolder);
        List<TestCaseChromosome> testCases = new ArrayList<>();
        List<Class<?>> extraClasses = new ArrayList<>();
        collectTestClasses(testCases, extraClasses, testFolder);
        sut = new SUTChromosome(cuts, extraClasses);
        testSuite = new TestSuiteChromosome(sut, testCases);
    }

    /**
     * Create the instance of the fitness function that will be used to guide a metaheuristic search. It is assumed that the Fitness Function has been set by the main method and is contained in the
     * {@link Properties#FITNESS_FUNC} object. Default case is an {@link APFDFunction}, and other options include {@link DPFitnessFunction} at present. More will be added soon
     *
     * @param problem
     *            - the TestSuiteChromosome that requires a fitness function to calculate its fitness. The {@link TestSuiteChromosome#setFitnessFunction(FitnessFunction)} method <b>must</b> be called
     *            before any evolutions are done
     */
    protected void setupFitnessFunction() {
        FitnessFunction<TestSuiteChromosome> func;
        switch (Properties.COVERAGE_APPROACH) {
        case LINE:
            func = new APLCFunction(testSuite);
            break;
        case BRANCH:
            func = new APBCFunction(testSuite);
            break;
        default:
            func = new APLCFunction(testSuite);
        }
        if (algorithm instanceof MutationSearchAlgorithm) {
            func = ((MutationSearchAlgorithm) algorithm).getFitnessFunction();
        }
        testSuite.setFitnessFunction(func);
        Properties.INSTRUMENT = func instanceof InstrumentedFitnessFunction;
    }

    /**
     * Method that can decide how to display results once the algorithm has finished executing.
     *
     * @param algorithm
     *            - the Search Algorithm that was used to find a solution
     * @param problem
     *            - the problem that the search algorithm had to solve
     */
    protected void reportResults(SearchAlgorithm algorithm, Chromosome problem) {
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
            String problemName = problem.getClass().getName();
            problemName = problemName.substring(problemName.lastIndexOf('.') + 1);
            String[] problemSplit = problemName.split("(?=\\p{Upper})");
            for (String name : problemSplit) {
                sb.append(name + " ");
            }
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
        generateTSC();
        algorithm.setSearchProblem(testSuite);
        setupFitnessFunction();
        if (Properties.PRIORITISE) {
            logger.info("Finished parsing source folder. Found " + sut.getClassesUnderTest().size()
                    + " source classes and " + testSuite.size() + " test cases");
            algorithm.start();
        }
        addWriter(new CoverageWriter(testSuite, algorithm));
        addWriter(new TestCaseOrderingWriter(algorithm));
        addWriter(new MiscStatsWriter(algorithm));
        reportResults(algorithm, testSuite);

    }
}
