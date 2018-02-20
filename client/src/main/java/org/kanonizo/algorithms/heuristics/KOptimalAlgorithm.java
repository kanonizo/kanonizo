package org.kanonizo.algorithms.heuristics;

import com.scythe.instrumenter.analysis.ClassAnalyzer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.AbstractSearchAlgorithm;
import org.kanonizo.commandline.ProgressBar;
import org.kanonizo.framework.CUTChromosome;
import org.kanonizo.framework.TestCaseChromosome;

public class KOptimalAlgorithm extends AbstractSearchAlgorithm {

    Map<CUTChromosome, Set<Integer>> cache = new HashMap<CUTChromosome, Set<Integer>>();
    private int k;

    public KOptimalAlgorithm() {
        this.k = 2;
    }

    @Override
    public void generateSolution() {
        List<TestCaseChromosome> testCases = new ArrayList<TestCaseChromosome>(problem.getTestCases());
        List<TestCaseChromosome> newOrder = new ArrayList<TestCaseChromosome>();
        ProgressBar bar = new ProgressBar(ClassAnalyzer.out);
        bar.setTitle("Performing KOptimal Algorithm");
        problem.getSUT().getClassesUnderTest().forEach(cut -> cache.put(cut, new HashSet<Integer>()));
        while (testCases.size() > k - 1) {
            age++;
            List<TestCaseChromosome> bestK = selectOptimal(testCases);
            for (int i = 0; i < bestK.size(); i++) {
                TestCaseChromosome testCase = bestK.get(i);
                newOrder.add(testCase);
                testCases.remove(testCase);
                Framework.getInstrumenter().getLinesCovered(testCase).forEach((cut, lines) -> {
                    cache.get(cut)
                            .addAll(lines);
                });
            }
            bar.reportProgress(newOrder.size(), newOrder.size() + testCases.size());
        }
        bar.complete();
        newOrder.add(testCases.get(0));
        testCases.remove(0);

        problem.setTestCases(newOrder);
    }

    private List<TestCaseChromosome> selectOptimal(List<TestCaseChromosome> testCases) {
        List<TestCaseChromosome> cases = new ArrayList<TestCaseChromosome>();
        double maxFitness = 0.0;
        for (int i = 0; i < testCases.size(); i++) {
            for (int j = 0; j < testCases.size(); j++) {
                if (i > j) {
                    TestCaseChromosome tc1 = testCases.get(i);
                    TestCaseChromosome tc2 = testCases.get(j);
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

    private double getFitness(TestCaseChromosome tc1, TestCaseChromosome tc2) {
        Map<CUTChromosome, Set<Integer>> linesCovered = Framework.getInstrumenter().getLinesCovered(tc1);
        Map<CUTChromosome, Set<Integer>> tempCache = new HashMap<CUTChromosome, Set<Integer>>(cache);
        double fitness = getFitness(linesCovered, tempCache);
        linesCovered.forEach((cut, covered) -> {
            tempCache.get(cut).addAll(covered);

        });
        double fitness2 = getFitness(Framework.getInstrumenter().getLinesCovered(tc2), tempCache);
        // favour earlier detection
        // TODO investigate whether or not the case I'm investigating is such an edge case that this isn't necessary.
        // In my (very basic) situation, this returns sub-optimal test case ordering because the 2 tests achieve 100%
        // line coverage between them.
        return (fitness * 2) + fitness2;
    }

    private double getFitness(Map<CUTChromosome, Set<Integer>> linesCovered, Map<CUTChromosome, Set<Integer>> tempCache) {
        return linesCovered.entrySet().stream().mapToDouble(entry -> {
            CUTChromosome cut = entry.getKey();
            Set<Integer> covered = entry.getValue();
            return covered.stream().mapToDouble(line -> tempCache.get(cut).contains(line) ? 0 : 1)
                    .sum();
        }).sum();
    }

}
