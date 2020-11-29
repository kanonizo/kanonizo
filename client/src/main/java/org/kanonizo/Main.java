package org.kanonizo;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import com.scythe.instrumenter.instrumentation.InstrumentingClassLoader;
import com.scythe.instrumenter.mutation.MutationProperties;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.algorithms.Algorithm;
import org.kanonizo.algorithms.Algorithm.AlgorithmFactory;
import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.annotations.Prerequisite;
import org.kanonizo.commandline.Table;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.display.Display;
import org.kanonizo.display.DisplayType;
import org.kanonizo.display.DisplayType.DisplayFactory;
import org.kanonizo.exception.SystemConfigurationException;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.instrumenters.InstrumenterType;
import org.kanonizo.instrumenters.InstrumenterType.InstrumenterFactory;
import org.kanonizo.util.Util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.kanonizo.TestSuitePrioritisationConfiguration.CommandLineOption.ALGORITHM_OPTION;
import static org.kanonizo.TestSuitePrioritisationConfiguration.CommandLineOption.GUI_OPTION;
import static org.kanonizo.TestSuitePrioritisationConfiguration.CommandLineOption.INSTRUMENTER_OPTION;
import static org.kanonizo.TestSuitePrioritisationConfiguration.CommandLineOption.LIB_FOLDER_OPTION;
import static org.kanonizo.TestSuitePrioritisationConfiguration.CommandLineOption.ROOT_FOLDER_OPTION;
import static org.kanonizo.TestSuitePrioritisationConfiguration.CommandLineOption.SOURCE_FOLDER_OPTION;
import static org.kanonizo.TestSuitePrioritisationConfiguration.CommandLineOption.TEST_FOLDER_OPTION;
import static org.kanonizo.algorithms.Algorithm.GREEDY;
import static org.kanonizo.configuration.KanonizoConfigurationModel.fromCommandLine;
import static org.kanonizo.display.DisplayType.CONSOLE_DISPLAY;
import static org.kanonizo.instrumenters.InstrumenterType.SCYTHE_INSTRUMENTER;

public class Main
{

    public static final Logger logger = LogManager.getLogger(Main.class);


    public static void main(String[] args)
    {
        // org.evosuite.Properties.TT = true;
        Options options = TestSuitePrioritisationConfiguration.getCommandLineOptions();
        try
        {
            Set<Field> parameters = Util.getParameters();
            CommandLine line = new DefaultParser().parse(options, args, false);
            TestSuitePrioritisationConfiguration configuration = TestSuitePrioritisationConfiguration.from(line);
            if (configuration.hasTerminalOption())
            {
                switch (configuration.getTerminalOption())
                {
                    case HELP_OPTION:
                        HelpFormatter formatter = new HelpFormatter();
                        formatter.printHelp("Search Algorithms", options);
                        break;
                    case LIST_ALGORITHMS_OPTION:
                        Table availableAlgorithms = new Table(20, 50);
                        availableAlgorithms.setHeaders("Algorithm Name", "Algorithm Description");
                        Arrays.stream(Algorithm.values()).forEach(
                                alg -> availableAlgorithms.addRow(alg.readableName, alg.description));
                        availableAlgorithms.print();
                        break;
                    case LIST_PARAMETERS_OPTION:
                        Table params = new Table(35, 20, 100);
                        params.setHeaders("Parameter Key", "Parameter Group", "Description");
                        ArrayList<Field> ordered = new ArrayList<>(parameters);

                        ordered.sort(Comparator.comparing(o -> o.getAnnotation(Parameter.class).key()));
                        ordered.forEach(
                                par ->
                                {

                                    Parameter p = par.getAnnotation(Parameter.class);
                                    params.addRow(p.key(), p.category(), p.description());

                                });
                        params.print();
                        break;
                    default:
                        throw new IllegalStateException("Unexpected terminal command line option: " + configuration.getTerminalOption());
                }
                return;
            }

            TestSuitePrioritisation.handleProperties(line, parameters);
            InstrumentingClassLoader.getInstance().setVisitMutants(MutationProperties.VISIT_MUTANTS);
            KanonizoConfigurationModel configModel = fromCommandLine(line);
            Framework framework = buildFramework(configuration, configModel);
            TestSuite result = framework.run();

        }
        catch (MissingOptionException ignored)
        {
        }
        catch (Exception e)
        {
            logger.error(e);
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
     * @param configuration - the {@link CommandLine} instance which must contain -s and -t options for source
     *                      and test folders respectively
     * @param configModel
     * @return a {@link TestSuite} object containing a {@link SystemUnderTest} with all of the source
     * classes and a list of {@link TestCase} objects for all of the test cases contained within the
     * specified location
     */
    private static Framework buildFramework(
            TestSuitePrioritisationConfiguration configuration,
            KanonizoConfigurationModel configModel
    ) throws Exception
    {
        if (!configuration.getCommandLineValue(SOURCE_FOLDER_OPTION).isPresent() && !configuration.getCommandLineValue(
                TEST_FOLDER_OPTION).isPresent())
        {
            throw new MissingOptionException(
                    "In order to run the test suite prioritisation, a source folder must be given using -s or --sourceFolder and a test folder must be given using -t or --testFolder");
        }

        if (!MutationProperties.MAJOR_ROOT.isEmpty())
        {
            File majorJar = Util.getFile(MutationProperties.MAJOR_ROOT + "/config/config.jar");
            Util.addToClassPath(majorJar);
        }
        File sourceFolder = Util.getFile(configuration.getRequiredCommandLineValue(SOURCE_FOLDER_OPTION));
        File testFolder = Util.getFile(configuration.getRequiredCommandLineValue(TEST_FOLDER_OPTION));
        Optional<String> rootFolderName = configuration.getCommandLineValue(ROOT_FOLDER_OPTION);
        File rootFolder = rootFolderName.map(Util::getFile).orElse(null);
        List<File> libFolders = configuration.getCommandLineValues(LIB_FOLDER_OPTION).map(suppliedLibFolders -> suppliedLibFolders.stream().map(
                Util::getFile).collect(toList())).orElse(emptyList());
        Display display = getDisplay(configuration, configModel);
        String instrumenterChoice = configuration.getCommandLineValue(INSTRUMENTER_OPTION).orElse(SCYTHE_INSTRUMENTER.commandLineSwitch);
        Instrumenter instrumenter = getInstrumenter(instrumenterChoice, configModel, display, sourceFolder);
        String algorithmChoice = configuration.getCommandLineValue(ALGORITHM_OPTION).orElse(GREEDY.commandLineSwitch);
        SearchAlgorithm algorithm = getAlgorithm(algorithmChoice, configModel, instrumenter, display);
        return new Framework(
                configModel,
                sourceFolder,
                testFolder,
                rootFolder,
                libFolders,
                emptyList(),
                new SystemUnderTest(configModel, instrumenter, algorithm),
                algorithm,
                instrumenter,
                display
        );
    }

    private static <T extends Display> T getDisplay(
            TestSuitePrioritisationConfiguration line,
            KanonizoConfigurationModel configModel
    )
    {
        DisplayFactory<T> displayFactory = DisplayType.fromCommandLineSwitch(
                line.getCommandLineValue(GUI_OPTION).orElse(CONSOLE_DISPLAY.commandLineSwitch)
        ).getDisplayFactory();
        T display = displayFactory.from(configModel);
        display.initialise();
        return display;
    }

    private static <T extends Instrumenter> T getInstrumenter(
            String instrumenterChoice,
            KanonizoConfigurationModel configModel,
            Display display,
            File sourceFolder
    ) throws Exception
    {
        InstrumenterFactory<T> instrumenterFactory = InstrumenterType.fromCommandLineSwitch(instrumenterChoice).getInstrumenterFactory();
        return instrumenterFactory.from(configModel, display, sourceFolder);
    }

    private static <T extends SearchAlgorithm> T getAlgorithm(
            String algorithmChoice,
            KanonizoConfigurationModel configurationModel,
            Instrumenter instrumenter,
            Display display
    ) throws IOException
    {
        Optional<AlgorithmFactory<T>> algorithmFactory = Algorithm.withCommandLineSwitch(algorithmChoice).map(Algorithm::getFactory);
        if (algorithmFactory.isPresent())
        {
            T searchAlgorithm = algorithmFactory.get().from(configurationModel, emptyList(), instrumenter, display);
            List<Prerequisite<T>> predicates = Framework.getPrerequisites(searchAlgorithm);
            Optional<String> failedPrerequisite = predicates
                    .stream()
                    .filter(predicate -> !predicate.test(searchAlgorithm))
                    .map(Prerequisite::failureMessage)
                    .findFirst();
            if (failedPrerequisite.isPresent())
            {
                throw new SystemConfigurationException(failedPrerequisite.get());
            }
            return searchAlgorithm;
        }
        List<String> algorithmNames = Arrays.stream(Algorithm.values())
                .map(alg -> alg.readableName)
                .collect(toList());

        throw new RuntimeException(
                "Algorithm could not be created. The list of available algorithms is given as: "
                        + String.join("\n", algorithmNames)
        );

    }

}
