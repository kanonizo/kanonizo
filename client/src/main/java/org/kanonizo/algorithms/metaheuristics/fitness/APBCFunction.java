package org.kanonizo.algorithms.metaheuristics.fitness;

import java.util.Set;

import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Goal;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestCaseContainer;
import org.kanonizo.util.HashSetCollector;

public class APBCFunction <T extends TestCaseContainer> extends APFDFunction<T>
{
    private final Instrumenter instrumenter;

    public APBCFunction(Instrumenter instrumenter, T sut)
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
        return sut.getClassesUnderTest().stream().map(instrumenter::getBranches).collect(new HashSetCollector<>());
    }

    @Override
    public FitnessFunction<T> clone()
    {
        return new APBCFunction<>(instrumenter, sut);
    }

    @Override
    protected double calculateTotalGoalsCovered()
    {
        return instrumenter.getBranchesCovered(sut.getClassesUnderTest()).size();
    }

}
