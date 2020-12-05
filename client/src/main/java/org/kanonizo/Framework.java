package org.kanonizo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.runners.Parameterized.Parameters;
import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.annotations.Prerequisite;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.configuration.configurableoption.BooleanOption;
import org.kanonizo.configuration.configurableoption.FileOption;
import org.kanonizo.configuration.configurableoption.StringOption;
import org.kanonizo.display.Display;
import org.kanonizo.framework.TestCaseStore;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.objects.ParameterisedTestCase;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.junit.TestingUtils;
import org.kanonizo.junit.testidentification.JUnit3TestCaseIdentificationStrategy;
import org.kanonizo.junit.testidentification.JUnit3TestSuiteIdentificationStrategy;
import org.kanonizo.junit.testidentification.JUnit4ParameterisedTestIdentificationStrategy;
import org.kanonizo.junit.testidentification.JUnit4TestIdentificationStrategy;
import org.kanonizo.junit.testidentification.JUnit5TestIdentificationStrategy;
import org.kanonizo.junit.testidentification.TestIdentificationStrategy;
import org.kanonizo.listeners.TestCaseSelectionListener;
import org.kanonizo.reporting.CoverageWriter;
import org.kanonizo.reporting.CsvWriter;
import org.kanonizo.reporting.MiscStatsWriter;
import org.kanonizo.reporting.TestCaseOrderingWriter;
import org.kanonizo.typeadapters.AlgorithmAdapter;
import org.kanonizo.typeadapters.FileTypeAdapter;
import org.kanonizo.typeadapters.InstrumenterAdapter;
import org.kanonizo.util.RecursiveFileFinder;
import org.kanonizo.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.empty;
import static org.junit.runner.Description.createTestDescription;
import static org.kanonizo.configuration.configurableoption.BooleanOption.booleanOption;
import static org.kanonizo.configuration.configurableoption.FileOption.fileOption;
import static org.kanonizo.configuration.configurableoption.StringOption.stringOption;
import static org.kanonizo.junit.TestingUtils.getTestMethods;

public class Framework implements Serializable
{
    private static final StringOption FORBIDDEN_CLASSNAMES_OPTION = stringOption("forbidden_classnames", "");
    private static final BooleanOption USE_SUITE_METHODS_OPTION = booleanOption("use_suite_methods", false);
    private static final FileOption LOG_DIR_OPTION = fileOption("log_dir");
    private static final StringOption LOG_FILENAME_OPTION = stringOption("log_filename", "");

    private static final Function<KanonizoConfigurationModel, List<TestIdentificationStrategy>> TEST_IDENTIFICATION_STRATEGIES = configModel ->
            asList(
                    new JUnit5TestIdentificationStrategy(),
                    new JUnit4ParameterisedTestIdentificationStrategy(),
                    new JUnit4TestIdentificationStrategy(configModel),
                    new JUnit3TestSuiteIdentificationStrategy(),
                    new JUnit3TestCaseIdentificationStrategy()
            );

    private static final Logger logger = LogManager.getLogger(Framework.class);

    @Expose
    private final File sourceFolder;
    @Expose
    private final File testFolder;
    @Expose
    private final List<File> libraries;
    private final List<CsvWriter> writers;
    private final SystemUnderTest sut;
    @Expose
    private final SearchAlgorithm algorithm;
    @Expose
    private final Instrumenter instrumenter;
    private final Display display;
    @Expose
    private final File rootFolder;
    private final KanonizoConfigurationModel configModel;
    private final List<File> sourceFiles;
    private final List<File> testFiles;
    private final List<String> forbiddenClasses;
    private final boolean useSuiteMethods;
    private final Path logFileDirectory;
    private final String logFileNamePattern;

    private final List<TestCaseSelectionListener> listeners = new ArrayList<>();
    private final List<TestIdentificationStrategy> testCaseIdentificationStrategies;

    public Framework(
            KanonizoConfigurationModel configModel,
            File sourceFolder,
            File testFolder,
            File rootFolder,
            List<File> libraries,
            List<CsvWriter> writers,
            SystemUnderTest sut,
            SearchAlgorithm algorithm,
            Instrumenter instrumenter,
            Display display
    )
    {
        this.configModel = configModel;
        this.sourceFolder = sourceFolder;
        this.testFolder = testFolder;
        this.libraries = libraries;
        this.writers = writers;
        this.sut = sut;
        this.algorithm = algorithm;
        this.instrumenter = instrumenter;
        this.display = display;
        this.rootFolder = rootFolder != null ? rootFolder : new File(System.getProperty("user.home"));
        this.sourceFiles = new RecursiveFileFinder(sourceFolder, ".class").getAllFiles();
        this.testFiles = new RecursiveFileFinder(testFolder, ".class").getAllFiles();
        this.forbiddenClasses = Arrays.stream(
                configModel.getConfigurableOptionValue(FORBIDDEN_CLASSNAMES_OPTION).split(","))
                .filter(StringUtils::isNotEmpty)
                .collect(toList());
        this.useSuiteMethods = configModel.getConfigurableOptionValue(USE_SUITE_METHODS_OPTION);
        this.logFileDirectory = Paths.get(configModel.getFileOption(LOG_DIR_OPTION).toURI());
        this.logFileNamePattern = configModel.getStringOption(LOG_FILENAME_OPTION);
        this.testCaseIdentificationStrategies = TEST_IDENTIFICATION_STRATEGIES.apply(configModel);
    }

    public SearchAlgorithm getAlgorithm()
    {
        return algorithm;
    }

    public Instrumenter getInstrumenter()
    {
        return instrumenter;
    }

    public void addWriter(CsvWriter writer)
    {
        writers.add(writer);
    }

    public File getRootFolder()
    {
        return rootFolder;
    }

    public List<File> getLibraries()
    {
        return libraries;
    }


    public void loadClasses()
    {
        Premain.instrument = true;
        List<ClassUnderTest> classesUnderTest = sourceFiles
                .stream()
                .map(this::loadClassFromFile)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ClassUnderTest::new)
                .collect(toList());
        Premain.instrument = false;
        List<TestSuite> testSuites = new LinkedList<>();
        List<TestCase> testCases = testFiles
                .stream()
                .map(this::loadClassFromFile)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(this::findTestCasesInFile)
                .collect(toList());
        for (File file : testFiles)
        {
            Optional<Class<?>> optionalTestClass = loadClassFromFile(file);
            if (optionalTestClass.isPresent())
            {
                Class<?> testClass = optionalTestClass.get();
                if (Util.isTestClass(testClass))
                {
                    Optional<TestSuite> junit3TestSuite = TestingUtils.getTestSuite(testClass);
                    if (junit3TestSuite.isPresent() && TestingUtils.isSuiteContainer(junit3TestSuite.get()))
                    {
                        logger.info("Found test suite in class " + testClass.getSimpleName());
                        testSuites.add(junit3TestSuite.get());
                    }
                    List<Method> testMethods = getTestMethods(testClass);
                    logger.info("Adding " + testMethods.size() + " test methods from " + testClass.getName());
                    for (Method m : testMethods)
                    {
                        if (TestingUtils.isParameterizedTest(testClass, m))
                        {
                            Optional<Method> parameterMethod = Arrays.stream(testClass.getMethods())
                                    .filter(method -> method.getAnnotation(Parameters.class) != null).findFirst();
                            if (parameterMethod.isPresent())
                            {
                                try
                                {
                                    Iterable<Object[]> parameters = (Iterable<Object[]>) parameterMethod.get()
                                            .invoke(null, new Object[]{});
                                    for (Object[] inst : parameters)
                                    {
                                        ParameterisedTestCase ptc = new ParameterisedTestCase(testClass, m, inst);
                                        sut.suite.addTestCase(ptc);
                                    }
                                }
                                catch (IllegalAccessException | InvocationTargetException e)
                                {
                                    logger.error(e);
                                }
                            }
                            else
                            {
                                logger
                                        .error(
                                                "Trying to create parameterized test case that has no parameter method");
                            }
                        }
                        else
                        {
                            TestCase t = new TestCase(testClass, m, configModel);
                            sut.suite.addTestCase(t);
                        }
                    }
                }
                else
                {
                    sut.addExtraClass(testClass);
                    logger.info("Adding supporting test class " + testClass.getName());
                }
            }
        }
        if (!testSuites.isEmpty())
        {
            sut.getTestSuite().clear();
            testSuites.forEach(testSuite -> sut.getTestSuite().addAll(collectTestCases(testSuite)));
            for (TestSuite ts : testSuites)
            {
                List<TestCase> testCasesInSuite = collectTestCases(ts);
                for (TestCase tc : testCasesInSuite)
                {
                    sut.getTestSuite().addTestCase(tc);
                }
            }
        }
        ClassUnderTest.resetCount();
        logger.info("Finished adding source and test files. Total " + sut.getClassesUnderTest().size()
                            + " classes and " + sut.getTestSuite().size() + " test cases");
    }

    private Stream<TestCase> findTestCasesInFile(Class<?> testClass)
    {
        for (TestIdentificationStrategy testIdentificationStrategy : testCaseIdentificationStrategies)
        {
            if (testIdentificationStrategy.handles(testClass))
            {
                return testIdentificationStrategy.testCasesFrom(testClass).stream();
            }
        }
        return empty();
    }

    private List<TestCase> collectTestCases(TestSuite suite)
    {
        List<TestCase> retTests = new ArrayList<>();
        Enumeration<Test> tests = suite.tests();
        if (TestingUtils.isSuiteContainer(suite))
        {
            while (tests.hasMoreElements())
            {
                junit.framework.Test next = tests.nextElement();
                retTests.addAll(collectTestCases((TestSuite) next));
            }
        }
        else
        {
            logger.info("Adding " + suite.testCount() + " test cases from " + suite.getName());
            while (tests.hasMoreElements())
            {
                junit.framework.Test next = tests.nextElement();
                if (next instanceof junit.framework.TestCase)
                {
                    junit.framework.TestCase nextCase = (junit.framework.TestCase) next;
                    Class<? extends Test> testClass = next.getClass();
                    try
                    {
                        Method m = testClass.getMethod(nextCase.getName());
                        String desc = createTestDescription(testClass, m.getName()).toString();
                        TestCase tc = TestCaseStore.with(desc);
                        // some test cases never get added to the test case store because kanonizo doesn't recognise them
                        // as test cases. To prevent NPEs later on we only at test cases that are registered in the store
                        if (tc != null)
                        {
                            retTests.add(tc);
                        }
                    }
                    catch (NoSuchMethodException e)
                    {

                    }

                }
            }
        }
        return retTests;
    }

    private Optional<Class<?>> loadClassFromFile(File file)
    {
        try
        {
            ClassParser parser = new ClassParser(file.getAbsolutePath());
            JavaClass javaClass = parser.parse();
            if (forbiddenClasses.size() > 0 && forbiddenClasses.stream().anyMatch(
                    f -> javaClass.getClassName().substring(javaClass.getPackageName().length() + 1).startsWith(f)))
            {
                logger.info("Ignoring class " + javaClass.getClassName() + " because it is forbidden");
                return Optional.empty();
            }
            else
            {
                return Optional.of(instrumenter.loadClass(javaClass.getClassName()));
            }

        }
        catch (ClassNotFoundException | NoClassDefFoundError | IOException | ExceptionInInitializerError e)
        {
            logger.error(e);
        }
        return Optional.empty();
    }

    private void reportResults(String algorithmName, org.kanonizo.framework.objects.TestSuite solution)
    {
        logger.info(
                format(
                        "The solution found by the %s for the problem is:\n %s",
                        algorithmName,
                        solution
                )
        );
        for (CsvWriter writer : writers)
        {
            writer.write();
        }
        logger.info("Results complete");
    }

    public org.kanonizo.framework.objects.TestSuite run()
    {
        loadClasses();

        if (Properties.PRIORITISE)
        {
            algorithm.setSearchProblem(sut);
        }

        // collect (or read) coverage
        instrumenter.collectCoverage(sut.getTestSuite());

        TestCaseOrderingWriter writer = new TestCaseOrderingWriter(
                logFileDirectory,
                logFileNamePattern,
                algorithm,
                instrumenter
        );
        addSelectionListener(writer);
        addWriter(writer);
        addWriter(new CoverageWriter(logFileDirectory, logFileNamePattern, sut, instrumenter));
        addWriter(new MiscStatsWriter(logFileDirectory, logFileNamePattern, algorithm));
        if (Properties.PRIORITISE)
        {
            org.kanonizo.framework.objects.TestSuite solution = algorithm.run();
            reportResults(algorithm.readableName(), solution);
            return solution;
        }
        return sut.getTestSuite();
    }

    public static <T extends SearchAlgorithm> List<Prerequisite<T>> getPrerequisites(T searchAlgorithm)
    {
        List<Method> requirements = Arrays.stream(searchAlgorithm.getClass().getMethods())
                .filter(m -> m.isAnnotationPresent(Prerequisite.class))
                .filter(method -> isStatic(method.getModifiers()))
                .filter(method -> method.getReturnType() == Boolean.class || method.getReturnType() == boolean.class)
                .collect(toList());
        requirements.forEach(method ->
                             {
                                 if (!method.canAccess(null))
                                 {
                                     method.setAccessible(true);
                                 }
                             });
        return requirements;
    }

    public void write(File out) throws IOException
    {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(File.class, new FileTypeAdapter())
                .registerTypeAdapter(SearchAlgorithm.class, new AlgorithmAdapter())
                .registerTypeAdapter(Instrumenter.class, new InstrumenterAdapter())
                .create();
        FileOutputStream w = new FileOutputStream(out);
        w.write(gson.toJson(this).getBytes());
        w.flush();
    }

    public Framework read(File in) throws FileNotFoundException
    {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(File.class, new FileTypeAdapter())
                .registerTypeAdapter(SearchAlgorithm.class, new AlgorithmAdapter())
                .registerTypeAdapter(Instrumenter.class, new InstrumenterAdapter())
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        Framework fw = gson.fromJson(new FileReader(in), Framework.class);
        return fw;
    }

    public Display getDisplay()
    {
        return display;
    }

    public File getSourceFolder()
    {
        return sourceFolder;
    }

    public File getTestFolder()
    {
        return testFolder;
    }

    public void addSelectionListener(TestCaseSelectionListener list)
    {
        listeners.add(list);
    }

    public void notifyTestCaseSelection(TestCase tc)
    {
        listeners.forEach(listener -> listener.testCaseSelected(tc));
    }

}
