package org.kanonizo.reporting;

import java.util.Calendar;

import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.algorithms.metaheuristics.fitness.FitnessFunction;
import org.kanonizo.framework.objects.TestSuite;
import com.scythe.instrumenter.InstrumentationProperties;

public class FitnessWriter extends CsvWriter {
  private SearchAlgorithm algorithm;
  private FitnessFunction<TestSuite> func;

  public FitnessWriter(SearchAlgorithm algorithm) {
    this.algorithm = algorithm;
  }

  public FitnessWriter(FitnessFunction<TestSuite> func) {
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

}
