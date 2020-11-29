package org.kanonizo.algorithms.faultprediction;

import com.google.common.io.Files;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.TestCasePrioritiser;
import org.kanonizo.algorithms.heuristics.AdditionalGreedyAlgorithm;
import org.kanonizo.algorithms.heuristics.comparators.AdditionalGreedyComparator;
import org.kanonizo.algorithms.heuristics.comparators.GreedyComparator;
import org.kanonizo.algorithms.heuristics.comparators.RandomComparator;
import org.kanonizo.algorithms.heuristics.comparators.ConstraintSolverComparator;
import org.kanonizo.algorithms.heuristics.comparators.DissimilarityComparator;
import org.kanonizo.annotations.Prerequisite;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.configuration.configurableoption.BooleanOption;
import org.kanonizo.configuration.configurableoption.ConfigurableOption;
import org.kanonizo.configuration.configurableoption.DoubleOption;
import org.kanonizo.configuration.configurableoption.FileOption;
import org.kanonizo.configuration.configurableoption.IntOption;
import org.kanonizo.display.Display;
import org.kanonizo.framework.ClassStore;
import org.kanonizo.framework.ObjectiveFunction;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.listeners.TestOrderChangedListener;
import org.kanonizo.util.Util;

import static java.util.Comparator.comparingDouble;
import static org.kanonizo.configuration.configurableoption.BooleanOption.booleanOption;
import static org.kanonizo.configuration.configurableoption.ConfigurableOption.configurableOptionFrom;
import static org.kanonizo.configuration.configurableoption.DoubleOption.doubleOption;
import static org.kanonizo.configuration.configurableoption.FileOption.fileOption;
import static org.kanonizo.configuration.configurableoption.IntOption.intOption;

public class Schwa extends TestCasePrioritiser
{
    private static final DoubleOption REVISIONS_WEIGHT_OPTION = doubleOption(
            "schwa_revisions_weight",
            0.3
    );
    private static final DoubleOption AUTHORS_WEIGHT_OPTION = doubleOption(
            "schwa_authors_weight",
            0.2;
    private static final DoubleOption FIXES_WEIGHT_OPTION = doubleOption(
            "schwa_fixes_weight",
            0.5
    );
    private static final ConfigurableOption<ObjectiveFunction> SECONDARY_OBJECTIVE_FUNCTION = configurableOptionFrom(
            "schwa_secondary_objective",
            ObjectiveFunction.class,
            new GreedyComparator(),
            Schwa::getOptions
    );

    private static final IntOption CLASSES_PER_GROUP_OPTION = intOption(
            "classes_per_group",
            1
    );
    private static final BooleanOption USE_PERCENTAGE_OF_CLASSES_OPTION = booleanOption(
            "use_percentage_of_classes",
            false
    );
    private static final FileOption SCHWA_FILE_OPTION = fileOption("schwa_file");

    private static Logger logger = LogManager.getLogger(Schwa.class);

    private final double revisionsWeight;
    private final double authorsWeight;
    private final double fixesWeight;
    private final ObjectiveFunction secondaryObjective;
    private final int classesPerGroup;
    private final boolean usePercentageOfClasses;
    private final File schwaFile;

    private final List<SchwaClass> active = new ArrayList<>();
    private List<SchwaClass> classes;
    private List<TestCase> testCasesForActive = new ArrayList<>();
    private int totalClasses;

    public Schwa(
            KanonizoConfigurationModel configurationModel,
            List<TestOrderChangedListener> testOrderChangedListeners,
            Instrumenter instrumenter,
            Display display
    ) throws IOException
    {
        super(configurationModel, testOrderChangedListeners, instrumenter, display);
        this.revisionsWeight = configurationModel.getConfigurableOptionValue(REVISIONS_WEIGHT_OPTION);
        this.authorsWeight = configurationModel.getConfigurableOptionValue(AUTHORS_WEIGHT_OPTION);
        this.fixesWeight = configurationModel.getConfigurableOptionValue(FIXES_WEIGHT_OPTION);
        this.secondaryObjective = configurationModel.getConfigurableOptionValue(SECONDARY_OBJECTIVE_FUNCTION);
        this.classesPerGroup = configurationModel.getConfigurableOptionValue(CLASSES_PER_GROUP_OPTION);
        this.usePercentageOfClasses = configurationModel.getConfigurableOptionValue(USE_PERCENTAGE_OF_CLASSES_OPTION);
        this.schwaFile = runSchwaIfNecessary(configurationModel.getConfigurableOptionValue(SCHWA_FILE_OPTION));

    }

    private File runSchwaIfNecessary(File providedSchwaFile) throws IOException
    {
        if (validSchwaFile(providedSchwaFile))
        {
            return providedSchwaFile;
        }

        File generatedSchwaFile = File.createTempFile("schwa-json-output", ".tmp");
        generatedSchwaFile.deleteOnExit();
        SchwaRunner runner = new SchwaRunner(generatedSchwaFile);
        runner.run();
    }

    private static boolean validSchwaFile(File schwaFile)
    {
        return schwaFile != null && schwaFile.exists() && schwaFile.getAbsolutePath()
                .endsWith(".json");
    }

    public void init(List<TestCase> testCases)
    {
        super.init(testCases);
        // run schwa
        try
        {
            Gson gson = new Gson();
            SchwaRoot root = gson.fromJson(new FileReader(schwaFile), SchwaRoot.class);
            classes = root.getChildren().stream().filter(
                    cl -> cl.getPath().endsWith(".java") && getClassFile(cl.getPath()) != null
                            && ClassStore.get(getClassName(getClassFile(cl.getPath()))) != null)
                    .collect(Collectors.toList());
            totalClasses = classes.size();
            if (classes.isEmpty())
            {
                logger.error(
                        "No classes remaining. Is the project root set correctly so that we can identify java files from the Schwa output?");
            }
            // sort classes by probability of containing a fault
            classes.sort(comparingDouble(o -> 1 - o.getProb()));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public TestCase selectTestCase(List<TestCase> testCases)
    {
        while (testCasesForActive.size() == 0)
        {
            if (classes.size() > 0)
            {
                // pick next n classes to prioritise tests for, either CLASSES_PER_GROUP or all remaining classes
                active.clear();
                int classesToSelect = Math
                        .min(
                                usePercentageOfClasses ? totalClasses * classesPerGroup / 100 : classesPerGroup,
                                classes.size()
                        );
                for (int i = 0; i < classesToSelect; i++)
                {
                    active.add(classes.get(i));
                }
                if (secondaryObjective != null && secondaryObjective.needsTargetClass())
                {
                    secondaryObjective.setTargetClasses(
                            active.stream().map(cl -> ClassStore.get(getClassName(getClassFile(cl.getPath()))).getCUT()).collect(
                                    Collectors.toList()));
                }
                classes.removeAll(active);
                // grab covering test cases
                testCasesForActive = getTestsCoveringClasses(testCases, active);
            }
            else
            {
                // if there are no more classes reported by Schwa, just add test cases in the order we found them
                testCasesForActive = testCases;
            }
            if (secondaryObjective != null)
            {
                // secondary objective sort for test cases
                testCasesForActive = secondaryObjective.sort(testCasesForActive);
            }
        }
        TestCase next = testCasesForActive.get(0);
        testCasesForActive.remove(next);
        return next;
    }

    private List<TestCase> getTestsCoveringClasses(
            List<TestCase> candidates,
            List<SchwaClass> classes
    )
    {
        Set<TestCase> tests = new HashSet<>();
        Iterator<SchwaClass> it = classes.iterator();
        while (it.hasNext())
        {
            SchwaClass cl = it.next();
            String filePath = cl.getPath();
            tests.addAll(getTestsCoveringClass(candidates, filePath));
        }
        return new ArrayList<>(tests);
    }

    private File getClassFile(String filePath)
    {
        String fullPath = fw.getRootFolder().getAbsolutePath() + File.separator + filePath;
        File javaFile = new File(fullPath);
        if (!javaFile.exists())
        {
            return null;
        }
        String className = getClassName(javaFile);
        ClassUnderTest cut = ClassStore.get(className);
        if (cut == null)
        {
            return null;
        }

        return javaFile;
    }

    private String getClassName(File javaFile)
    {
        try
        {
            List<String> lines = Files.readLines(javaFile, Charset.defaultCharset());
            Optional<String> pkgOpt = lines.stream().filter(line -> line.startsWith("package"))
                    .findFirst();
            String pkg = pkgOpt.map(p -> p.substring("package".length() + 1, p.length() - 1)).orElse("");
            String className = (!pkg.isEmpty() ? pkg + "." : "") + (javaFile.getAbsolutePath()
                    .substring(
                            javaFile.getAbsolutePath().lastIndexOf(File.separatorChar) + 1,
                            javaFile.getAbsolutePath().length() - ".java".length()
                    ));
            return className;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return "";
    }

    private List<TestCase> getTestsCoveringClass(List<TestCase> candidates, String filePath)
    {
        if (getClassFile(filePath) == null)
        {
            return Collections.emptyList();
        }

        File javaFile = getClassFile(filePath);
        String className = getClassName(javaFile);
        ClassUnderTest cut = ClassStore.get(className);
        if (cut == null)
        {
            // test class
            return Collections.emptyList();
        }
        Set<Line> linesInCut = inst.getLines(cut);
        return candidates.stream()
                .filter(tc -> inst.getLinesCovered(tc).stream().anyMatch(l -> linesInCut.contains(l)))
                .collect(
                        Collectors.toList());
    }

    enum SecondaryObjective
    {
        GREEDY("greedy", GreedyComparator::new),
        ADDITIONAL_GREEDY("additionalgreedy", AdditionalGreedyComparator::new),
        RANDOM("random", RandomComparator::new),
        CONSTRAINT_SOLVER("contraint_solver", ConstraintSolverComparator::new),
        DISSIMILARITY("dissimilarity", DissimilarityComparator::new);
        private final String commandLineSwitch;
        private final Supplier<ObjectiveFunction> implementationSupplier;

        SecondaryObjective(
                String commandLineSwitch,
                Supplier<ObjectiveFunction> implementationSupplier
        )
        {
            this.commandLineSwitch = commandLineSwitch;
            this.implementationSupplier = implementationSupplier;
        }

    }

    public static ObjectiveFunction getOptions(String objectiveName)
    {
        return Arrays.stream(SecondaryObjective.values())
                .filter(secondaryObjective -> secondaryObjective.commandLineSwitch.equalsIgnoreCase(objectiveName))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new)
                .implementationSupplier
                .get();
    }

    @Prerequisite(failureMessage = "Feature weights do not add up to 1. -Dschwa_revisions_weight, -Dschwa_authors_weight and -Dschwa_fixes_weight should sum to 1")
    public static boolean checkWeights()
    {
        return validSchwaFile() || Util
                .doubleEquals(REVISIONS_WEIGHT + AUTHORS_WEIGHT + FIXES_WEIGHT, 1);
    }

    @Prerequisite(failureMessage = "Python3 is not installed on this system or is not executable on the system path. Please check your python3 installation.")
    public static boolean checkPythonInstallation()
    {
        if (validSchwaFile())
        {
            return true;
        }
        int returnCode = runProcess("python3", "--version");
        return returnCode == 0;

    }

    @Prerequisite(failureMessage = "Schwa is not installed on this system, and Kanonizo failed to install it. Try again or visit Schwa on GitHub (https://github.com/andrefreitas/schwa) to manually install")
    public static boolean checkSchwaInstallation()
    {
        if (validSchwaFile())
        {
            return true;
        }
        int returnCode = runProcess("schwa", "-h");
        if (returnCode != 0)
        {
            returnCode = runProcess("python3", "-m", "schwa", "-h");
        }
        //TODO install Schwa if not present on users system??
        return returnCode == 0;
    }

    @Prerequisite(failureMessage = "In order to use Schwa, project root must be set. The project root must be a git repository")
    public static boolean checkProjectRoot()
    {
        if (validSchwaFile())
        {
            return true;
        }
        return Framework.getInstance().getRootFolder() != null && isGitRepository(
                Framework.getInstance().getRootFolder());
    }

    private static boolean isGitRepository(File root)
    {
        return root.listFiles((n) -> n != null && n.getName().equals(".git")).length > 0;
    }

    @Override
    public String readableName()
    {
        return "schwa";
    }
}
