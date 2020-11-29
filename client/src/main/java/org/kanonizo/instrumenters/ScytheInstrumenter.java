package org.kanonizo.instrumenters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.scythe.instrumenter.analysis.ClassAnalyzer;
import com.scythe.instrumenter.instrumentation.ClassReplacementTransformer;
import com.scythe.instrumenter.instrumentation.InstrumentingClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.Framework;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.configuration.configurableoption.ConfigurableOption;
import org.kanonizo.display.Display;
import org.kanonizo.framework.ClassStore;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Branch;
import org.kanonizo.framework.objects.BranchStore;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.LineStore;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.typeadapters.ScytheTypeWriter;
import org.kanonizo.util.HashSetCollector;
import org.kanonizo.util.Util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.kanonizo.configuration.configurableoption.ConfigurableOption.configurableOptionFrom;
import static org.kanonizo.instrumenters.InstrumenterType.SCYTHE_INSTRUMENTER;

//
public class ScytheInstrumenter implements Instrumenter
{
    private static final ConfigurableOption<Boolean> WRITE_COVERAGE_FILE_OPTION = configurableOptionFrom(
            "scythe_write",
            Boolean.class,
            false
    );
    private static final ConfigurableOption<Boolean> READ_COVERAGE_FILE_OPTION = configurableOptionFrom(
            "scythe_read",
            Boolean.class,
            false
    );
    private static final ConfigurableOption<File> SCYTHE_FILE_OPTION = configurableOptionFrom(
            "scythe_filename",
            File.class,
            null,
            File::new
    );

    private TestSuite testSuite;
    private final boolean writeCoverageToFile;
    private final boolean readCoverageFromFile;
    private final File scytheOutputOrInputFile;
    private final Display display;
    private static Logger logger = LogManager.getLogger(ScytheInstrumenter.class);
    private Map<TestCase, Set<org.kanonizo.framework.objects.Line>> linesCovered = new HashMap<>();
    private Map<TestCase, Set<org.kanonizo.framework.objects.Branch>> branchesCovered = new HashMap<>();

    private static final String[] forbiddenPackages = new String[]{"org/kanonizo", "org/junit",
            "org/apache/commons/cli", "junit", "org/apache/bcel", "org/apache/logging/log4j",
            "org/objectweb/asm",
            "javax/swing", "javax/servlet", "org/xml", "org/hamcrest"};

    public ScytheInstrumenter(KanonizoConfigurationModel configurationModel, Display display, File sourceFolder)
    {
        this.writeCoverageToFile = configurationModel.getConfigurableOptionValue(WRITE_COVERAGE_FILE_OPTION);
        this.readCoverageFromFile = configurationModel.getConfigurableOptionValue(READ_COVERAGE_FILE_OPTION);
        this.scytheOutputOrInputFile = configurationModel.getConfigurableOptionValue(SCYTHE_FILE_OPTION);
        this.display = display;
        Arrays.stream(forbiddenPackages)
                .forEach(s -> ClassReplacementTransformer.addForbiddenPackage(s));
    }

    public ScytheInstrumenter(
            Map<TestCase, Set<Line>> linesCovered,
            Map<TestCase, Set<Branch>> branchesCovered,
            TestSuite testSuite
    )
    {
        this.writeCoverageToFile = false;
        this.readCoverageFromFile = false;
        this.scytheOutputOrInputFile = null;
        this.display = null;
        this.linesCovered = linesCovered;
        this.branchesCovered = branchesCovered;
        this.testSuite = testSuite;
    }

    private static void reportException(Exception e)
    {
        logger.error(e);
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException
    {
        return InstrumentingClassLoader.getInstance().loadClass(className);
    }

    @Override
    public void collectCoverage(TestSuite testSuite)
    {
        if (readCoverageFromFile)
        {
            if (validScytheCoverageFile())
            {
                Gson gson = getGson();
                try
                {
                    display.notifyTaskStart("Reading Coverage File", true);
                    ScytheInstrumenter instrumenter = gson
                            .fromJson(new FileReader(scytheOutputOrInputFile), ScytheInstrumenter.class);
                    // removing loading window
                    display.reportProgress(1, 1);
                    this.linesCovered = instrumenter.linesCovered;
                    this.branchesCovered = instrumenter.branchesCovered;
                    this.testSuite = instrumenter.testSuite;
                }
                catch (FileNotFoundException e)
                {
                    e.printStackTrace();
                }
            }
            else
            {
                throw new RuntimeException(
                        "Scythe Coverage file is missing. Ensure that -Dscythe_output_dir exists and -Dscythe_filename exists within that directory");
            }
        }
        else
        {
            try
            {
                Util.suppressOutput();
                display.notifyTaskStart("Running Test Cases", true);
                for (TestCase testCase : this.testSuite.getTestCases())
                {
                    try
                    {
                        testCase.run();
                        // debug code to find out where/why failures are occurring. Use
                        // breakpoints after execution to locate failures
                        ClassAnalyzer.collectHitCounters(true);
                        linesCovered.put(testCase, collectLines(testCase));
                        branchesCovered.put(testCase, collectBranches(testCase));
                        ClassAnalyzer.resetCoverage();
                        display.reportProgress(
                                (double) this.testSuite.getTestCases().indexOf(testCase) + 1,
                                this.testSuite.getTestCases().size()
                        );
                    }
                    catch (Throwable e)
                    {
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
            }
            catch (final Exception e)
            {
                // runtime startup exception
                reportException(e);
            }
            if (writeCoverageToFile)
            {
                if (scytheOutputOrInputFile != null && !scytheOutputOrInputFile.exists())
                {
                    try
                    {
                        scytheOutputOrInputFile.createNewFile();
                    }
                    catch (IOException e)
                    {
                        logger.debug("Failed to create coverage file");
                    }
                }
                Gson gson = getGson();
                String serialised = gson.toJson(this);
                try
                {
                    BufferedOutputStream out = new BufferedOutputStream(
                            new FileOutputStream(scytheOutputOrInputFile, false));
                    out.write(serialised.getBytes());
                    out.flush();
                }
                catch (FileNotFoundException e)
                {
                    logger.debug("Output file is missing!");
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    logger.debug("Failed to write output");
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean validScytheCoverageFile()
    {
        return scytheOutputOrInputFile != null && scytheOutputOrInputFile.exists();
    }

    private Gson getGson()
    {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(
                ScytheInstrumenter.class,
                new ScytheTypeWriter(linesCovered, branchesCovered, testSuite.getTestCases())
        );
        return builder.create();
    }

    private Set<org.kanonizo.framework.objects.Line> collectLines(TestCase testCase)
    {
        Set<org.kanonizo.framework.objects.Line> covered = new HashSet<>();
        List<Class<?>> changedClasses = ClassAnalyzer.getChangedClasses();
        for (Class<?> cl : changedClasses)
        {
            if (ClassStore.get(cl.getName()) != null)
            {
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

    private Set<org.kanonizo.framework.objects.Branch> collectBranches(TestCase testCase)
    {
        Set<org.kanonizo.framework.objects.Branch> covered = new HashSet<>();
        List<Class<?>> changedClasses = ClassAnalyzer.getChangedClasses();
        for (Class<?> cl : changedClasses)
        {
            if (ClassStore.get(cl.getName()) != null)
            {
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
    public Set<Line> getLinesCovered(TestCase testCase)
    {
        return linesCovered.containsKey(testCase) ? linesCovered.get(testCase) : new HashSet<>();
    }

    @Override
    public Set<Branch> getBranchesCovered(TestCase testCase)
    {
        return branchesCovered.containsKey(testCase) ? branchesCovered.get(testCase) : new HashSet<>();
    }

    @Override
    public int getTotalLines(ClassUnderTest cut)
    {
        return ClassAnalyzer.getCoverableLines(cut.getCUT().getName()).size();
    }

    @Override
    public int getTotalBranches(ClassUnderTest cut)
    {
        return ClassAnalyzer.getCoverableBranches(cut.getCUT().getName()).size();
    }

    @Override
    public Set<org.kanonizo.framework.objects.Line> getLines(ClassUnderTest cut)
    {
        return ClassAnalyzer.getCoverableLines(cut.getCUT().getName()).stream()
                .map(line -> LineStore.with(cut, line.getLineNumber())).collect(Collectors.toSet());
    }

    @Override
    public Set<org.kanonizo.framework.objects.Branch> getBranches(ClassUnderTest cut)
    {
        return ClassAnalyzer.getCoverableBranches(cut.getCUT().getName()).stream()
                .map(branch -> BranchStore.with(cut, branch.getLineNumber(), branch.getGoalId()))
                .collect(Collectors.toSet());
    }

    @Override
    public int getTotalLines(SystemUnderTest sut)
    {
        return sut.getClassesUnderTest().stream()
                .mapToInt(cut -> ClassAnalyzer.getCoverableLines(cut.getCUT().getName()).size()).sum();
    }

    @Override
    public int getTotalBranches(SystemUnderTest sut)
    {
        return sut.getClassesUnderTest().stream()
                .mapToInt(cut -> ClassAnalyzer.getCoverableBranches(cut.getCUT().getName()).size())
                .sum();
    }

    @Override
    public Set<Line> getLinesCovered(ClassUnderTest cut)
    {
        return linesCovered.values().stream().map(
                set -> set.stream().filter(line -> line.getParent().equals(cut))
                        .collect(Collectors.toSet())).collect(new HashSetCollector<>());
    }

    @Override
    public Set<Branch> getBranchesCovered(ClassUnderTest cut)
    {
        return branchesCovered.values().stream().map(
                set -> set.stream().filter(line -> line.getParent().equals(cut))
                        .collect(Collectors.toSet())).collect(new HashSetCollector<>());
    }

    @Override
    public ClassLoader getClassLoader()
    {
        return InstrumentingClassLoader.getInstance();
    }

    @Override
    public String readableName()
    {
        return "Scythe";
    }

    @Override
    public String commandLineSwitch()
    {
        return SCYTHE_INSTRUMENTER.commandLineSwitch;
    }
}