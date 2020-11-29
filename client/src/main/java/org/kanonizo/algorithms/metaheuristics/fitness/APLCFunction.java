package org.kanonizo.algorithms.metaheuristics.fitness;

import org.kanonizo.Framework;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Goal;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestCaseContainer;
import org.kanonizo.util.HashSetCollector;

import java.util.Set;

public class APLCFunction<T extends TestCaseContainer> extends APFDFunction<T>
{
    private final Instrumenter instrumenter;

    public APLCFunction(Instrumenter instrumenter, T sut)
    {
        super(sut);
        this.instrumenter = instrumenter;
    }

    @Override
    public Set<? extends Goal> getCoveredGoals(TestCase tc)
    {
        return instrumenter.getLinesCovered(tc);
    }

    @Override
    protected Set<? extends Goal> getGoals()
    {
        return sut.getClassesUnderTest().stream().map(instrumenter::getLines).collect(new HashSetCollector<>());
    }

    @Override
    protected double calculateTotalGoalsCovered()
    {
        return instrumenter.getLinesCovered(sut.getClassesUnderTest()).size();
    }

    @Override
    public FitnessFunction<T> clone()
    {
        return new APLCFunction<>(instrumenter, sut);
    }
}