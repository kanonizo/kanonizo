package org.kanonizo.reporting;

import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.framework.objects.TestSuite;

public class MiscStatsWriter extends CsvWriter
{
    private final SearchAlgorithm algorithm;

    public MiscStatsWriter(SearchAlgorithm algorithm)
    {
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
        addRow(Double.toString(optimal.getFitness()), Integer.toString(algorithm.getAge()),
               Long.toString(algorithm.getTotalTime()), Integer.toString(algorithm.getFitnessEvaluations())
        );
    }

}
