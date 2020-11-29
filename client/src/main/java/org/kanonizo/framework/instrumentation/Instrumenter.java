package org.kanonizo.framework.instrumentation;

import java.util.List;
import java.util.Set;

import org.kanonizo.framework.Readable;
import org.kanonizo.framework.objects.Branch;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.util.HashSetCollector;

public interface Instrumenter extends Readable
{
    Class<?> loadClass(String className) throws ClassNotFoundException;

    void collectCoverage(TestSuite testSuite);

    Set<Line> getLinesCovered(TestCase testCase);

    Set<Branch> getBranchesCovered(TestCase testCase);

    int getTotalLines(ClassUnderTest cut);

    int getTotalBranches(ClassUnderTest cut);

    Set<Line> getLines(ClassUnderTest cut);

    Set<Branch> getBranches(ClassUnderTest cut);

    Set<Line> getLinesCovered(ClassUnderTest cut);

    Set<Branch> getBranchesCovered(ClassUnderTest cut);

    default int getTotalLines(SystemUnderTest sut)
    {
        return sut.getClassesUnderTest().stream().mapToInt(this::getTotalLines).sum();
    }

    default int getLinesCovered(TestSuite testSuite)
    {
        return testSuite.getTestCases().stream().mapToInt(testCase -> getLinesCovered(testCase).size()).sum();
    }

    default  int getTotalBranches(SystemUnderTest sut)
    {
        return sut.getClassesUnderTest().stream().mapToInt(this::getTotalBranches).sum();
    }

    default int getBranchesCovered(TestSuite testSuite)
    {
        return testSuite.getTestCases().stream().mapToInt(testCase -> getBranchesCovered(testCase).size()).sum();
    }

    default Set<Line> getLinesCovered(List<ClassUnderTest> classesUnderTest)
    {
        return classesUnderTest.stream().map(this::getLinesCovered).collect(new HashSetCollector<>());
    }

    default Set<Branch> getBranchesCovered(List<ClassUnderTest> classesUnderTest)
    {
        return classesUnderTest.stream().map(this::getBranchesCovered).collect(new HashSetCollector<>());
    }

    ClassLoader getClassLoader();

    String commandLineSwitch();
}
