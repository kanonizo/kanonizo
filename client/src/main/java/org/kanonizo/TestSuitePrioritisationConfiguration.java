package org.kanonizo;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.kanonizo.TestSuitePrioritisationConfiguration.CommandLineOption.terminalOptions;

public class TestSuitePrioritisationConfiguration
{
    private final CommandLine line;

    public enum CommandLineOption
    {
        HELP_OPTION("h", "help", "Prints out this help and returns", true),
        ALGORITHM_OPTION("a", "algorithm", "The choice of algorithm to use. Options are Greedy, AdditionalGreedy, KOptimal, Irreplaceability, EIrreplaceability, HillClimb, GeneticAlgorithm, HypervolumeGA, EpistaticGA. If not specified the default choice will be the Greedy Algorithm"),
        SOURCE_FOLDER_OPTION("s", "sourceFolder", "Directory containing all source classes for the program. The source code will be instrumented while running the tests contained in these classes. Source classes should not be included"),
        TEST_FOLDER_OPTION("t", "testFolder",  "Directory containing all test classes for the program. The source code will be instrumented while running the tests contained in these classes. Source classes should not be included"),
        LIB_FOLDER_OPTION("l", "libFolder", "Library of all jar files required in order to run the source or the tests. This is an optional parameter, in the case of this project being controlled by maven dependencies will be automatically resolved assuming a project structure of {project_root}/target/classes and {project_root}/target/tests"),
        INSTRUMENTER_OPTION("i", "instrumenter", "The instrumenter to use to collect code coverage. Choices are null, scythe and gzoltar"),
        GUI_OPTION("g", "gui", "Option to enable the gui"),
        ROOT_FOLDER_OPTION("r", "root", "Root folder of the target project"),
        LIST_ALGORITHMS_OPTION(null, "listAlgorithms", "List available algorithms and return", true),
        LIST_PARAMETERS_OPTION(null, "listParameters", "List available parameters and return", true);

        private final String shortOption;
        private final String longOption;
        public final String description;
        public final boolean causesExit;

        CommandLineOption(String shortOption, String longOption, String description)
        {
            this(shortOption, longOption, description, false);
        }

        CommandLineOption(
                String shortOption,
                String longOption,
                String description,
                boolean causesExit
        )
        {
            this.shortOption = shortOption;
            this.longOption = longOption;
            this.description = description;
            this.causesExit = causesExit;
        }

        public Optional<String> getShortOption()
        {
            return Optional.ofNullable(shortOption);
        }

        public Optional<String> getLongOption()
        {
            return Optional.ofNullable(longOption);
        }

        static List<CommandLineOption> terminalOptions()
        {
            return Arrays.stream(values()).filter(opt -> opt.causesExit).collect(toList());
        }
    }

    private TestSuitePrioritisationConfiguration(CommandLine line)
    {
        this.line = line;
    }

    public static TestSuitePrioritisationConfiguration from(CommandLine commandLine)
    {
        return new TestSuitePrioritisationConfiguration(commandLine);
    }

    public Optional<String> getCommandLineValue(CommandLineOption commandLineOption)
    {
        return line.hasOption(commandLineOption.longOption) ? Optional.of(line.getOptionValue(commandLineOption.longOption) : Optional.empty());
    }

    public Optional<List<String>> getCommandLineValues(CommandLineOption commandLineOption)
    {
        return line.hasOption(commandLineOption.longOption) ? Optional.of(asList(line.getOptionValues(commandLineOption.longOption))) : Optional.empty();
    }

    public String getRequiredCommandLineValue(CommandLineOption commandLineOption)
    {
        return getCommandLineValue(commandLineOption).orElseThrow(IllegalStateException::new);
    }

    public static Options getCommandLineOptions()
    {
        Options options = new Options();
        Option property = Option.builder("-D").argName("property=value").hasArgs().valueSeparator()
                .desc("use value for given property").build();
        options.addOption(property);
        for (CommandLineOption option : CommandLineOption.values())
        {
            Optional<String> shortOption = option.getShortOption();
            Option.Builder commandLineOptionBuilder = shortOption.map(Option::builder).orElseGet(Option::builder);
            option.getLongOption().ifPresent(commandLineOptionBuilder::longOpt);
            commandLineOptionBuilder.desc(option.description);
            options.addOption(commandLineOptionBuilder.build());
        }
        return options;
    }

    public boolean hasTerminalOption()
    {
        return terminalOptions().stream().anyMatch(opt -> line.hasOption(opt.longOption));
    }

    public CommandLineOption getTerminalOption()
    {
        return terminalOptions().stream().filter(opt -> line.hasOption(opt.longOption)).findFirst().orElseThrow(IllegalStateException::new);
    }

}
