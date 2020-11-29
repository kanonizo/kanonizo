package org.kanonizo.junit;

import java.io.Serializable;

public class KanonizoTestFailure implements Serializable
{
    private final Throwable cause;
    private final String trace;

    public KanonizoTestFailure(Throwable cause, String trace)
    {
        this.cause = cause;
        this.trace = trace;
    }

    public Throwable getCause()
    {
        return cause;
    }

    public String getTrace()
    {
        return trace;
    }
}
