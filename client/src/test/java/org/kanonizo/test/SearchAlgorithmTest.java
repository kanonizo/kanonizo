package org.kanonizo.test;

import java.io.File;
import org.junit.Before;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.framework.objects.TestSuite;

public abstract class SearchAlgorithmTest extends MockitoTest {
    protected SearchAlgorithm algorithm;

    private TestSuite tsc;

    protected SearchAlgorithmTest(SearchAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    protected TestSuite getTsc() {
        return tsc;
    }

    @Before
    public void setup() throws ClassNotFoundException {
        Framework f = Framework.getInstance();
        f.setSourceFolder(new File("./testing/sample_classes"));
        f.setTestFolder(new File("./testing/sample_tests"));
        f.setAlgorithm(algorithm);
        f.run();
    }

}
