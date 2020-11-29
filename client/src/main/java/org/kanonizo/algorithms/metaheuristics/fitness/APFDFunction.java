package org.kanonizo.algorithms.metaheuristics.fitness;

import org.kanonizo.framework.objects.Goal;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestCaseContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class APFDFunction<T extends TestCaseContainer> implements FitnessFunction<T>
{
    protected final T sut;
    protected final Set<? extends Goal> totalGoals;
    protected final Double coveredGoals;

    public APFDFunction(T sut)
    {
        this.sut = sut;
        coveredGoals = calculateTotalGoalsCovered();
        totalGoals = getGoals();
    }

    protected abstract double calculateTotalGoalsCovered();

    protected Map<Goal, Integer> getGoalMap()
    {
        Map<Goal, Integer> goalMap = new HashMap<>();
        List<TestCase> testCases = sut.getTestCases();
        for (int i = 0; i < testCases.size(); i++)
        {
            final int ind = i;
            TestCase tc = testCases.get(i);
            Set<? extends Goal> goalsCovered = getCoveredGoals(tc);
            goalsCovered.forEach(goal ->
                                 {
                                     if (!goalMap.containsKey(goal))
                                     {
                                         goalMap.put(goal, ind + 1);
                                     }
                                 });
        }
        return goalMap;

    }

    protected double calculateTestCaseIndices()
    {
        Map<Goal, Integer> goalMap = getGoalMap();
        return goalMap.values().stream().mapToDouble(Integer::intValue).sum();
    }

    @Override
    public double evaluateFitness()
    {
        double apfd = calculateTestCaseIndices();
        double percentageOfGoalsCoveredByTestSuite = getPercentageOfGoalsCoveredByTestSuite();
        double totalTestCases = sut.getTestCases().size();
        double totalCoverableGoals = Math.max(totalGoals.size(), 1);
        return 1 - (percentageOfGoalsCoveredByTestSuite - apfd / (totalCoverableGoals * totalTestCases) + percentageOfGoalsCoveredByTestSuite / (2 * totalTestCases));
    }

    protected double getPercentageOfGoalsCoveredByTestSuite()
    {
        return totalGoals.size() == 0 ? 0 : coveredGoals / totalGoals.size();
    }

    public abstract Set<? extends Goal> getCoveredGoals(TestCase tc);

    protected abstract Set<? extends Goal> getGoals();

    @Override
    public void dispose()
    {
        totalGoals.clear();
    }

    @Override
    public T getSystem()
    {
        return sut;
    }

}
