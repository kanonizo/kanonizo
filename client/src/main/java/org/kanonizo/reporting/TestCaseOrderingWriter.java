package org.kanonizo.reporting;

import java.util.List;
import java.util.Optional;

import org.kanonizo.Framework;
import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.algorithms.TestCasePrioritiser;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.junit.KanonizoTestFailure;
import org.kanonizo.listeners.TestCaseSelectionListener;

public class TestCaseOrderingWriter extends CsvWriter implements TestCaseSelectionListener
{
    private SearchAlgorithm algorithm;
    private Instrumenter inst;
    private boolean finalWrite;

    protected TestCaseOrderingWriter()
    {
    }

    public TestCaseOrderingWriter(SearchAlgorithm algorithm)
    {
        super();
        this.algorithm = algorithm;

        finalWrite = !(algorithm instanceof TestCasePrioritiser);
        inst = Framework.getInstance().getInstrumenter();
        Framework.getInstance().addSelectionListener(this);
        setHeaders(
                new String[]{"TestCase", "ExecutionTime", "Passed", "Failures", "TotalLinesCovered"});
    }

    @Override
    protected void prepareCsv()
    {
        if (finalWrite)
        {
            TestSuite optimal = algorithm.getCurrentOptimal();

            optimal.getTestCases().forEach(testCase -> addRow(
                    testCase.toString(),
                    Long.toString(testCase.getExecutionTime()),
                    Boolean.toString(!testCase.hasFailures()),
                    stackTraceToString(testCase.getFailures()),
                    Integer.toString(inst.getLinesCovered(testCase).size())
            ));
        }
    }

    @Override
    public String getDir()
    {
        return "ordering";
    }

    @Override
    public void testCaseSelected(TestCase tc)
    {
        writeRow(
                tc.toString(),
                Long.toString(tc.getExecutionTime()),
                Boolean.toString(!tc.hasFailures()),
                stackTraceToString(tc.getFailures()),
                Integer.toString(inst.getLinesCovered(tc).size())
        );
    }

    private String stackTraceToString(List<KanonizoTestFailure> failures)
    {
        Optional<String> failString = failures.stream().map(f -> normalizeStackTrace(f.getTrace())).reduce((a, b) -> a + ":" + b);
        return failString.isPresent() ? failString.get() : "";
    }

    public static String normalizeStackTrace(String stackTrace)
    {
        // Given a multi-line stack trace, crushes it down to one line
        // and removes potentially-variable formatting.
        // Ideally, any two stack traces which represent "the same error"
        // should be normalized to identical strings.
        return stackTrace.replaceAll(" *\r?\n[ \t]*", " ") // kill newlines and surrounding space
                .replaceAll("^[ \t\r\n]*|[ \t\r\n]*$", ""); // strip whitespace
    }
}
