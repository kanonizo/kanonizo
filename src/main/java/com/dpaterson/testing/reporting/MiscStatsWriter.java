package com.dpaterson.testing.reporting;

import com.dpaterson.testing.algorithms.SearchAlgorithm;
import com.dpaterson.testing.framework.TestSuiteChromosome;

public class MiscStatsWriter extends CsvWriter {

  private SearchAlgorithm algorithm;

  public MiscStatsWriter(SearchAlgorithm algorithm) {
    this.algorithm = algorithm;
  }

  @Override
  public String getDir() {
    return "statistics";
  }

  @Override
  protected void prepareCsv() {
    TestSuiteChromosome optimal = algorithm.getCurrentOptimal();
    setHeaders(new String[] { "Fitness", "Iterations", "Algorithm Execution Time", "Fitness Evaluations" });
    addRow(new String[] { Double.toString(optimal.getFitness()), Integer.toString(algorithm.getAge()),
        Long.toString(algorithm.getTotalTime()), Integer.toString(algorithm.getFitnessEvaluations()) });
  }

}
