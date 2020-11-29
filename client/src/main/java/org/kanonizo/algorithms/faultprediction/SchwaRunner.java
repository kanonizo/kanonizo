package org.kanonizo.algorithms.faultprediction;

import java.io.File;
import java.io.IOException;

public class SchwaRunner
{
    private final File outputFile;

    public SchwaRunner(File outputFile)
    {
        this.outputFile = outputFile;
    }

    void run()
    {
        runProcess(outputFile, "schwa", )
    }

    private int runProcess(String... process)
    {
        return runProcess(null, process);
    }

    private int runProcess(File output, String... process)
    {
        ProcessBuilder pb = new ProcessBuilder(process);
        int returnCode = -1;
        try
        {
            if (output != null)
            {
                pb.redirectOutput(output);
            }
            Process processRun = pb.start();
            returnCode = processRun.waitFor();
        }
        catch (IOException | InterruptedException e)
        {
            return returnCode;
        }
        return returnCode;
    }
}
