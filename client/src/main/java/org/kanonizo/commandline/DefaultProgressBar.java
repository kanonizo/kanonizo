package org.kanonizo.commandline;

import org.apache.commons.lang.StringUtils;

import java.io.PrintStream;

public class DefaultProgressBar implements ProgressBar
{
    private final PrintStream out;

    public DefaultProgressBar(PrintStream out)
    {
        this.out = out;
    }

    public DefaultProgressBar()
    {
        this(System.out);
    }

    @Override
    public void setTitle(String title)
    {
        out.println(title);
    }

    @Override
    public void reportProgress(double currentPoint, double totalPoints)
    {
        double percentageThrough = currentPoint / totalPoints * 100;
        // int repeats = title.length()
        out.print("\r|" + StringUtils.repeat("=", (int) percentageThrough)
                          + StringUtils.repeat(" ", 100 - (int) percentageThrough) + "| " + (int) percentageThrough
                          + "%");

    }

    @Override
    public void complete()
    {
        out.print("\n");
    }
}
