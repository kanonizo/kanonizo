package org.kanonizo.algorithms.stoppingconditions;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import org.kanonizo.algorithms.SearchAlgorithm;

public class StagnationStoppingCondition implements StoppingCondition {
    /**
     * Whether or not to use stagnation as a stopping condition for a
     * metaheuristic search algorithm. Stagnation refers to a period of time
     * where the fitness of the best solution in a population does not increase
     */
    @Parameter(key = "use_stagnation_stopping_condition", description = "Whether or not to use an stagnation stopping condition. If true, then the value of PATIENCE will be used to determine how long an algorithm can go without improving fitness before being stopped", category = "TCP")
    public static boolean USE_STAGNATION = false;

    @Parameter(key = "patience", description = "Patience refers to the amount of time an algorithm can go without improving the fitness of its best candidate solution before being considered stagnant and terminated. Usage of this property is determined by USE_STAGNATION", category = "TCP")
    public static int PATIENCE = 20;

    private int patience = PATIENCE;
    private int timeStagnant;
    private double previousFitness;

    public int getPatience() {
        return patience;
    }

    public void setPatience(int patience) {
        this.patience = patience;
    }

    @Override
    public boolean shouldFinish(SearchAlgorithm algorithm) {
        double fitness = algorithm.getCurrentOptimal().getFitness();
        if (fitness == previousFitness && ++timeStagnant == patience) {
            return true;
        } else if (fitness != previousFitness) {
            timeStagnant = 0;
            previousFitness = fitness;
        }
        return false;
    }

}
