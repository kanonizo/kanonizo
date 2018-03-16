package org.kanonizo.instrumenters;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Branch;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;

/**
 * Class to not instrument classes, not interested in the output of test cases, defers class loading
 * to the system class loader
 */
@org.kanonizo.annotations.Instrumenter(readableName = "null")
public class NullInstrumenter implements Instrumenter {

  @Override
  public Class<?> loadClass(String className) throws ClassNotFoundException {
    return ClassLoader.getSystemClassLoader().loadClass(className);
  }

  @Override
  public void setTestSuite(TestSuite ts) {

  }

  @Override
  public void collectCoverage() {

  }

  @Override
  public Set<Line> getLinesCovered(TestCase testCase) {
    return new HashSet<>();
  }

  @Override
  public Set<Branch> getBranchesCovered(TestCase testCase) {
    return new HashSet<>();
  }

  @Override
  public int getTotalLines(ClassUnderTest cut) {
    return 0;
  }

  @Override
  public int getTotalBranches(ClassUnderTest cut) {
    return 0;
  }

  @Override
  public Set<Line> getLines(ClassUnderTest cut) {
    return Collections.emptySet();
  }

  @Override
  public Set<Branch> getBranches(ClassUnderTest cut) {
    return Collections.emptySet();
  }


  @Override
  public int getTotalLines(SystemUnderTest sut) {
    return 0;
  }

  @Override
  public int getLinesCovered(TestSuite testSuite) {
    return 0;
  }

  @Override
  public int getTotalBranches(SystemUnderTest sut) {
    return 0;
  }

  @Override
  public int getBranchesCovered(TestSuite testSuite) {
    return 0;
  }

  @Override
  public Set<Line> getLinesCovered(ClassUnderTest cut) {
    return Collections.emptySet();
  }

  @Override
  public Set<Line> getLinesCovered(SystemUnderTest sut) { return Collections.emptySet(); }

  @Override
  public Set<Branch> getBranchesCovered(ClassUnderTest cut) {
    return Collections.emptySet();
  }

  @Override
  public Set<Branch> getBranchesCovered(SystemUnderTest sut) {
    return Collections.emptySet();
  }

  @Override
  public ClassLoader getClassLoader() {
    return ClassLoader.getSystemClassLoader();
  }

}
