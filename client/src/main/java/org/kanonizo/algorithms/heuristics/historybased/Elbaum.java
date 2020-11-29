package org.kanonizo.algorithms.heuristics.historybased;

import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.configuration.configurableoption.ConfigurableOption;
import org.kanonizo.display.Display;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.listeners.TestOrderChangedListener;
import org.kanonizo.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.kanonizo.configuration.configurableoption.ConfigurableOption.configurableOptionFrom;

public class Elbaum extends HistoryBased
{
    private static final ConfigurableOption<Integer> TIME_SINCE_LAST_FAILURE_OPTION = configurableOptionFrom("time_since_last_failure", Integer.class, 5);
    private static final ConfigurableOption<Integer> NEW_TEST_CASE_LIMIT_OPTION = configurableOptionFrom("new_test_case_executions", Integer.class, 5);

    private final int timeSinceLastFailure;
    private final int newTestCaseLimit;
    private final Map<TestCase, Integer> priority = new HashMap<>();

    public Elbaum(
            KanonizoConfigurationModel configurationModel,
            List<TestOrderChangedListener> testOrderChangedListeners,
            Instrumenter instrumenter,
            Display display
    )
    {
        super(configurationModel, testOrderChangedListeners, instrumenter, display);
        this.timeSinceLastFailure = configurationModel.getConfigurableOptionValue(TIME_SINCE_LAST_FAILURE_OPTION);
        this.newTestCaseLimit = configurationModel.getConfigurableOptionValue(NEW_TEST_CASE_LIMIT_OPTION);
    }

    @Override
    public void init(List<TestCase> testCases)
    {
        super.init(testCases);
        for (TestCase tc : testCases)
        {
            if (getTimeSinceLastFailure(tc) <= timeSinceLastFailure || getNumExecutions(tc) <= newTestCaseLimit)
            {
                priority.put(tc, 1);
            }
            else
            {
                priority.put(tc, 2);
            }
        }
        Map<TestCase, Integer> testCasesOrderedByPriority = Util.sortByValue(priority);
        priority.clear();
        priority.putAll(testCasesOrderedByPriority);
    }

    @Override
    public TestCase selectTestCase(List<TestCase> testCases)
    {
        TestCase tc = priority.keySet().iterator().next();
        priority.remove(tc);
        return tc;
    }

    @Override
    public String readableName()
    {
        return "elbaum";
    }
}
