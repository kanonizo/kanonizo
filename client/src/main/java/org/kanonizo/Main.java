package org.kanonizo;

import com.scythe.instrumenter.PropertySource;
import com.scythe.instrumenter.analysis.ClassAnalyzer;
import com.scythe.instrumenter.instrumentation.ClassReplacementTransformer;
import com.scythe.instrumenter.instrumentation.InstrumentingClassLoader;
import com.scythe.instrumenter.mutation.MutationProperties;
import com.scythe.util.ArrayUtils;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.algorithms.stoppingconditions.FitnessStoppingCondition;
import org.kanonizo.algorithms.stoppingconditions.IterationsStoppingCondition;
import org.kanonizo.algorithms.stoppingconditions.StagnationStoppingCondition;
import org.kanonizo.algorithms.stoppingconditions.TimeStoppingCondition;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.framework.SUTChromosome;
import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestSuiteChromosome;
import org.kanonizo.framework.instrumentation.ScytheInstrumenter;
import org.kanonizo.util.Util;
import org.reflections.Reflections;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Main {
    public static final Logger logger = LogManager.getLogger(Main.class);
    private static final String[] forbiddenPackages = new String[] { "com/dpaterson", "org/junit",
            "org/apache/commons/cli", "junit", "org/apache/bcel", "org/apache/logging/log4j", "org/objectweb/asm",
            "javax/swing", "javax/servlet", "org/xml" };

    public static void main(String[] args) {
        // org.evosuite.Properties.TT = true;
        ClassAnalyzer.setOut(System.out);
        Arrays.asList(forbiddenPackages).stream().forEach(s -> ClassReplacementTransformer.addForbiddenPackage(s));
        Options options = TestSuitePrioritisation.getCommandLineOptions();
        Framework fw = new Framework();
        try {
            CommandLine line = new DefaultParser().parse(options, args, false);
            if (TestSuitePrioritisation.hasHelpOption(line)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Search Algorithms", options);
                return;
            }
            List<PropertySource> sources = ArrayUtils.createList(Properties.instance(),
                    com.scythe.instrumenter.InstrumentationProperties.instance(), MutationProperties.instance());
            TestSuitePrioritisation.handleProperties(line, sources);
            if (MutationProperties.VISIT_MUTANTS) {
                InstrumentingClassLoader.getInstance().setVisitMutants(true);
            }
            setupFramework(line, fw);
            fw.run();
        } catch (final ParseException | ClassNotFoundException e) {
            logger.error(e);
        }
        // necessary due to random thread creation during test cases (don't do
        // this ever again)
        System.exit(0);
    }

    /**
     * Takes options from the {@link CommandLine} and creates a {@link TestSuiteChromosome} instance containing all of the test cases and a {@link SUTChromosome} object containing all of the source
     * classes. The command line must contain a -s/--sourceFolder option for the source classes and a -t/--testFolder option for the test cases. Both of these folders will be added to the classpath,
     * and all nested .class files will be loaded in as either source or test cases ready for instrumentation. Currently, instrumentation and JUnit execution takes place in the constructor of a
     * {@link TestSuiteChromosome}, but this may well be changed as fitness functions are introduced that are not reliant on code coverage.
     *
     * @param line
     *            - the {@link CommandLine} instance which must contain -s and -t options for source and test folders respectively
     * @return a {@link TestSuiteChromosome} object containing a {@link SUTChromosome} with all of the source classes and a list of {@link TestCaseChromosome} objects for all of the test cases
     *         contained within the specified location
     */
    public static void setupFramework(CommandLine line, Framework fw) throws MissingOptionException {
        ScytheInstrumenter.getNullOut();
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
                fw.addLibFolder(file);
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
        File root = file.getParentFile().getParentFile();
        if (MavenAnalyser.isMavenProject(root) && fw.getLibFolders().isEmpty()) {
            MavenAnalyser.addMavenDependencies(root);
        }
        // test folder
        folder = line.getOptionValue("t");
        // relative path
        file = Util.getFile(folder);
        fw.setTestFolder(file);
        String algorithmChoice = line.hasOption("a") ? line.getOptionValue("a") : "";
        fw.setAlgorithm(getAlgorithm(algorithmChoice));
    }

    private static SearchAlgorithm getAlgorithm(String algorithmChoice) {
        Reflections r = new Reflections();
        Set<Class<?>> algorithms = r.getTypesAnnotatedWith(Algorithm.class);
        SearchAlgorithm algorithm = null;
        for(Class<?> c : algorithms){
            if(algorithmChoice.equals(c.getAnnotation(Algorithm.class).readableName())){
                try {
                    algorithm = (SearchAlgorithm) c.newInstance();
                    break;
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        algorithm.addStoppingCondition(new FitnessStoppingCondition());
        if (Properties.USE_TIME) {
            algorithm.addStoppingCondition(new TimeStoppingCondition());
        }
        if (Properties.USE_ITERATIONS) {
            algorithm.addStoppingCondition(new IterationsStoppingCondition());
        }
        if (Properties.USE_STAGNATION) {
            algorithm.addStoppingCondition(new StagnationStoppingCondition());
        }
        return algorithm;
    }

}
