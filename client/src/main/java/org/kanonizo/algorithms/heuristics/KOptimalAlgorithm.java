package org.kanonizo.algorithms.heuristics;

import com.scythe.instrumenter.analysis.ClassAnalyzer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.AbstractSearchAlgorithm;
import org.kanonizo.commandline.ProgressBar;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;

public class KOptimalAlgorithm extends AbstractSearchAlgorithm {

    Set<Line> cache = new HashSet<>();
    private int k;

    public KOptimalAlgorithm() {
        this.k = 2;
    }

    @Override
    public void generateSolution() {
        TestSuite suite = problem.clone().getTestSuite();
        List<TestCase> testCases = new ArrayList<TestCase>(suite.getTestCases());
        List<TestCase> newOrder = new ArrayList<TestCase>();
        ProgressBar bar = new ProgressBar(ClassAnalyzer.out);
        bar.setTitle("Performing KOptimal Algorithm");
        while (testCases.size() > k - 1) {
            age++;
            List<TestCase> bestK = selectOptimal(testCases);
            for (int i = 0; i < bestK.size(); i++) {
                TestCase testCase = bestK.get(i);
                newOrder.add(testCase);
                testCases.remove(testCase);
                cache.addAll(Framework.getInstrumenter().getLinesCovered(testCase));
            }
            bar.reportProgress(newOrder.size(), newOrder.size() + testCases.size());
        }
        bar.complete();
        newOrder.add(testCases.get(0));
        testCases.remove(0);
        suite.setTestCases(newOrder);
        setCurrentOptimal(suite);
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
        temp.addAll(Framework.getInstrumenter().getLinesCovered(tc1));
        temp.addAll(Framework.getInstrumenter().getLinesCovered(tc2));
        return temp.size() - previousLines;
    }

}
