package org.kanonizo.framework.instrumentation;

import org.kanonizo.framework.CUTChromosome;
import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestSuiteChromosome;

import java.util.*;

/**
 * Class to not instrument classes, not interested in the output of test cases, defers class loading to the system class
 * loader
 */
@org.kanonizo.annotations.Instrumenter(readableName = "null")
public class NullInstrumenter implements Instrumenter {
    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return ClassLoader.getSystemClassLoader().loadClass(className);
    }

    @Override
    public void setTestSuite(TestSuiteChromosome ts) {

    }

    @Override
    public void runTestCases() {

    }

    @Override
    public Map<String, Set<Integer>> getLinesCovered(TestCaseChromosome testCase) {
        return new HashMap<>();
    }

    @Override
    public Map<String, Set<Integer>> getBranchesCovered(TestCaseChromosome testCase) {
        return new HashMap<>();
    }

    @Override
    public double getLineCoverage(CUTChromosome cut) {
        return 0;
    }

    @Override
    public double getBranchCoverage(CUTChromosome cut) {
        return 0;
    }

    @Override
    public List<Class<?>> getAffectedClasses() {
        return Collections.emptyList();
    }

}
