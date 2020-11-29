package org.kanonizo.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public final class NullPrintStream extends PrintStream
{
    public static NullPrintStream instance = new NullPrintStream();

    public NullPrintStream()
    {
        super(new OutputStream()
        {

            @Override
            public void write(int b) throws IOException
            {
                // do nothing, we don't want execution code from the program
                // runtime being dumped into console
            }

        });
    }

}
