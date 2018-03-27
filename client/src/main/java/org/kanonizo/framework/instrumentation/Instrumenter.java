package org.kanonizo.framework.instrumentation;

import java.util.Set;
import org.kanonizo.framework.Readable;
import org.kanonizo.framework.objects.Branch;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;

public interface Instrumenter extends Readable {
  Class<?> loadClass(String className) throws ClassNotFoundException;

  void setTestSuite(TestSuite ts);

  void collectCoverage();

  Set<Line> getLinesCovered(TestCase testCase);

  Set<Branch> getBranchesCovered(TestCase testCase);

  int getTotalLines(ClassUnderTest cut);

  int getTotalBranches(ClassUnderTest cut);

  Set<Line> getLines(ClassUnderTest cut);

  Set<Branch> getBranches(ClassUnderTest cut);

  int getTotalLines(SystemUnderTest sut);

  int getLinesCovered(TestSuite testSuite);

  int getTotalBranches(SystemUnderTest sut);

  int getBranchesCovered(TestSuite testSuite);

  Set<Line> getLinesCovered(ClassUnderTest cut);

  Set<Line> getLinesCovered(SystemUnderTest sut);

  Set<Branch> getBranchesCovered(ClassUnderTest cut);

  Set<Branch> getBranchesCovered(SystemUnderTest sut);

  ClassLoader getClassLoader();
}
