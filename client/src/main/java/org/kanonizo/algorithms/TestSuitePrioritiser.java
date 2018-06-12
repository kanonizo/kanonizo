package org.kanonizo.algorithms;

import static org.kanonizo.algorithms.stoppingconditions.TimeStoppingCondition.MAX_EXECUTION_TIME;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.stoppingconditions.FitnessStoppingCondition;
import org.kanonizo.algorithms.stoppingconditions.IterationsStoppingCondition;
import org.kanonizo.algorithms.stoppingconditions.StagnationStoppingCondition;
import org.kanonizo.algorithms.stoppingconditions.StoppingCondition;
import org.kanonizo.algorithms.stoppingconditions.TimeStoppingCondition;
import org.kanonizo.display.Display;
import org.kanonizo.framework.objects.TestSuite;

public abstract class TestSuitePrioritiser extends AbstractSearchAlgorithm {

  private static final Logger logger = LogManager.getLogger(TestSuitePrioritiser.class);
  protected List<TestSuite> population = new ArrayList<>();
  protected List<EvolutionListener> evolutionListeners = new ArrayList<>();

  public TestSuitePrioritiser(){
    addStoppingCondition(new TimeStoppingCondition());
    addStoppingCondition(new IterationsStoppingCondition());
    addStoppingCondition(new FitnessStoppingCondition());
    addStoppingCondition(new StagnationStoppingCondition());
  }

  public void addEvolutionListener(EvolutionListener e) {
    evolutionListeners.add(e);
  }

  public void removeEvolutionListener(EvolutionListener e) {
    evolutionListeners.remove(e);
  }

  public List<EvolutionListener> getEvolutionListeners() {
    return evolutionListeners;
  }

  protected void sortPopulation() {
    Collections.sort(population);
  }

  protected boolean shouldFinish() {
    for (StoppingCondition cond : stoppingConditions) {
      if (cond.shouldFinish(this)) {
        logger.info("Algorithm terminated by " + cond.getClass().getName());
        return true;
      }
    }
    return false;
  }

  @Override
  protected final void generateSolution() {
    Display d = Framework.getInstance().getDisplay();
    LocalDateTime date = LocalDateTime
        .ofInstant(Instant.ofEpochMilli(startTime), TimeZone.getDefault().toZoneId());
    DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy 'at' HH:mm:ss");
    logger.info(getClass().getSimpleName() + " started searching at : " + date.format(format));
    population = generateInitialPopulation();
    startTime = System.currentTimeMillis();
    while (!shouldFinish()) {
      age++;
      population = evolve();
      for (EvolutionListener e : evolutionListeners) {
        e.evolutionComplete();
      }
      sortPopulation();
      setCurrentOptimal(population.get(0));
      d.reportProgress(
          Math.min((double) System.currentTimeMillis() - startTime, MAX_EXECUTION_TIME),
          MAX_EXECUTION_TIME);
    }
    StoppingCondition terminatingStoppingCondition = stoppingConditions.stream()
        .filter(cond -> cond.shouldFinish(this)).findFirst().get();
    LocalDateTime enddate = LocalDateTime
        .ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()),
            TimeZone.getDefault().toZoneId());
    logger.info("Total Number of iterations: " + age + "\n");
    logger.info(getClass().getSimpleName() + " finished execution at : " + enddate.format(format));
    logger.info(
        getClass().getSimpleName() + " terminated by: " + terminatingStoppingCondition.getClass()
            .getSimpleName());

  }

  protected abstract List<TestSuite> generateInitialPopulation();

  protected abstract List<TestSuite> evolve();

  public interface EvolutionListener {

    void evolutionComplete();
  }
}
