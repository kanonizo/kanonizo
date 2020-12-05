package org.kanonizo.algorithms.metaheuristics.crossover;

import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.util.RandomSource;

import static org.kanonizo.framework.objects.TestSuite.copyOf;

public class SinglePointCrossover implements CrossoverFunction
{

    @Override
    public void crossover(TestSuite firstParent, TestSuite secondParent)
    {
        if (firstParent.size() < 2 || secondParent.size() < 2)
        {
            return;
        }

        int crossoverPoint = RandomSource.nextInt(Math.min(firstParent.size(), secondParent.size()) - 1) + 1;

        TestSuite firstChild = copyOf(firstParent);
        TestSuite secondChild = copyOf(secondParent);

        firstParent.crossover(secondChild, crossoverPoint, crossoverPoint);
        secondParent.crossover(firstChild, crossoverPoint, crossoverPoint);
    }

}
