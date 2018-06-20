package org.kanonizo.algorithms;

import static org.kanonizo.algorithms.metaheuristics.GeneticAlgorithm.TRACK_GENERATION_FITNESS;
import static org.kanonizo.algorithms.stoppingconditions.TimeStoppingCondition.MAX_EXECUTION_TIME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.kanonizo.Framework;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.display.Display;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.reporting.FitnessWriter;
import org.kanonizo.util.RandomInstance;

@Algorithm
public class RandomSearchAlgorithm extends TestSuitePrioritiser {

  private TestSuite clone;

  public RandomSearchAlgorithm(){
    FitnessWriter writer = new FitnessWriter(this);
    addEvolutionListener(new EvolutionListener() {
      @Override
      public void evolutionComplete() {
        if (TRACK_GENERATION_FITNESS) {
          writer.addRow(age, getCurrentOptimal().getFitness());
        } else {
          writer.addRow(age, clone.getFitness());
        }
      }
    });
  }

  @Override
  protected List<TestSuite> generateInitialPopulation() {
    return Collections.singletonList(problem.getTestSuite());
  }

  @Override
  protected List<TestSuite> evolve() {
    clone = getCurrentOptimal().getParent().clone().getTestSuite();
    List<TestCase> testCases = clone.getTestCases();
    List<TestCase> randomOrdering = generateRandomOrder(testCases);
    clone.setTestCases(randomOrdering);
    if (clone.fitter(getCurrentOptimal()).equals(clone)) {
      return Collections.singletonList(clone);
    } else {
      return Collections.singletonList(getCurrentOptimal());
    }
  }

  private List<TestCase> generateRandomOrder(List<TestCase> testCases) {
    List<TestCase> unorderedCases = new ArrayList<TestCase>(testCases);
    List<TestCase> orderedCases = new ArrayList<TestCase>();
    while (unorderedCases.size() > 0) {
      int index = RandomInstance.nextInt(unorderedCases.size());
      TestCase chr = unorderedCases.get(index);
      orderedCases.add(chr);
      unorderedCases.remove(chr);
    }
    return orderedCases;
  }

  @Override
  public String readableName() {
    return "randomsearch";
  }
}
