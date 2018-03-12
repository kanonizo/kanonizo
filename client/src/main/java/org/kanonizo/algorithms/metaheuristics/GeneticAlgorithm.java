package org.kanonizo.algorithms.metaheuristics;

import static org.kanonizo.algorithms.stoppingconditions.TimeStoppingCondition.MAX_EXECUTION_TIME;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
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
import org.kanonizo.Framework;
import org.kanonizo.Properties;
import org.kanonizo.algorithms.AbstractSearchAlgorithm;
import org.kanonizo.algorithms.metaheuristics.crossover.CrossoverFunction;
import org.kanonizo.algorithms.metaheuristics.crossover.SinglePointCrossover;
import org.kanonizo.algorithms.metaheuristics.selection.RankSelection;
import org.kanonizo.algorithms.metaheuristics.selection.SelectionFunction;
import org.kanonizo.algorithms.stoppingconditions.StoppingCondition;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.display.Display;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.reporting.FitnessWriter;
import org.kanonizo.util.RandomInstance;

@Algorithm(readableName = "geneticalgorithm")
public class GeneticAlgorithm extends AbstractSearchAlgorithm {
  @Parameter(key = "track_generation_fitness", description = "In the FitnessWriter it is possible to track the current fitness evaluation or the entire generation max fitness. Seeing the entire generation max fitness allows the user to see the progression of the population over time (for example in the GA), while seeing the individual fitness allows to see the spread of fitness scores across the population/evolutions. Set to true to track the whole generation fitness, set to false to see individual evaluation fitness", category = "TCP")
  public static boolean TRACK_GENERATION_FITNESS = true;

  @Parameter(key = "elite", description = "The number of individuals to automatically pass through to the next generation", hasArgs = true, category = "Genetic Algorithm")
  public static int ELITE = 1;
  //Population size for metaheuristic search
  @Parameter(key = "population_size", description = "Population size for the genetic algorithm", category = "TCP")
  public static int POPULATION_SIZE = 50;

  // Chance of a mutation occurring in a metaheuristic search
  @Parameter(key = "mutation_chance", description = "The probability during any evolution that an individual is mutated", category = "TCP")
  public static double MUTATION_CHANCE = 0.2;

  //Chance of a crossover event occurring in a metaheuristic search
  @Parameter(key = "crossover_chance", description = "The probability during any evolution that an individual is crossed over", category = "TCP")
  public static double CROSSOVER_CHANCE = 0.7;



  private List<TestSuite> population = new ArrayList<TestSuite>();
  private static Logger logger = LogManager.getLogger(GeneticAlgorithm.class);
  private SelectionFunction<TestSuite> selection = new RankSelection<>();
  private CrossoverFunction crossover = new SinglePointCrossover();
  private FitnessWriter writer = new FitnessWriter(this);

  public void setCrossoverFunction(CrossoverFunction crossover) {
    this.crossover = crossover;
  }

  public void setSelectionFunction(SelectionFunction<TestSuite> function) {
    this.selection = function;
  }

  @Override
  public void generateSolution() {
    LocalDateTime date = LocalDateTime
        .ofInstant(Instant.ofEpochMilli(startTime), TimeZone.getDefault().toZoneId());
    DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy 'at' HH:mm:ss");
    logger.info("Genetic Algorithm started searching at : " + date.format(format));
    generateInitialPopulation();
    startTime = System.currentTimeMillis();
    logger.info(
        "Genetic Algorithm running for: " + MAX_EXECUTION_TIME / 1000 + " seconds");
    setCurrentOptimal(population.get(0));
    Display d = Framework.getInstance().getDisplay();
    System.out.println("Genetic Algorithm");
    while (!shouldFinish()) {
      age++;
      evolve();
      sortPopulation();
      setCurrentOptimal(population.get(0));
      if (TRACK_GENERATION_FITNESS) {
        writer.addRow(age, getCurrentOptimal().getFitness());
      }
      d.reportProgress(
          Math.min((double) System.currentTimeMillis() - startTime, MAX_EXECUTION_TIME),
          MAX_EXECUTION_TIME);
    }
    writer.write();
    System.out.println();
    StoppingCondition terminatingStoppingCondition = stoppingConditions.stream()
        .filter(cond -> cond.shouldFinish(this)).findFirst().get();
    LocalDateTime enddate = LocalDateTime
        .ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()),
            TimeZone.getDefault().toZoneId());
    logger.info("Total Number of iterations: " + age + "\n");
    logger.info("Genetic Algorithm finished execution at : " + enddate.format(format));
    logger.info("Genetic Algorithm terminated by: " + terminatingStoppingCondition.getClass()
        .getSimpleName());

  }

  protected void generateInitialPopulation() {
    logger.info("Generating initial population");
    for (int i = 0; i < POPULATION_SIZE; i++) {
      TestSuite clone = problem.clone().getTestSuite();
      List<Integer> testCaseOrdering = IntStream.range(0, clone.getTestCases().size())
          .collect(ArrayList::new,
              ArrayList::add, ArrayList::addAll);
      List<TestCase> randomOrdering = new ArrayList<TestCase>();
      while (!testCaseOrdering.isEmpty()) {
        int index = RandomInstance.nextInt(testCaseOrdering.size());
        TestCase tc = clone.getTestCases().get(testCaseOrdering.get(index));
        randomOrdering.add(tc);
        testCaseOrdering.remove(index);
      }
      clone.setTestCases(randomOrdering);
      population.add(clone);
    }
  }

  protected void evolve() {
    long startTime = System.currentTimeMillis();
    List<TestSuite> newIndividuals = new ArrayList<>();
    // apply elitism
    newIndividuals.addAll(elitism());

    while (!isNewGenerationFull(newIndividuals)) {

      TestSuite parent1 = selection.select(population);
      TestSuite parent2 = selection.select(population);

      TestSuite offspring1 = parent1.clone();
      TestSuite offspring2 = parent2.clone();

      if (RandomInstance.nextDouble() <= CROSSOVER_CHANCE) {
        crossover.crossover(offspring1, offspring2);
      }

      if (RandomInstance.nextDouble() <= MUTATION_CHANCE) {
        offspring1 = offspring1.mutate();
        offspring2 = offspring2.mutate();
      }
      evolutionComplete(offspring1, offspring2);
      newIndividuals.addAll(getNFittest(2, parent1, parent2, offspring1, offspring2));
    }
    population = newIndividuals;
    if (Properties.PROFILE) {
      System.out
          .println("Evolution completed in: " + (System.currentTimeMillis() - startTime) + "ms");
      System.out.println("Fittest individual has fitness: " + population.get(0).getFitness());
    }
  }

  protected void evolutionComplete(TestSuite... evolved) {
    for (TestSuite ts : evolved) {
      ts.evolutionComplete();
      fitnessEvaluations++;
      if (!TRACK_GENERATION_FITNESS) {
        writer.addRow(fitnessEvaluations, ts.getFitness());
      }
    }
  }

  protected void sortPopulation() {
    Collections.sort(population);
  }

  @Override
  public TestSuite getCurrentOptimal() {
    return population.get(0);
  }

  protected boolean isNewGenerationFull(List<TestSuite> newGeneration) {
    return newGeneration.size() > POPULATION_SIZE - 1;
  }

  /**
   * Elitism in Genetic Algorithms is the automatic addition of the fittest individuals into the
   * next generation. It guarantees a certain number of individuals will not be subject to mutation
   * or crossover. The number of elite individuals is determined by the ELITE property.
   *
   * @return a list of elite individuals to automatically add into the next generation
   */
  protected List<TestSuite> elitism() {
    sortPopulation();
    List<TestSuite> elite = new ArrayList<>();
    for (int i = 0; i < ELITE; i++) {
      elite.add(population.get(i).clone());
    }
    return elite;
  }

  protected List<TestSuite> getNFittest(int n, TestSuite... elements) {
    List<TestSuite> candidates = Arrays.asList(elements);
    Collections.sort(candidates);
    return candidates.subList(0, n);
  }
}
