package org.kanonizo.algorithms;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.algorithms.stoppingconditions.MaxGenerationsStoppingCondition;
import org.kanonizo.algorithms.stoppingconditions.StagnationStoppingCondition;
import org.kanonizo.algorithms.stoppingconditions.StoppingCondition;
import org.kanonizo.algorithms.stoppingconditions.TimeStoppingCondition;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.configuration.configurableoption.BooleanOption;
import org.kanonizo.configuration.configurableoption.IntOption;
import org.kanonizo.display.Display;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Population;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.listeners.TestOrderChangedListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static org.kanonizo.configuration.configurableoption.BooleanOption.booleanOption;

public abstract class TestSuitePrioritiser extends AbstractSearchAlgorithm
{
    private static final BooleanOption USE_MAX_GENERATIONS_STOPPING_CONDITION =
            booleanOption(
                    "use_max_generations_stopping_condition",
                    true
            );
    private static final IntOption MAX_GENERATIONS = IntOption.intOption(
            "max_iterations",
            10000
    );
    private static final BooleanOption USE_MAX_TIME_STOPPING_CONDITION = booleanOption(
            "use_max_time_stopping_condition",
            true
    );
    private static final IntOption MAX_TIME = IntOption.intOption(
            "max_time",
            60000
    );
    private static final BooleanOption USE_STAGNATION_STOPPING_CONDITIONS = booleanOption(
            "use_stagnation_stopping_condition",
            false
    );
    private static final IntOption STAGNATION_PATIENCE = IntOption.intOption(
            "stagnation_patience",
            20
    );
    protected static final BooleanOption TRACK_GENERATION_FITNESS_OPTION = booleanOption("track_generation_fitness", true);

    private static final Logger logger = LogManager.getLogger(TestSuitePrioritiser.class);
    private final KanonizoConfigurationModel configurationModel;
    private final Display display;
//    private final List<EvolutionListener> evolutionListeners;
    protected Population<TestSuite> population = new Population<>();

    public TestSuitePrioritiser(
            KanonizoConfigurationModel configurationModel,
            List<TestOrderChangedListener> testOrderChangedListeners,
            Instrumenter instrumenter,
            Display display
    )
    {
        super(configurationModel, testOrderChangedListeners, instrumenter, display);
        this.configurationModel = configurationModel;
        this.display = display;
        addDefaultStoppingConditions(configurationModel);
    }

    private void addDefaultStoppingConditions(KanonizoConfigurationModel configurationModel)
    {
        if (configurationModel.getConfigurableOptionValue(USE_MAX_TIME_STOPPING_CONDITION))
        {
            addStoppingCondition(new TimeStoppingCondition(configurationModel.getConfigurableOptionValue(MAX_TIME)));
        }
        if (configurationModel.getConfigurableOptionValue(USE_MAX_GENERATIONS_STOPPING_CONDITION))
        {
            addStoppingCondition(new MaxGenerationsStoppingCondition(configurationModel.getConfigurableOptionValue(MAX_GENERATIONS)));
        }
        if (configurationModel.getConfigurableOptionValue(USE_STAGNATION_STOPPING_CONDITIONS))
        {
            addStoppingCondition(new StagnationStoppingCondition(configurationModel.getConfigurableOptionValue(STAGNATION_PATIENCE)));
        }
        addStoppingCondition(algorithm -> algorithm.getAge() > 0 && algorithm.getCurrentOptimal().getFitness() <= 0.0);
    }

    protected void sortPopulation()
    {
        Collections.sort(population);
    }

    protected boolean shouldFinish()
    {
        Optional<StoppingCondition> terminalCondition = stoppingConditions.stream().filter(cond -> cond.shouldFinish(
                this)).findFirst();
        terminalCondition.ifPresent(cond -> logger.debug("Algorithm terminated by %s", cond.getClass().getName()));
        return terminalCondition.isPresent();
    }

    @Override
    protected final TestSuite generateSolution()
    {
        LocalDateTime date = LocalDateTime
                .ofInstant(Instant.ofEpochMilli(startTime), TimeZone.getDefault().toZoneId());
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy 'at' HH:mm:ss");
        logger.info("%s started searching at : %s", getClass().getSimpleName(), date.format(format));
        population = generateInitialPopulation();
        startTime = Instant.now();
        while (!shouldFinish())
        {
            age++;
            population = evolve();
            evolutionListeners.forEach(EvolutionListener::evolutionComplete);
            sortPopulation();
            setCurrentOptimal(population.getBest());
            display.reportProgress(
                    Math.min(Instant.now().minusMillis(startTime.toEpochMilli()), configurationModel.getConfigurableOptionValue(MAX_TIME)),
                    configurationModel.getConfigurableOptionValue(MAX_TIME);
            );
        }
        StoppingCondition terminatingStoppingCondition = stoppingConditions.stream()
                .filter(cond -> cond.shouldFinish(this)).findFirst().get();
        LocalDateTime enddate = LocalDateTime
                .ofInstant(
                        Instant.now(),
                        TimeZone.getDefault().toZoneId()
                );
        logger.info("Total Number of iterations: " + age + "\n");
        logger.info(getClass().getSimpleName() + " finished execution at : " + enddate.format(format));
        logger.info(
                getClass().getSimpleName() + " terminated by: " + terminatingStoppingCondition.getClass()
                        .getSimpleName());
        return population.getBest();
    }

    protected abstract Population<TestSuite> generateInitialPopulation();

    protected abstract Population<TestSuite> evolve();

    @FunctionalInterface
    public interface EvolutionListener
    {
        void evolutionComplete();
    }
}
