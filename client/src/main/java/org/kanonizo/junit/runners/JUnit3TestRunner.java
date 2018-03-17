package org.kanonizo.junit.runners;

import com.scythe.instrumenter.InstrumentationProperties;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import junit.textui.TestRunner;
import org.apache.bcel.classfile.ClassParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.instrumenters.ScytheInstrumenter;
import org.kanonizo.junit.KanonizoTestFailure;
import org.kanonizo.junit.KanonizoTestResult;
import org.kanonizo.util.NullPrintStream;

public class JUnit3TestRunner extends TestRunner implements KanonizoTestRunner {
  private static Logger logger = LogManager.getLogger(JUnit3TestRunner.class);

  public JUnit3TestRunner() {
    super(NullPrintStream.instance);
  }

  @Override
  public KanonizoTestResult runTest(TestCase tc) {
    Test test = createMethodSuite(tc.getTestClass(), tc.getMethod());
    if (test == null){
      logger.error("Unable to create test case");
      throw new RuntimeException();
    }
    long startTime = System.currentTimeMillis();
    TestResult result = doRun(test, false);
    long runTime = System.currentTimeMillis() - startTime;
    List<KanonizoTestFailure> failures = new ArrayList<>();
    Enumeration<TestFailure> errors = result.failures();
    while(errors.hasMoreElements()){
      TestFailure failure = errors.nextElement();
      failures.add(new KanonizoTestFailure(failure.thrownException(), failure.trace()));
    }
    return new KanonizoTestResult(tc.getTestClass(), tc.getMethod(), result.wasSuccessful(), failures, runTime);
  }

  private static Test createMethodSuite(Class<?> testClass, Method testMethod) {
    try {
      Constructor constructor = testClass.getConstructor(new Class[]{String.class});
      return (Test) constructor.newInstance(new Object[]{testMethod.getName()});
    } catch (NoSuchMethodException e) {
      try {
        Constructor constructor = testClass.getConstructor(new Class[0]);
        junit.framework.TestCase test = (junit.framework.TestCase) constructor.newInstance(new Object[0]);
        test.setName(testMethod.getName());
        return test;
      } catch (ClassCastException e1) {
        boolean methodExists;
        try {
          //noinspection SSBasedInspection
          testClass.getMethod(testMethod.getName(), new Class[0]);
          methodExists = true;
        } catch (NoSuchMethodException e2) {
          methodExists = false;
        }
        if (!methodExists) {
          logger.error("Trying to invoke a test method that does not exist: " + testClass.getName() + "." + testMethod.getName());
        }
        return null;
      } catch (Exception e1) {
        logger.error(JUnit3TestRunner.class.getSimpleName() + " was unable to instantiate a new instance of the test class "+testClass.getSimpleName() + "." + testMethod.getName());
      }
    } catch (Throwable e) {
      logger.error(JUnit3TestRunner.class.getSimpleName() + " was unable to instantiate a new instance of the test class"+testClass.getSimpleName() + "." + testMethod.getName());
    }
    return null;
  }

  public static void main(String[] args){
    if(args.length > 0){
      String fileName = args[0];
      File f = new File(fileName);
      if(!f.exists()){
        System.out.println("Error: File "+f+" not found");
      }
      TestCase.USE_TIMEOUT = false;
      InstrumentationProperties.WRITE_CLASS = true;
      Class<?> cl = loadClassFromFile(f);
      String testMethod = args[1];
      try {
        Method m = cl.getMethod(testMethod);
        TestCase tc = new TestCase(cl,m);
        tc.run();
        if(tc.hasFailures()){
          tc.getFailures().stream().forEach(fail -> fail.getCause().printStackTrace());
        }
        System.out.println("1 Test Case run: "+tc.getFailures().size() + " failures");
        Optional<String> failures = tc.getFailures().stream().map(fail -> fail.getTrace()).reduce((a, b) -> a+"\n"+b);
        if(failures.isPresent()){
          System.out.println(failures.get());
        }
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      }


    }
  }
  private static Class<?> loadClassFromFile(File f){
    Class<?> cl = null;
    try {
      ClassParser parser = new ClassParser(f.getAbsolutePath());
      org.apache.bcel.classfile.JavaClass jcl = parser.parse();
      ScytheInstrumenter inst = new ScytheInstrumenter();
      cl = inst.loadClass(jcl.getClassName());

    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      logger.error(e);
    } catch (IOException e) {
      logger.error(e);
    }
    return cl;
  }
}
