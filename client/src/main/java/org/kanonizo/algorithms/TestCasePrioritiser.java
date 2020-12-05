package org.kanonizo.algorithms;

import org.kanonizo.Framework;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.display.Display;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.listeners.TestOrderChangedListener;

import java.util.LinkedList;
import java.util.List;

public abstract class TestCasePrioritiser extends AbstractSearchAlgorithm
{
    private final Instrumenter instrumenter;
    private final Display display;
    private final Instrumenter inst;

    public TestCasePrioritiser(
            KanonizoConfigurationModel configurationModel,
            List<TestOrderChangedListener> testOrderChangedListeners,
            Instrumenter instrumenter,
            Display display
    )
    {
        super(configurationModel, testOrderChangedListeners, instrumenter, display);
        this.instrumenter = instrumenter;
        this.display = display;
    }

    @Override
    protected final TestSuite generateSolution()
    {
        TestSuite suite = SystemUnderTest.copyOf(problem).getTestSuite();
        List<TestCase> testCases = suite.getTestCases();
        List<TestCase> orderedTestCases = new LinkedList<>();
        init(testCases);
        while (!testCases.isEmpty())
        {
            TestCase tc = selectTestCase(testCases);
            testCases.remove(tc);
            orderedTestCases.add(tc);
            fw.notifyTestCaseSelection(tc);
            display.reportProgress(orderedTestCases.size(), testCases.size() + orderedTestCases.size());
        }
        suite.setTestCases(orderedTestCases);
        notifyTestSuiteOrderingChanged(orderedTestCases);
        return suite;
    }

    public void init(List<TestCase> testCases)
    {

    }

    public abstract TestCase selectTestCase(List<TestCase> testCases);
}
