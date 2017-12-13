package org.kanonizo.framework.instrumentation;

import org.kanonizo.framework.CUTChromosome;
import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestSuiteChromosome;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Instrumenter {
    Class<?> loadClass(String className) throws ClassNotFoundException;
    void setTestSuite(TestSuiteChromosome ts);
    void runTestCases();
    Map<String, Set<Integer>> getLinesCovered(TestCaseChromosome testCase);
    Map<String, Set<Integer>> getBranchesCovered(TestCaseChromosome testCase);
    double getLineCoverage(CUTChromosome cut);
    double getBranchCoverage(CUTChromosome cut);
    List<Class<?>> getAffectedClasses();
}
