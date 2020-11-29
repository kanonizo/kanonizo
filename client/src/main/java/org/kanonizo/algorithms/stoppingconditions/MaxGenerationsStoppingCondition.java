package org.kanonizo.algorithms.stoppingconditions;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import org.kanonizo.algorithms.SearchAlgorithm;

public class MaxGenerationsStoppingCondition implements StoppingCondition
{
	private final int maxGenerations;

    public MaxGenerationsStoppingCondition(int maxGenerations)
    {
        this.maxGenerations = maxGenerations;
    }

    @Override
    public boolean shouldFinish(SearchAlgorithm algorithm)
    {
        return algorithm.getAge() >= maxGenerations;
    }

}
