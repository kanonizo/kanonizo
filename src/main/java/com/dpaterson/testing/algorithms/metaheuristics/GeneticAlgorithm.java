package com.dpaterson.testing.algorithms.metaheuristics;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dpaterson.testing.Properties;
import com.dpaterson.testing.algorithms.AbstractSearchAlgorithm;
import com.dpaterson.testing.algorithms.metaheuristics.crossover.CrossoverFunction;
import com.dpaterson.testing.algorithms.metaheuristics.crossover.SinglePointCrossover;
import com.dpaterson.testing.algorithms.metaheuristics.selection.RankSelection;
import com.dpaterson.testing.algorithms.metaheuristics.selection.SelectionFunction;
import com.dpaterson.testing.commandline.ProgressBar;
import com.dpaterson.testing.framework.TestCaseChromosome;
import com.dpaterson.testing.framework.TestSuiteChromosome;
import com.dpaterson.testing.reporting.FitnessWriter;
import com.dpaterson.testing.util.RandomInstance;

public class GeneticAlgorithm extends AbstractSearchAlgorithm {

  private List<TestSuiteChromosome> population = new ArrayList<TestSuiteChromosome>();
  private static Logger logger = LogManager.getLogger(GeneticAlgorithm.class);
  private SelectionFunction<TestSuiteChromosome> selection = new RankSelection<>();
  private CrossoverFunction crossover = new SinglePointCrossover();
  private FitnessWriter writer = new FitnessWriter(this);

  public void setCrossoverFunction(CrossoverFunction crossover) {
    this.crossover = crossover;
  }

  public void setSelectionFunction(SelectionFunction<TestSuiteChromosome> function) {
    this.selection = function;
  }

  @Override
  public void generateSolution() {
    LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), TimeZone.getDefault().toZoneId());
    DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy 'at' HH:mm:ss");
    logger.info("Genetic Algorithm started searching at : " + date.format(format));
    generateInitialPopulation();
    startTime = System.currentTimeMillis();
    logger.info("Genetic Algorithm running for: " + Properties.MAX_EXECUTION_TIME / 1000 + " seconds");
    setCurrentOptimal(population.get(0));
    ProgressBar bar = new ProgressBar();
    bar.setTitle("Genetic Algorithm");
    while (!shouldFinish()) {
      age++;
      evolve();
      sortPopulation();
      setCurrentOptimal(population.get(0));
      if (Properties.TRACK_GENERATION_FITNESS) {
        writer.addRow(age, getCurrentOptimal().getFitness());
      }
      bar.reportProgress(Math.min((double) System.currentTimeMillis() - startTime, Properties.MAX_EXECUTION_TIME),
          Properties.MAX_EXECUTION_TIME);
    }
    writer.write();
    bar.complete();
    LocalDateTime enddate = LocalDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()),
        TimeZone.getDefault().toZoneId());
    logger.info("Total Number of iterations: " + age + "\n");
    logger.info("Genetic Algorithm finished execution at : " + enddate.format(format));

  }

  protected void generateInitialPopulation() {
    logger.info("Generating initial population");
    for (int i = 0; i < Properties.POPULATION_SIZE; i++) {
      TestSuiteChromosome clone = problem.clone();
      List<Integer> testCaseOrdering = IntStream.range(0, clone.getTestCases().size()).collect(ArrayList::new,
          ArrayList::add, ArrayList::addAll);
      List<TestCaseChromosome> randomOrdering = new ArrayList<TestCaseChromosome>();
      while (!testCaseOrdering.isEmpty()) {
        int index = RandomInstance.nextInt(testCaseOrdering.size());
        TestCaseChromosome tc = clone.getTestCases().get(testCaseOrdering.get(index));
        randomOrdering.add(tc);
        testCaseOrdering.remove(index);
      }
      clone.setTestCases(randomOrdering);
      population.add(clone);
    }
  }

  protected void evolve() {
    long startTime = System.currentTimeMillis();
    List<TestSuiteChromosome> newIndividuals = new ArrayList<>();
    // apply elitism
    newIndividuals.addAll(elitism());

    while (!isNewGenerationFull(newIndividuals)) {

      TestSuiteChromosome parent1 = selection.select(population);
      TestSuiteChromosome parent2 = selection.select(population);

      TestSuiteChromosome offspring1 = parent1.clone();
      TestSuiteChromosome offspring2 = parent2.clone();

      if (RandomInstance.nextDouble() <= Properties.CROSSOVER_CHANCE) {
        crossover.crossover(offspring1, offspring2);
      }

      if (RandomInstance.nextDouble() <= Properties.MUTATION_CHANCE) {
        offspring1 = offspring1.mutate();
        offspring2 = offspring2.mutate();
      }
      evolutionComplete(offspring1, offspring2);
      newIndividuals.addAll(getNFittest(2, parent1, parent2, offspring1, offspring2));
    }
    population = newIndividuals;
    if (Properties.PROFILE) {
      System.out.println("Evolution completed in: " + (System.currentTimeMillis() - startTime) + "ms");
      System.out.println("Fittest individual has fitness: " + population.get(0).getFitness());
    }
  }

  protected void evolutionComplete(TestSuiteChromosome... evolved) {
    for (TestSuiteChromosome ts : evolved) {
      ts.evolutionComplete();
      fitnessEvaluations++;
      if (!Properties.TRACK_GENERATION_FITNESS) {
        writer.addRow(fitnessEvaluations, ts.getFitness());
      }
    }
  }

  protected void sortPopulation() {
    Collections.sort(population);
  }

  @Override
  public TestSuiteChromosome getCurrentOptimal() {
    return population.get(0);
  }

  protected boolean isNewGenerationFull(List<TestSuiteChromosome> newGeneration) {
    return newGeneration.size() > Properties.POPULATION_SIZE - 1;
  }

  /**
   * Elitism in Genetic Algorithms is the automatic addition of the fittest individuals into the next generation. It guarantees a certain number of individuals will not be subject to mutation or
   * crossover. The number of elite individuals is determined by {@link Properties.ELITE}.
   * 
   * @return a list of elite individuals to automatically add into the next generation
   */
  protected List<TestSuiteChromosome> elitism() {
    sortPopulation();
    List<TestSuiteChromosome> elite = new ArrayList<>();
    for (int i = 0; i < Properties.ELITE; i++) {
      elite.add(population.get(i).clone());
    }
    return elite;
  }

  protected List<TestSuiteChromosome> getNFittest(int n, TestSuiteChromosome... elements) {
    List<TestSuiteChromosome> candidates = Arrays.asList(elements);
    Collections.sort(candidates);
    return candidates.subList(0, n);
  }
}
