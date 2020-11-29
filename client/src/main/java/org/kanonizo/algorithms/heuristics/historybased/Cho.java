package org.kanonizo.algorithms.heuristics.historybased;

import com.google.common.annotations.VisibleForTesting;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.configuration.configurableoption.DoubleOption;
import org.kanonizo.display.Display;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.listeners.TestOrderChangedListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static org.kanonizo.configuration.configurableoption.DoubleOption.doubleOption;

public class Cho extends HistoryBased
{

    public static DoubleOption ALPHA_OPTION = doubleOption("cho_alpha", 1.0);
    public static DoubleOption BETA_OPTION = doubleOption("cho_beta", 0.7);
    public static DoubleOption GAMMA_OPTION = doubleOption("cho_gamma", 0.4);
    public static DoubleOption DELTA_OPTION = doubleOption("cho_delta", 0.1);
    public static DoubleOption OMEGA_OPTION = doubleOption("cho_omega", 0.1);

    private static final int TEST_FAILURE_WEIGHT = -1;
    private HashMap<TestCase, Double> priority;
    private List<TestCase> ordering;

    private final double alpha;
    private final double beta;
    private final double gamma;
    private final double delta;
    private final double omega;

    public Cho(
            KanonizoConfigurationModel configurationModel,
            List<TestOrderChangedListener> testOrderChangedListeners,
            Instrumenter instrumenter,
            Display display
    )
    {
        super(configurationModel, testOrderChangedListeners, instrumenter, display);
        this.alpha = configurationModel.getDoubleOption(ALPHA_OPTION);
        this.beta = configurationModel.getDoubleOption(BETA_OPTION);
        this.gamma = configurationModel.getDoubleOption(GAMMA_OPTION);
        this.delta = configurationModel.getDoubleOption(DELTA_OPTION);
        this.omega = configurationModel.getDoubleOption(OMEGA_OPTION);
    }

    @Override
    public void init(List<TestCase> testCases)
    {
        super.init(testCases);

        priority = new HashMap<>();
        ordering = new ArrayList<>();

        testCases.forEach(tc -> priority.put(tc, getPriority(tc)));
        ordering.addAll(testCases);
        ordering.sort(Comparator.comparingDouble(priority::get));
    }

    @VisibleForTesting
    protected double getPriority(TestCase tc)
    {
        List<Boolean> results = getResults(tc);
        if (results.size() == 0)
        {
            return 0;
        }
        int consecutiveFails = results.indexOf(true);
        if (consecutiveFails > 0)
        {
            // push simulated pass to the start of the results to ensure recent consecutive fails contribute to
            // frMin, frMax, frAvg etc
            results.add(0, true);
        }
        int frMin = Integer.MAX_VALUE;
        int frMax = 0;
        double frSum = 0.0;
        int frCount = 0;
        int failures = 0;
        for (Boolean result : results)
        {
            if (result)
            {
                if (failures > 0)
                {
                    if (failures < frMin)
                    {
                        frMin = failures;
                    }
                    if (failures > frMax)
                    {
                        frMax = failures;
                    }
                    frSum += failures;
                    frCount++;
                    failures = 0;
                }
            }
            else
            {
                failures++;
            }
        }

        double frAvg = frSum / frCount;
        double priority = 0;
        for (int i = 0; i < results.size() - consecutiveFails; i++)
        {
            if (!results.get(i))
            {
                priority += TEST_FAILURE_WEIGHT * omega;
            }
        }
        for (int i = 0; i < consecutiveFails; i++)
        {
            if (i < frMin)
            {
                priority += TEST_FAILURE_WEIGHT * alpha;
            }
            else if (i < frAvg)
            {
                priority += TEST_FAILURE_WEIGHT * beta;
            }
            else if (i < frMax)
            {
                priority += TEST_FAILURE_WEIGHT * gamma;
            }
            else
            {
                priority += TEST_FAILURE_WEIGHT * delta;
            }
        }
        return priority;
    }

    @Override
    public TestCase selectTestCase(List<TestCase> testCases)
    {
        TestCase next = ordering.get(0);
        ordering.remove(next);
        return next;
    }

    @Override
    public String readableName()
    {
        return "cho";
    }
}
