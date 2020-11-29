package org.kanonizo.junit;

import java.lang.reflect.Method;
import java.util.List;

public class KanonizoTestResult
{
    private final List<KanonizoTestFailure> failures;
    private final boolean successful;
    private final String testClassName;
    private final String testMethodName;
    private final long executionTime;

    public KanonizoTestResult(
            String testClassName,
            String testMethodName,
            boolean successful,
            List<KanonizoTestFailure> failures,
            long executionTime
    )
    {
        this.testClassName = testClassName;
        this.testMethodName = testMethodName;
        this.successful = successful;
        this.failures = failures;
        this.executionTime = executionTime;
    }

    public List<KanonizoTestFailure> getFailures()
    {
        return failures;
    }

    public boolean wasSuccessful()
    {
        return successful;
    }

    public String getTestClassName()
    {
        return testClassName;
    }

    public String getTestMethodName()
    {
        return testMethodName;
    }

    public long getExecutionTime()
    {
        return executionTime;
    }

}
