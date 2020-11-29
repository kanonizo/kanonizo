package org.kanonizo.algorithms;

import org.kanonizo.RandomAlgorithm;
import org.kanonizo.algorithms.faultprediction.Schwa;
import org.kanonizo.algorithms.heuristics.AdditionalFEPAlgorithm;
import org.kanonizo.algorithms.heuristics.AdditionalGreedyAlgorithm;
import org.kanonizo.algorithms.heuristics.GreedyAlgorithm;
import org.kanonizo.algorithms.heuristics.KOptimalAlgorithm;
import org.kanonizo.algorithms.heuristics.TotalFEPAlgorithm;
import org.kanonizo.algorithms.heuristics.historybased.Cho;
import org.kanonizo.algorithms.heuristics.historybased.Elbaum;
import org.kanonizo.algorithms.heuristics.historybased.Huang;
import org.kanonizo.algorithms.heuristics.historybased.Marijan;
import org.kanonizo.algorithms.metaheuristics.EpistaticGeneticAlgorithm;
import org.kanonizo.algorithms.metaheuristics.GeneticAlgorithm;
import org.kanonizo.algorithms.metaheuristics.HillClimbAlgorithm;
import org.kanonizo.algorithms.metaheuristics.HistoryBasedGeneticAlgorithm;
import org.kanonizo.algorithms.metaheuristics.HypervolumeGeneticAlgorithm;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.display.Display;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.listeners.TestOrderChangedListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum Algorithm
{
    RANDOM("random", "", RandomAlgorithm::new),
    RANDOM_SEARCH("randomsearch", "", RandomSearchAlgorithm::new),
    GREEDY("greedy", "", GreedyAlgorithm::new),
    ADDITIONAL_GREEDY("additionalgreedy", "", AdditionalGreedyAlgorithm::new),
    K_OPTIMAL("koptimal", "", KOptimalAlgorithm::new),
    TOTAL_FEP("totalfep", "", TotalFEPAlgorithm::new),
    ADDITIONAL_FEP("additionalfep", "", AdditionalFEPAlgorithm::new),
    SCHWA("schwa", "", Schwa::new),
    CHO("cho", "", Cho::new),
    HUANG("huang", "", Huang::new),
    MARIJAN("marijan", "", Marijan::new),
    ELBAUM("elbaum", "", Elbaum::new),
    GENETIC_ALGORITHM("geneticalgorithm", "", GeneticAlgorithm::new),
    HILL_CLIMB("hillclimb", "", HillClimbAlgorithm::new),
    EPISTATIC_GENETIC_ALGORITHM("epistaticga", "", EpistaticGeneticAlgorithm::new),
    HISTORY_BASED_GENETIC_ALGORITHM("historyga", "", HistoryBasedGeneticAlgorithm::new),
    HYPERVOLUME_BASED_GENETIC_ALGORITHM("hypervolumega", "", HypervolumeGeneticAlgorithm::new);

    private final AlgorithmFactory<?> factory;
    public final String description;
    public final String readableName;
    public final String commandLineSwitch;

    <T extends SearchAlgorithm> Algorithm(
            String commandLineSwitch,
            String description,
            AlgorithmFactory<T> factory
    )
    {
        this.factory = factory;
        this.description = description;
        this.commandLineSwitch = commandLineSwitch;
        String algorithmName = name();
        char firstCharacterOfAlgorithmName = algorithmName.charAt(0);
        this.readableName = firstCharacterOfAlgorithmName + algorithmName.toLowerCase().substring(1).replaceAll("_", " ");
    }

    public <T extends SearchAlgorithm> AlgorithmFactory<T> getFactory()
    {
        return (AlgorithmFactory<T>) factory;
    }

    public static Optional<Algorithm> withCommandLineSwitch(String commandLineSwitch)
    {
        return Arrays.stream(values()).filter(alg -> alg.commandLineSwitch.equalsIgnoreCase(commandLineSwitch)).findFirst();
    }

    @FunctionalInterface
    public interface AlgorithmFactory<T extends SearchAlgorithm>
    {
        T from(
                KanonizoConfigurationModel configurationModel,
                List<TestOrderChangedListener> testOrderChangedListeners,
                Instrumenter instrumenter,
                Display display
        ) throws IOException;
    }
}
