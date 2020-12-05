package org.kanonizo.reporting;

import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.algorithms.TestCasePrioritiser;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.junit.KanonizoTestFailure;
import org.kanonizo.listeners.TestCaseSelectionListener;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class TestCaseOrderingWriter extends CsvWriter implements TestCaseSelectionListener
{
    private final SearchAlgorithm algorithm;
    private final Instrumenter instrumenter;
    private final boolean willWriteMultipleTimes;

    public TestCaseOrderingWriter(
            Path logFileDirectory,
            String logFileNamePattern,
            SearchAlgorithm algorithm,
            Instrumenter instrumenter
    )
    {
        super(logFileDirectory, logFileNamePattern);
        this.algorithm = algorithm;
        this.instrumenter = instrumenter;

        willWriteMultipleTimes = !(algorithm instanceof TestCasePrioritiser);
        setHeaders(
                "TestCase", "ExecutionTime", "Passed", "Failures", "TotalLinesCovered"
        );
    }

    @Override
    protected void prepareCsv()
    {
        if (willWriteMultipleTimes)
        {
            TestSuite optimal = algorithm.getCurrentOptimal();

            optimal.getTestCases().forEach(testCase -> addRow(
                    testCase.toString(),
                    Long.toString(testCase.getExecutionTime()),
                    Boolean.toString(!testCase.hasFailures()),
                    stackTraceToString(testCase.getFailures()),
                    Integer.toString(instrumenter.getLinesCovered(testCase).size())
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
                Integer.toString(instrumenter.getLinesCovered(tc).size())
        );
    }

    private String stackTraceToString(List<KanonizoTestFailure> failures)
    {
        return failures.stream().map(f -> normalizeStackTrace(f.getTrace())).collect(Collectors.joining(":"));
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
