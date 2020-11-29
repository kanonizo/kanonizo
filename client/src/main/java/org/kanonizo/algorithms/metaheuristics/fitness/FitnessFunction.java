package org.kanonizo.algorithms.metaheuristics.fitness;

import org.kanonizo.Disposable;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestSuite;

public interface FitnessFunction<T> extends Disposable
{
    double evaluateFitness();

    FitnessFunction<T> clone();

    T getSystem();

    default boolean isMaximisationFunction()
    {
        return false;
    }
}
