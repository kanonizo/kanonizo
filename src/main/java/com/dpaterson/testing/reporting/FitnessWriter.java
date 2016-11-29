package com.dpaterson.testing.reporting;

import java.util.Calendar;

import com.dpaterson.testing.algorithms.SearchAlgorithm;
import com.dpaterson.testing.algorithms.metaheuristics.fitness.FitnessFunction;
import com.dpaterson.testing.framework.TestSuiteChromosome;
import com.sheffield.instrumenter.InstrumentationProperties;

public class FitnessWriter extends CsvWriter {
  private SearchAlgorithm algorithm;
  private FitnessFunction<TestSuiteChromosome> func;

  public FitnessWriter(SearchAlgorithm algorithm) {
    this.algorithm = algorithm;
  }

  public FitnessWriter(FitnessFunction<TestSuiteChromosome> func) {
    this.func = func;
  }

  protected FitnessWriter() {

  }

  @Override
  public String getDir() {
    return "fitness";
  }

  @Override
  protected void prepareCsv() {
    setHeaders(new String[] { "Iteration", "Best Individual Fitness" });
  }

  public void addRow(int iteration, double fitness) {
    super.addRow(new String[] { Integer.toString(iteration), Double.toString(1 - fitness) });
  }

  @Override
  protected String getLogFileName() {
    return InstrumentationProperties.LOG_FILENAME.equals("")
        ? getSimpleName() + FORMAT.format(Calendar.getInstance().getTime()) + ".csv"
        : InstrumentationProperties.LOG_FILENAME + ".csv";
  }

  private String getSimpleName() {
    return algorithm == null ? func.getClass().getSimpleName() : algorithm.getClass().getSimpleName();
  }

}
