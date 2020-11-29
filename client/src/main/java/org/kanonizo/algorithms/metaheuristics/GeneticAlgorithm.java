package org.kanonizo.algorithms.metaheuristics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.Properties;
import org.kanonizo.algorithms.TestSuitePrioritiser;
import org.kanonizo.algorithms.metaheuristics.crossover.CrossoverFunction;
import org.kanonizo.algorithms.metaheuristics.crossover.SinglePointCrossover;
import org.kanonizo.algorithms.metaheuristics.selection.RankSelection;
import org.kanonizo.algorithms.metaheuristics.selection.SelectionFunction;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.configuration.configurableoption.ConfigurableOption;
import org.kanonizo.display.Display;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Population;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.listeners.TestOrderChangedListener;
import org.kanonizo.reporting.FitnessWriter;
import org.kanonizo.util.RandomSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.kanonizo.configuration.configurableoption.ConfigurableOption.configurableOptionFrom;

public class GeneticAlgorithm extends TestSuitePrioritiser
{
    private static final ConfigurableOption<Integer> ELITE_INDIVIDUALS_OPTION = configurableOptionFrom("elite", Integer.class, 1);
    private static final ConfigurableOption<Integer> POPULATION_SIZE_OPTION = configurableOptionFrom("elite", Integer.class, 1);
    private static final ConfigurableOption<Double> MUTATION_CHANCE_OPTION = configurableOptionFrom("mutation_chance", Double.class, 0.2);
    private static final ConfigurableOption<Double> CROSSOVER_CHANCE_OPTION = configurableOptionFrom("crossover_chance", Double.class, 0.7);

    private static final Logger logger = LogManager.getLogger(GeneticAlgorithm.class);
    private final boolean trackGenerationFitness;
    private final int eliteIndividualsInPopulation;
    private final int populationSize;
    private final double mutationChance;
    private final double crossoverChance;
    private final Display display;
    private final FitnessWriter writer;

    private final SelectionFunction<TestSuite> selection = new RankSelection<>();
    private final CrossoverFunction crossover = new SinglePointCrossover();

    public GeneticAlgorithm(
            KanonizoConfigurationModel configurationModel,
            List<TestOrderChangedListener> testOrderChangedListeners,
            Instrumenter instrumenter,
            Display display
    )
    {
        super(
                configurationModel,
                testOrderChangedListeners,
                instrumenter,
                display
        );
        this.trackGenerationFitness = configurationModel.getConfigurableOptionValue(TRACK_GENERATION_FITNESS_OPTION);
        this.eliteIndividualsInPopulation = configurationModel.getConfigurableOptionValue(ELITE_INDIVIDUALS_OPTION);
        this.populationSize = configurationModel.getConfigurableOptionValue(POPULATION_SIZE_OPTION);
        this.mutationChance = configurationModel.getConfigurableOptionValue(MUTATION_CHANCE_OPTION);
        this.crossoverChance = configurationModel.getConfigurableOptionValue(CROSSOVER_CHANCE_OPTION);
        this.display = display;
        writer = new FitnessWriter(this::getAge, () -> getCurrentOptimal().getFitness());
    }

    protected Population<TestSuite> generateInitialPopulation()
    {
        logger.info("Generating initial population");
        Population<TestSuite> pop = new Population<>();
        for (int i = 0; i < populationSize; i++)
        {
            TestSuite clone = problem.clone().getTestSuite();
            List<Integer> testCaseOrdering = IntStream.range(0, clone.getTestCases().size())
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            List<TestCase> randomOrdering = new ArrayList<>();
            while (!testCaseOrdering.isEmpty())
            {
                int index = RandomSource.nextInt(testCaseOrdering.size());
                TestCase tc = clone.getTestCases().get(testCaseOrdering.get(index));
                randomOrdering.add(tc);
                testCaseOrdering.remove(index);
            }
            clone.setTestCases(randomOrdering);
            pop.add(clone);
        }
        return pop;
    }

    protected Population<TestSuite> evolve()
    {
        long startTime = System.currentTimeMillis();
        // apply elitism
        Population<TestSuite> newIndividuals = new Population<>(elitism());

        while (!isNewGenerationFull(newIndividuals))
        {

            TestSuite parent1 = selection.select(population);
            TestSuite parent2 = selection.select(population);

            TestSuite offspring1 = parent1.getParent().clone().getTestSuite();
            TestSuite offspring2 = parent2.getParent().clone().getTestSuite();

            if (RandomSource.nextDouble() <= crossoverChance)
            {
                crossover.crossover(offspring1, offspring2);
            }

            if (RandomSource.nextDouble() <= mutationChance)
            {
                offspring1 = offspring1.mutate();
                offspring2 = offspring2.mutate();
            }
            evolutionComplete(offspring1, offspring2);
            newIndividuals.addAll(getNFittest(2, parent1, parent2, offspring1, offspring2));
        }

        if (Properties.PROFILE)
        {
            System.out
                    .println("Evolution completed in: " + (System.currentTimeMillis() - startTime) + "ms");
            System.out.println("Fittest individual has fitness: " + population.getBest().getFitness());
        }
        return newIndividuals;
    }

    protected void evolutionComplete(TestSuite... evolved)
    {
        for (TestSuite ts : evolved)
        {
            ts.evolutionComplete();
            fitnessEvaluations++;
            if (!trackGenerationFitness)
            {
                writer.addRow(fitnessEvaluations, ts.getFitness());
            }
        }
    }


    @Override
    public TestSuite getCurrentOptimal()
    {
        return population.get(0);
    }

    protected boolean isNewGenerationFull(List<TestSuite> newGeneration)
    {
        return newGeneration.size() > populationSize - 1;
    }

    protected List<TestSuite> elitism()
    {
        sortPopulation();
        List<TestSuite> elite = new ArrayList<>();
        for (int i = 0; i < eliteIndividualsInPopulation; i++)
        {
            elite.add(population.get(i).getParent().clone().getTestSuite());
        }
        return elite;
    }

    protected List<TestSuite> getNFittest(int n, TestSuite... elements)
    {
        List<TestSuite> candidates = Arrays.asList(elements);
        Collections.sort(candidates);
        return candidates.subList(0, n);
    }

    @Override
    public String readableName()
    {
        return "geneticalgorithm";
    }
}
