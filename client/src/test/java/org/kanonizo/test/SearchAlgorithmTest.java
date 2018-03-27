package org.kanonizo.test;

import static org.junit.Assert.fail;

import com.scythe.instrumenter.InstrumentationProperties;
import com.scythe.instrumenter.instrumentation.ClassReplacementTransformer;
import com.scythe.instrumenter.instrumentation.InstrumentingClassLoader;
import java.io.File;
import java.util.Arrays;
import org.junit.Before;
import org.junit.BeforeClass;
import org.kanonizo.Framework;
import org.kanonizo.Main;
import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.instrumenters.ScytheInstrumenter;

public abstract class SearchAlgorithmTest extends MockitoTest {
    protected SearchAlgorithm algorithm;
    protected Instrumenter scytheInst = new ScytheInstrumenter();
    private TestSuite tsc;


    protected SearchAlgorithmTest(SearchAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    protected TestSuite getTsc() {
        return tsc;
    }

    @Before
    public void setup(){
        Framework f = Framework.getInstance();
        f.setInstrumenter(scytheInst);
        f.setSourceFolder(new File("./testing/src"));
        f.setTestFolder(new File("./testing/test"));
        try{
            InstrumentationProperties.WRITE_CLASS_IF_MODIFIED = true;
            Thread.currentThread().setContextClassLoader(InstrumentingClassLoader.getInstance());
            scytheInst.loadClass("sample_classes.Stack");
            scytheInst.loadClass("sample_tests.StackTest");
        }catch(ClassNotFoundException e){
            fail("Couldn't find class sample_classes.Stack");
        }
        f.setAlgorithm(algorithm);
        try{
            f.run();
        }catch(ClassNotFoundException e){
            e.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
