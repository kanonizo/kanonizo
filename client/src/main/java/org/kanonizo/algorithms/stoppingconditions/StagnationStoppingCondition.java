package org.kanonizo.algorithms.stoppingconditions;

import org.kanonizo.algorithms.SearchAlgorithm;

public class StagnationStoppingCondition implements StoppingCondition
{
    private final int patience;
    private int timeStagnant;
    private double previousFitness;

    public StagnationStoppingCondition(int patience)
    {
        this.patience = patience;
    }

    public int getPatience()
    {
        return patience;
    }

    @Override
    public boolean shouldFinish(SearchAlgorithm algorithm)
    {
        double fitness = algorithm.getCurrentOptimal().getFitness();
        if (fitness == previousFitness && ++timeStagnant == patience)
        {
            return true;
        }
        else if (fitness != previousFitness)
        {
            timeStagnant = 0;
            previousFitness = fitness;
        }
        return false;
    }

}
