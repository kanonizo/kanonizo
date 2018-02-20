package org.kanonizo.framework.instrumentation;

import java.util.Map;
import java.util.Set;
import org.kanonizo.framework.CUTChromosome;
import org.kanonizo.framework.SUTChromosome;
import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestSuiteChromosome;

public interface Instrumenter {
    Class<?> loadClass(String className) throws ClassNotFoundException;
    void setTestSuite(TestSuiteChromosome ts);
    void collectCoverage();
    Map<CUTChromosome, Set<Integer>> getLinesCovered(TestCaseChromosome testCase);
    Map<CUTChromosome, Set<Integer>> getBranchesCovered(TestCaseChromosome testCase);
    int getTotalLines(CUTChromosome cut);
    int getTotalBranches(CUTChromosome cut);
    Set<Integer> getLines(CUTChromosome cut);
    Set<Integer> getBranches(CUTChromosome cut);
    int getTotalLines(SUTChromosome sut);
    int getLinesCovered(TestSuiteChromosome testSuite);
    int getTotalBranches(SUTChromosome sut);
    int getBranchesCovered(TestSuiteChromosome testSuite);

}
