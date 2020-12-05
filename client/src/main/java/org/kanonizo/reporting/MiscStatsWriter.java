package org.kanonizo.reporting;

import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.framework.objects.TestSuite;

import java.nio.file.Path;

public class MiscStatsWriter extends CsvWriter
{
    private final SearchAlgorithm algorithm;

    public MiscStatsWriter(
            Path logFileDirectory,
            String logFileNamePattern,
            SearchAlgorithm algorithm
    )
    {
        super(logFileDirectory, logFileNamePattern);
        this.algorithm = algorithm;
    }

    @Override
    public String getDir()
    {
        return "statistics";
    }

    @Override
    protected void prepareCsv()
    {
        TestSuite optimal = algorithm.getCurrentOptimal();
        setHeaders("Fitness", "Iterations", "Algorithm Execution Time", "Fitness Evaluations");
        addRow(
                optimal.getFitness(),
                algorithm.getAge(),
                algorithm.getTotalTime(),
                algorithm.getFitnessEvaluations()

        );
    }

}
