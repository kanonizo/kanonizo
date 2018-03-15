package org.kanonizo.algorithms.metaheuristics.fitness;

import java.util.HashSet;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import org.kanonizo.Framework;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.TestCase;

public class TestSuiteFitnessFunction implements ToDoubleFunction<TestCase> {

  private Set<Line> coverableLines = new HashSet<>();
  private int totalCoverableLines;
  private Instrumenter inst = Framework.getInstance().getInstrumenter();

  public TestSuiteFitnessFunction(Set<Line> coverableLines) {
    this.coverableLines = coverableLines;
    totalCoverableLines = coverableLines.size();
  }

  @Override
  public double applyAsDouble(TestCase tc) {
    Set<Line> coveredLines = inst.getLinesCovered(tc);

    int newlyCoveredLines = coveredLines.stream().mapToInt(line -> coverableLines.contains(line) ? 0 : 1).sum();
    coverableLines.addAll(coveredLines);
    return (double) newlyCoveredLines / totalCoverableLines;
  }

}
