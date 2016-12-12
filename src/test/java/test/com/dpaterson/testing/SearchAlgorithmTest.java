package test.com.dpaterson.testing;

import java.io.File;

import org.junit.Before;

import com.dpaterson.testing.Framework;
import com.dpaterson.testing.algorithms.SearchAlgorithm;
import com.dpaterson.testing.framework.TestSuiteChromosome;

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
