package org.kanonizo.algorithms.heuristics.comparators;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.kanonizo.Framework;
import org.kanonizo.framework.ClassStore;
import org.kanonizo.framework.ObjectiveFunction;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.Pair;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.similarity.DistanceFunction;
import org.kanonizo.framework.similarity.JaccardDistance;
import org.kanonizo.util.HashSetCollector;
import org.kanonizo.util.RandomSource;

public class DissimilarityComparator implements ObjectiveFunction
{

    @Parameter(key = "similarity_min_test_cases", description = "When comparing similarity of test cases, use this parameter to select a minimum number of test cases that must be chosen regardless of similarity", category = "similarity")
    public static int minTestCases = -1;
    @Parameter(key = "similarity_coverage_adequacy", description = "When comparing similarity of test cases, use this parameter to specify a minimum percentage of the target class that must be covered", category = "similarity")
    public static int coverageAdequacy = -1;
    @Parameter(key = "similarity_distance_function", description = "Distance function to use when comparing test cases", category = "similarity")
    public static DistanceFunction<TestCase> dist = new JaccardDistance();

    private List<Class<?>> targetClasses;
    private Instrumenter inst = Framework.getInstance().getInstrumenter();

    public DissimilarityComparator()
    {
        Framework.getInstance().addPropertyChangeListener(
                Framework.INSTRUMENTER_PROPERTY_NAME,
                evt -> inst = (Instrumenter) evt.getNewValue()
        );
    }

    @Override
    public List<TestCase> sort(List<TestCase> candidates)
    {
        // calculate similarity matrix
        if (candidates.size() <= 1)
        {
            return candidates;
        }
        List<TestCase> copy = new ArrayList<>(candidates);
        Map<Pair<TestCase>, Double> similarity = new HashMap<>();
        for (TestCase candidate : candidates)
        {
            copy.remove(candidate);
            for (TestCase candidate2 : copy)
            {
                double sim = dist.getDistance(candidate, candidate2);
                similarity.put(Pair.of(candidate, candidate2), sim);
            }
        }
        List<TestCase> selected = new ArrayList<>();
        // pick starting test cases

        while (!shouldFinish(selected) && similarity.size() > 0)
        {
            // select most dissimilar test case
            double maxDissimilarity = similarity.values().parallelStream().mapToDouble(a -> a).max()
                    .getAsDouble();
            List<Pair<TestCase>> leastSimilar = similarity.entrySet().stream()
                    .filter(entry -> entry.getValue() == maxDissimilarity).map(entry -> entry.getKey()).collect(
                            Collectors.toList());
            Pair<TestCase> selectedPair;
            if (leastSimilar.size() > 1)
            {
                selectedPair = leastSimilar.get(RandomSource.nextInt(leastSimilar.size()));
            }
            else
            {
                selectedPair = leastSimilar.get(0);
            }
            TestCase selectedTest =
                    RandomSource.nextBoolean() ? selectedPair.getLeft() : selectedPair.getRight();

            // add test case to list of selected
            selected.add(selectedTest);
            // remove all instances of that test case
            similarity = similarity.entrySet().stream().filter(
                    entry -> !entry.getKey().contains(selectedTest))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        // keep selecting test cases until coverage criteria/fixed # of test cases selected
        return selected;
    }

    private boolean shouldFinish(List<TestCase> selected)
    {
        int linesToCover = targetClasses.stream()
                .mapToInt(cl -> inst.getTotalLines(ClassStore.get(cl.getName()))).sum();
        Set<Line> linesCovered1 = selected.stream().map(tc -> inst.getLinesCovered(tc)).collect(new HashSetCollector<>());
        linesCovered1 = linesCovered1.stream().filter(l -> targetClasses.contains(l.getParent().getCUT())).collect(
                Collectors.toSet());
        double linesCovered = linesCovered1.size();
        return (minTestCases != -1 && selected.size() > minTestCases) ||
                (coverageAdequacy != -1
                        && (linesCovered / linesToCover) * 100 > coverageAdequacy);
    }

    @Override
    public String readableName()
    {
        return "similarity";
    }

    public boolean needsTargetClass()
    {
        return true;
    }

    public void setTargetClasses(List<Class<?>> targetClasses)
    {
        this.targetClasses = targetClasses;
    }

}

