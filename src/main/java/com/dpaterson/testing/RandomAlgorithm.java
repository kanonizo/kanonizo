package com.dpaterson.testing;

import com.dpaterson.testing.algorithms.AbstractSearchAlgorithm;
import com.dpaterson.testing.framework.TestCaseChromosome;

import java.util.Collections;
import java.util.List;

/**
 * Created by davidpaterson on 16/12/2016.
 */
public class RandomAlgorithm extends AbstractSearchAlgorithm{
    @Override
    protected void generateSolution() {
        List<TestCaseChromosome> testCases = problem.getTestCases();
        Collections.shuffle(testCases);
        problem.setTestCases(testCases);
        fitnessEvaluations++;
    }

    @Override
    public boolean needsFitnessFunction() {
        return false;
    }
}
