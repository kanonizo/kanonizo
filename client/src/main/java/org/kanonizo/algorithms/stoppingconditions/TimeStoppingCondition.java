package org.kanonizo.algorithms.stoppingconditions;

import org.kanonizo.algorithms.SearchAlgorithm;

import java.time.Duration;
import java.time.Instant;

public class TimeStoppingCondition implements StoppingCondition
{
    private final Duration maxExecutionTime;
    public TimeStoppingCondition(int maxTimeAllowed)
    {
        this.maxExecutionTime = Duration.ofMillis(maxTimeAllowed);
    }

    @Override
    public boolean shouldFinish(SearchAlgorithm algorithm)
    {
        return algorithm.getStartTime().plus(maxExecutionTime).isAfter(Instant.now());
    }

}
