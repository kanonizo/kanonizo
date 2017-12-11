package org.kanonizo.test;

import org.junit.Before;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.framework.TestSuiteChromosome;

import java.io.File;

public abstract class SearchAlgorithmTest extends MockitoTest {
    protected SearchAlgorithm algorithm;

    private TestSuiteChromosome tsc;

    protected SearchAlgorithmTest(SearchAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    protected TestSuiteChromosome getTsc() {
        return tsc;
    }

    @Before
    public void setup() throws ClassNotFoundException {
        Framework f = new Framework();
        f.setSourceFolder(new File("./testing/sample_classes"));
        f.setTestFolder(new File("./testing/sample_tests"));
        f.setAlgorithm(algorithm);
        f.run();
    }

}
