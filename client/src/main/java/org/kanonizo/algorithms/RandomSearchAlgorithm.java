package org.kanonizo.algorithms;

import java.util.ArrayList;
import java.util.List;
import org.kanonizo.Framework;
import org.kanonizo.Properties;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.display.Display;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.reporting.FitnessWriter;
import org.kanonizo.util.RandomInstance;
@Algorithm(readableName = "randomsearch")
public class RandomSearchAlgorithm extends AbstractSearchAlgorithm {
    static {
        Properties.POPULATION_SIZE = 1;
    }

    private FitnessWriter writer = new FitnessWriter(this);

    @Override
    public void generateSolution() {

        List<TestCase> testCases = problem.getTestSuite().getTestCases();
        Display d = Framework.getInstance().getDisplay();
        System.out.println("Running Random Search");
        while (!shouldFinish()) {
            age++;
            TestSuite clone = getCurrentOptimal().clone();
            List<TestCase> randomOrdering = generateRandomOrder(testCases);
            clone.setTestCases(randomOrdering);
            fitnessEvaluations++;
            if (clone.fitter(getCurrentOptimal()).equals(clone)) {
                setCurrentOptimal(clone);
            }
            if (Properties.TRACK_GENERATION_FITNESS) {
                writer.addRow(age, getCurrentOptimal().getFitness());
            } else {
                writer.addRow(age, clone.getFitness());
            }
            d.reportProgress(Math.min((double) System.currentTimeMillis() - startTime, Properties.MAX_EXECUTION_TIME),
                    Properties.MAX_EXECUTION_TIME);
        }
        System.out.println();
        writer.write();
    }

    private List<TestCase> generateRandomOrder(List<TestCase> testCases) {
        List<TestCase> unorderedCases = new ArrayList<TestCase>(testCases);
        List<TestCase> orderedCases = new ArrayList<TestCase>();
        while (unorderedCases.size() > 0) {
            int index = RandomInstance.nextInt(unorderedCases.size());
            TestCase chr = unorderedCases.get(index);
            orderedCases.add(chr);
            unorderedCases.remove(chr);
        }
        return orderedCases;
    }

}
