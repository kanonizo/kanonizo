package org.kanonizo.algorithms;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.kanonizo.algorithms.metaheuristics.fitness.FitnessFunction;
import org.kanonizo.algorithms.stoppingconditions.StoppingCondition;
import org.kanonizo.framework.Readable;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;

public interface SearchAlgorithm extends Readable
{
    void setSearchProblem(SystemUnderTest problem);

    TestSuite getCurrentOptimal();

    void setCurrentOptimal(TestSuite chr);

    Instant getStartTime();

    int getAge();

    TestSuite run();

    int getFitnessEvaluations();

    void addStoppingCondition(StoppingCondition cond);

    void removeStoppingCondition(StoppingCondition cond);

    List<StoppingCondition> getStoppingConditions();

    Duration getTotalTime();

    default boolean needsFitnessFunction()
    {
        return true;
    }

    default boolean providesFitnessFunction() { return false; }

    default FitnessFunction<SystemUnderTest> getFitnessFunction() { return null; }

    default double getFitness(TestSuite chr)
    {
        // TODO remove default implementation once search algorithms have
        // implemented their own fitness functions
        return 0.0;
    }

    default double getFitness(TestCase chr)
    {
        // TODO remove default implementation once search algorithms have
        // implemented their own fitness functiosn
        return 0.0;
    }
}
