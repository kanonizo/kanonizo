package org.kanonizo.algorithms.stoppingconditions;

import org.kanonizo.Properties;
import org.kanonizo.algorithms.SearchAlgorithm;

public class StagnationStoppingCondition implements StoppingCondition {

    private int patience = Properties.PATIENCE;
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
