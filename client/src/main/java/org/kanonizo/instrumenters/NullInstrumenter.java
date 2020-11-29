package org.kanonizo.instrumenters;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.display.Display;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Branch;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;

import static org.kanonizo.instrumenters.InstrumenterType.NULL_INSTRUMENTER;

/**
 * Class to not instrument classes, not interested in the output of test cases, defers class loading
 * to the system class loader
 */
public class NullInstrumenter implements Instrumenter
{

    @SuppressWarnings("unused")
    public NullInstrumenter(KanonizoConfigurationModel configurationModel, Display display, File sourceFolder)
    {
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException
    {
        return ClassLoader.getSystemClassLoader().loadClass(className);
    }

    @Override
    public void collectCoverage(TestSuite testSuite)
    {

    }

    @Override
    public Set<Line> getLinesCovered(TestCase testCase)
    {
        return new HashSet<>();
    }

    @Override
    public Set<Branch> getBranchesCovered(TestCase testCase)
    {
        return new HashSet<>();
    }

    @Override
    public int getTotalLines(ClassUnderTest cut)
    {
        return 0;
    }

    @Override
    public int getTotalBranches(ClassUnderTest cut)
    {
        return 0;
    }

    @Override
    public Set<Line> getLines(ClassUnderTest cut)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<Branch> getBranches(ClassUnderTest cut)
    {
        return Collections.emptySet();
    }


    @Override
    public int getTotalLines(SystemUnderTest sut)
    {
        return 0;
    }

    @Override
    public int getTotalBranches(SystemUnderTest sut)
    {
        return 0;
    }

    @Override
    public Set<Line> getLinesCovered(ClassUnderTest cut)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<Line> getLinesCovered(List<ClassUnderTest> classesUnderTest)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<Branch> getBranchesCovered(ClassUnderTest cut)
    {
        return Collections.emptySet();
    }

    @Override
    public ClassLoader getClassLoader()
    {
        return ClassLoader.getSystemClassLoader();
    }

    @Override
    public String readableName()
    {
        return "null";
    }

    @Override
    public String commandLineSwitch()
    {
        return NULL_INSTRUMENTER.commandLineSwitch;
    }
}
