package org.kanonizo.algorithms.heuristics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kanonizo.algorithms.TestCasePrioritiser;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.display.Display;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.listeners.TestOrderChangedListener;

public class KOptimalAlgorithm extends TestCasePrioritiser
{
    private final int k = 2;
    private final Set<Line> allLinesSeen = new HashSet<>();
    private final List<TestCase> bestK = new ArrayList<>();

    public KOptimalAlgorithm(
            KanonizoConfigurationModel configurationModel,
            List<TestOrderChangedListener> testOrderChangedListeners,
            Instrumenter instrumenter,
            Display display
    )
    {
        super(configurationModel, testOrderChangedListeners, instrumenter, display);
    }

    @Override
    public TestCase selectTestCase(List<TestCase> testCases)
    {
        if (bestK.isEmpty())
        {
            selectOptimal(testCases);
        }
        TestCase best = bestK.get(0);
        bestK.remove(0);
        allLinesSeen.addAll(inst.getLinesCovered(best));
        return best;
    }

    private void selectOptimal(List<TestCase> testCases)
    {
        List<TestCase> cases = new ArrayList<TestCase>();
        double maxFitness = 0.0;
        for (int i = 0; i < testCases.size(); i++)
        {
            for (int j = 0; j < testCases.size(); j++)
            {
                if (i > j)
                {
                    TestCase tc1 = testCases.get(i);
                    TestCase tc2 = testCases.get(j);
                    double fitness = getFitness(tc1, tc2);
                    if (fitness > maxFitness)
                    {
                        maxFitness = fitness;
                        cases.clear();
                        cases.add(tc1);
                        cases.add(tc2);
                    }
                }
            }
        }
        bestK.clear();
        bestK.addAll(cases);
    }

    private double getFitness(TestCase tc1, TestCase tc2)
    {
        int previousLines = allLinesSeen.size();
        Set<Line> temp = new HashSet<>(allLinesSeen);
        temp.addAll(inst.getLinesCovered(tc1));
        temp.addAll(inst.getLinesCovered(tc2));
        return temp.size() - previousLines;
    }

    @Override
    public String readableName()
    {
        return "koptimal";
    }
}
