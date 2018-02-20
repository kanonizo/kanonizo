package org.kanonizo.instrumenters;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.kanonizo.framework.CUTChromosome;
import org.kanonizo.framework.SUTChromosome;
import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestSuiteChromosome;
import org.kanonizo.framework.instrumentation.Instrumenter;

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
    public void collectCoverage() {

    }

    @Override
    public Map<CUTChromosome, Set<Integer>> getLinesCovered(TestCaseChromosome testCase) {
        return new HashMap<>();
    }

    @Override
    public Map<CUTChromosome, Set<Integer>> getBranchesCovered(TestCaseChromosome testCase) {
        return new HashMap<>();
    }

    @Override
    public int getTotalLines(CUTChromosome cut) {
        return 0;
    }

    @Override
    public int getTotalBranches(CUTChromosome cut) {
        return 0;
    }

    @Override
    public Set<Integer> getLines(CUTChromosome cut) {
        return Collections.emptySet();
    }

    @Override
    public Set<Integer> getBranches(CUTChromosome cut) {
        return Collections.emptySet();
    }


    @Override
    public int getTotalLines(SUTChromosome sut) {
        return 0;
    }

    @Override
    public int getLinesCovered(TestSuiteChromosome testSuite) {
        return 0;
    }

    @Override
    public int getTotalBranches(SUTChromosome sut) {
        return 0;
    }

    @Override
    public int getBranchesCovered(TestSuiteChromosome testSuite) {
        return 0;
    }

}
