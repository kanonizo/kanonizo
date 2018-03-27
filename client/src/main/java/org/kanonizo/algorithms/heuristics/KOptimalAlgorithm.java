package org.kanonizo.algorithms.heuristics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.TestCasePrioritiser;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.TestCase;

public class KOptimalAlgorithm extends TestCasePrioritiser {

    Set<Line> cache = new HashSet<>();
    private List<TestCase> bestK = new ArrayList<>();
    private int k;

    public KOptimalAlgorithm() {
        this.k = 2;
    }

    @Override
    public TestCase selectTestCase(List<TestCase> testCases) {
        if(bestK.size() == 0) {
            bestK = selectOptimal(testCases);
        }
        TestCase best = bestK.get(0);
        bestK.remove(0);
        cache.addAll(inst.getLinesCovered(best));
        return best;
    }

    private List<TestCase> selectOptimal(List<TestCase> testCases) {
        List<TestCase> cases = new ArrayList<TestCase>();
        double maxFitness = 0.0;
        for (int i = 0; i < testCases.size(); i++) {
            for (int j = 0; j < testCases.size(); j++) {
                if (i > j) {
                    TestCase tc1 = testCases.get(i);
                    TestCase tc2 = testCases.get(j);
                    double fitness = getFitness(tc1, tc2);
                    if (fitness > maxFitness) {
                        maxFitness = fitness;
                        cases.clear();
                        cases.add(tc1);
                        cases.add(tc2);
                    }
                }
            }
        }
        return cases;
    }

    private double getFitness(TestCase tc1, TestCase tc2) {
        int previousLines = cache.size();
        Set<Line> temp = new HashSet<>(cache);
        temp.addAll(inst.getLinesCovered(tc1));
        temp.addAll(inst.getLinesCovered(tc2));
        return temp.size() - previousLines;
    }

    @Override
    public String readableName() {
        return "koptimal";
    }
}
