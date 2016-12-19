package com.dpaterson.testing;

import com.sheffield.instrumenter.InstrumentationProperties.Parameter;
import com.sheffield.instrumenter.PropertySource;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Properties implements PropertySource {

    private Properties() {
        reflectMap();
    }

    public static int TOTAL_TESTS;

    public static enum CoverageApproach {
        BRANCH, LINE
    }

    @Parameter(key = "number_mutations", description = "If we have decided to perform mutation based on the MUTATION_CHANCE property, the number of mutations determines how many individuals will swap places in their parent container", category = "TCP")
    public static int NUMBER_OF_MUTATIONS = 1;

    @Parameter(key = "elite", description = "The number of individuals to automatically pass through to the next generation", hasArgs = true, category = "TCP")
    public static int ELITE = 1;

    @Parameter(key = "rank_bias", description = "When using the rank selection strategy, each individual is given a rank based on its fitness. This is then used to form a proportion of numbers that will be used to select it. Depending on this number, the rank will be given more or less bias for how many numbers will result in the fitter individuals", category = "TCP")
    public static double RANK_BIAS = 1.7;

    /**
     * Selection for which type of coverage to use. Branch coverage (or decision
     * coverage) looks at statements at which the program can go one of two
     * ways. In these situations, branch coverage represents whether both
     * possible execution paths have been at some stage executed
     *
     */
    @Parameter(key = "coverage_approach", description = "Choose whether to use branch or line coverage as evaluation for the fitness function", category = "TCP")
    public static CoverageApproach COVERAGE_APPROACH = CoverageApproach.LINE;

    /**
     * Population size for metaheuristic search
     */
    @Parameter(key = "population_size", description = "Population size for the genetic algorithm", category = "TCP")
    public static int POPULATION_SIZE = 50;

    /**
     * Chance of a mutation occurring in a metaheuristic search
     */
    @Parameter(key = "mutation_chance", description = "The probability during any evolution that an individual is mutated", category = "TCP")
    public static double MUTATION_CHANCE = 0.2;

    /**
     * Chance of a crossover event occurring in a metaheuristic search
     */
    @Parameter(key = "crossover_chance", description = "The probability during any evolution that an individual is crossed over", category = "TCP")
    public static double CROSSOVER_CHANCE = 0.7;

    /**
     * The maximum amount of time allowed for execution of a metaheuristic
     * search algorithm
     */
    @Parameter(key = "max_execution_time", description = "The maximum amount of time a GA is allowed to run for", category = "TCP")
    public static long MAX_EXECUTION_TIME = 60 * 1000L;

    /**
     * The maximum number of iterations used for execution of a metaheuristic
     * search algorithm.
     */
    @Parameter(key = "max_iterations", description = "The maximum number of iterations a GA is allowed before it finishes", category = "TCP")
    public static int MAX_ITERATIONS = 10000;

    /**
     * Whether or not to use time as a stopping condition for a metaheuristic
     * search algorithm.
     */
    @Parameter(key = "use_time_stopping_condition", description = "Whether or not to use a time stopping condition. If true, then the value of MAX_EXECUTION_TIME will be used to stop the algorithm", category = "TCP")
    public static boolean USE_TIME = true;

    /**
     * Whether or not to use iterations as a stopping condition for a
     * metaheuristic search algorithm
     */
    @Parameter(key = "use_iterations_stopping_condition", description = "Whether or not to use an iterations stopping condition. If true, then the value of MAX_ITERATIONS will be used to stop the algorithm", category = "TCP")
    public static boolean USE_ITERATIONS = true;

    /**
     * Whether or not to use stagnation as a stopping condition for a
     * metaheuristic search algorithm. Stagnation refers to a period of time
     * where the fitness of the best solution in a population does not increase
     */
    @Parameter(key = "use_stagnation_stopping_condition", description = "Whether or not to use an stagnation stopping condition. If true, then the value of PATIENCE will be used to determine how long an algorithm can go without improving fitness before being stopped", category = "TCP")
    public static boolean USE_STAGNATION = true;

    /**
     * Probability of a test case being removed during mutation
     */
    @Parameter(key = "removal_chance", description = "The probability of a test case being removed during mutation. When removing test cases, where there are n test cases, each test case is removed with probability 1/n. This means that in some cases 2 test cases may be removed, and in others none will be", category = "TCP")
    public static double REMOVAL_CHANCE = 0d;

    /**
     * Probability of a test case being inserted during mutation
     */
    @Parameter(key = "insertion_chance", description = "The probability of a test case being inserted during mutation. This relies on tests having already been removed, as we cannot introduce new tests during TCP", category = "TCP")
    public static double INSERTION_CHANCE = 0d;

    /**
     * Probability of re-ordering some existing test cases during mutation
     */
    @Parameter(key = "reorder_chance", description = "The probability of test cases being reordered during mutation. If this chance is passed, two tests are selected at random and will have their places switched in the test suite", category = "TCP")
    public static double REORDER_CHANCE = 1d;

    /**
     * Fitness function to use when ordering test suites
     */
    @Parameter(key = "fitness_function", description = "The fitness function used to evaluate how good or bad a certain test case ordering within a test suite is. The default for this value is Average Percentage of Faults Detected", category = "TCP")
    public static String FITNESS_FUNC = "APFD";

    @Parameter(key = "prioritise", description = "Set to false to cancel prioritisation and simply run through the loading/instrumenting of classes. Use in conjunction with -Dwrite_class and -Dbytecode_dir to write instrumented classes", category = "TCP")
    public static boolean PRIORITISE = true;

    @Parameter(key = "profile", description = "This boolean allows debug information to be written to the console about how long functions are taking to run to find bottle necks in the program. Default is false", category = "TCP")
    public static boolean PROFILE = false;

    @Parameter(key = "kill_map", description = "This points to a file containing the kill map information generated by performing mutation analysis on the project. This is used to calculate the FEP score for each test case", category = "TCP")
    public static String KILL_MAP = "";

    @Parameter(key = "mutant_log", description = "This points to a file containing the mutation log information generated by running major on some source code. This helps to identify all mutants generated by major", category = "TCP")
    public static String MUTANT_LOG = "";

    @Parameter(key = "patience", description = "Patience refers to the amount of time an algorithm can go without improving the fitness of its best candidate solution before being considered stagnant and terminated. Usage of this property is determined by USE_STAGNATION", category = "TCP")
    public static int PATIENCE = 1000 / POPULATION_SIZE;

    @Parameter(key = "timeout", description = "Test cases can in some cases run infinitely. The timeout property allows the user to define a point at which to cut off long running test cases. The use of this property is controlled by Properties.USE_TIMEOUT", category="TCP")
    public static int TIMEOUT = 100000;

    @Parameter(key = "use_timeout", description = "Whether or not to use the test case timeout defined by Properties.TIMEOUT. Since for deterministic test cases we should not be expecting any infinite loops, it becomes less likely that timeouts will be hit", category = "TCP")
    public static boolean USE_TIMEOUT = true;

    /**
     * Not intended to be used from command line, so don't have an
     * {@link Parameter} annotation for this. Used to determine whether or not
     * we want the class loader to instrument the classes or not.
     */
    public static boolean INSTRUMENT = true;

    @Parameter(key = "track_generation_fitness", description = "In the FitnessWriter it is possible to track the current fitness evaluation or the entire generation max fitness. Seeing the entire generation max fitness allows the user to see the progression of the population over time (for example in the GA), while seeing the individual fitness allows to see the spread of fitness scores across the population/evolutions. Set to true to track the whole generation fitness, set to false to see individual evaluation fitness", category = "TCP")
    public static boolean TRACK_GENERATION_FITNESS = true;

    private Map<String, Field> parameterMap = new HashMap<String, Field>();

    private void reflectMap() {
        Arrays.asList(Properties.class.getFields()).forEach(field -> {
            if (field.isAnnotationPresent(Parameter.class)) {
                parameterMap.put(field.getAnnotation(Parameter.class).key(), field);
            }
        });
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setParameter(String key, String value) throws IllegalArgumentException, IllegalAccessException {
        if (!parameterMap.containsKey(key)) {
            throw new IllegalArgumentException(key + " was not found in the Properties class");
        }

        Field f = parameterMap.get(key);
        Class<?> cl = f.getType();
        if (cl.isAssignableFrom(Number.class) || cl.isPrimitive())

        {
            try {
                if (cl.equals(Long.class) || cl.equals(long.class)) {
                    Long l = Long.parseLong(value);
                    f.setLong(null, l);
                } else if (cl.equals(Double.class) || cl.equals(double.class)) {
                    Double d = Double.parseDouble(value);
                    f.setDouble(null, d);
                } else if (cl.equals(Float.class) || cl.equals(float.class)) {
                    Float fl = Float.parseFloat(value);
                    f.setFloat(null, fl);
                } else if (cl.equals(Integer.class) || cl.equals(int.class)) {
                    Integer in = Integer.parseInt(value);
                    f.setInt(null, in);
                } else if (cl.equals(Boolean.class) || cl.equals(boolean.class)) {
                    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                        throw new IllegalArgumentException("String " + value + " is not of the type boolean");
                    }
                    Boolean bl = Boolean.parseBoolean(value);
                    f.setBoolean(null, bl);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Required type for parameter " + f.getName() + " was not met by value" + value);
            }
        } else if (cl.isAssignableFrom(String.class))

        {
            f.set(null, value);
        }
        if (f.getType().isEnum())

        {
            f.set(null, Enum.valueOf((Class<Enum>) f.getType(), value.toUpperCase()));
        }
    }

    @Override
    public boolean hasParameter(String name) {
        return parameterMap.keySet().contains(name);
    }

    @Override
    public Set<String> getParameterNames() {
        return parameterMap.keySet();
    }

    private static Properties instance;

    public static Properties instance() {
        if (instance == null) {
            instance = new Properties();
        }
        return instance;
    }

}
