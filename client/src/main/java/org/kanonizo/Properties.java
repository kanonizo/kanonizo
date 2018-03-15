package org.kanonizo;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;

public class Properties {

    public static int TOTAL_TESTS;

    public static enum CoverageApproach {
        BRANCH, LINE
    }

    @Parameter(key = "number_mutations", description = "If we have decided to perform mutation based on the MUTATION_CHANCE property, the number of mutations determines how many individuals will swap places in their parent container", category = "TCP")
    public static int NUMBER_OF_MUTATIONS = 1;



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
     * Fitness function to use when ordering test suites
     */
    @Parameter(key = "fitness_function", description = "The fitness function used to evaluate how good or bad a certain test case ordering within a test suite is. The default for this value is Average Percentage of Faults Detected", category = "TCP")
    public static String FITNESS_FUNC = "APFD";

    @Parameter(key = "prioritise", description = "Set to false to cancel prioritisation and simply run through the loading/instrumenting of classes. Use in conjunction with -Dwrite_class and -Dbytecode_dir to write instrumented classes", category = "TCP")
    public static boolean PRIORITISE = true;

    @Parameter(key = "profile", description = "This boolean allows debug information to be written to the console about how long functions are taking to run to find bottle necks in the program. Default is false", category = "TCP")
    public static boolean PROFILE = false;


    /**
     * Not intended to be used from command line, so don't have an
     * {@link Parameter} annotation for this. Used to determine whether or not
     * we want the class loader to instrument the classes or not.
     */
    public static boolean INSTRUMENT = true;



    @Parameter(key = "instrumenter", description = "Choice of instrumentation tool to use to collect code coverage from test case execution", category="TCP")
    public static String INSTRUMENTER = "Scythe";


}
