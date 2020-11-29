package org.kanonizo.algorithms.heuristics.historybased;

import com.google.common.annotations.VisibleForTesting;
import org.kanonizo.algorithms.TestCasePrioritiser;
import org.kanonizo.commandline.DefaultProgressBar;
import org.kanonizo.commandline.ProgressBar;
import org.kanonizo.commandline.ProgressBar.InertProgressBar;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.configuration.configurableoption.ConfigurableOption;
import org.kanonizo.display.Display;
import org.kanonizo.exception.SystemConfigurationException;
import org.kanonizo.framework.TestCaseStore;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.listeners.TestOrderChangedListener;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;
import static java.util.Arrays.asList;
import static org.kanonizo.commandline.ProgressBar.PROGRESS_BAR_ENABLED_OPTION;
import static org.kanonizo.configuration.configurableoption.ConfigurableOption.configurableOptionFrom;
import static org.kanonizo.util.Util.getConstructorWithParameterTypes;

public abstract class HistoryBased extends TestCasePrioritiser
{
    private static final int PROJECT_ID = 0;
    private static final int VERSION_ID = 1;
    private static final int NUM_REVISIONS = 2;
    private static final int REVISION_ID = 3;
    private static final int TEST_NAME = 4;
    private static final int TEST_RUNTIME = 5;
    private static final int TEST_OUTCOME = 6;
    private static final int TEST_STACK_TRACE = 7;

    private static final ConfigurableOption<String> HISTORY_FILE_NAME = configurableOptionFrom("history_file", String.class, "");
    private final KanonizoConfigurationModel configModel;
    private final File historyFile;
    private final Display display;

    private Map<TestCase, List<TestCaseExecutionResult>> historyData = new HashMap<>();

    private long maxExecutionTime = -1;

    private int maxExecutions = -1;

    public HistoryBased(
            KanonizoConfigurationModel configModel,
            List<TestOrderChangedListener> testOrderChangedListeners,
            Instrumenter instrumenter,
            Display display
    )
    {
        super(configModel, testOrderChangedListeners, instrumenter, display);
        this.configModel = configModel;
        this.historyFile = new File(configModel.getConfigurableOptionValue(HISTORY_FILE_NAME));
        this.display = display;
    }

    public void init(List<TestCase> testCases)
    {
        if (historyFile == null || !historyFile.exists())
        {
            throw new SystemConfigurationException(
                    "In order to execute a history based technique you must provide a -Dhistory_file option in the command line");
        }
        readHistoryFile();
    }

    @VisibleForTesting
    protected void readHistoryFile()
    {
        try
        {
            Scanner p = new Scanner(historyFile);
            // skip header line
            p.nextLine();
            int numRecords = (int) Files.lines(Paths.get(historyFile.getAbsolutePath())).count();
            int count = 0;
            ProgressBar bar = configModel.getConfigurableOptionValue(PROGRESS_BAR_ENABLED_OPTION) ?
                    new DefaultProgressBar(System.out) :
                    new InertProgressBar();

            bar.setTitle("Reading history file");
            while (p.hasNextLine())
            {
                bar.reportProgress(count++, numRecords);
                String line = p.nextLine();
                String[] lineParts = line.split(",");
                String testCaseName = lineParts[TEST_NAME];
                String testClass = testCaseName.split("::")[0];
                String testMethod = testCaseName.split("::")[1];
                String testString = testMethod + "(" + testClass + ")";
                TestCase tc = TestCaseStore.with(testString);
                // skip 0th execution since this is the "current" state, and we shouldn't know this information at runtime
                int ind = Math.abs(Integer.parseInt(lineParts[REVISION_ID]));
                if (ind == 0)
                {
                    continue;
                }
                if (!historyData.containsKey(tc))
                {
                    historyData.put(tc, new ArrayList<>());
                }
                Throwable cause = null;
                if (lineParts.length > TEST_STACK_TRACE)
                {
                    // parse throwable into object here
                    String trace = lineParts[TEST_STACK_TRACE];
                    String exceptionClass = trace.split("[^a-zA-Z.@$]")[0];
                    try
                    {
                        Class<?> cl = Class.forName(exceptionClass);
                        Optional<? extends Constructor<?>> noArgsConstructor = getConstructorWithParameterTypes(cl);
                        if (noArgsConstructor.isPresent())
                        {
                            cause = (Throwable) noArgsConstructor.get().newInstance();
                        }
                        else
                        {
                            Optional<Constructor<?>> constructorWithAllPrimitiveArguments = Arrays.stream(cl.getConstructors())
                                    .filter(c -> areAllPrimitive(c.getParameterTypes())).findFirst();
                            if (constructorWithAllPrimitiveArguments.isPresent())
                            {
                                Constructor<?> con = constructorWithAllPrimitiveArguments.get();
                                // get array of default arguments
                                Object[] args = Arrays.stream(con.getParameterTypes()).map(this::mapArgument).toArray();
                                cause = (Throwable) con.newInstance(args);
                            }
                        }

                    }
                    catch (ClassNotFoundException e)
                    {
                        // if the exception class does not exist, we will just use a generic exception
                        cause = new Exception();
                    }
                    catch (InstantiationException | IllegalAccessException | InvocationTargetException e)
                    {
                        e.printStackTrace();
                    }
                }
                long executionTime = parseLong(lineParts[TEST_RUNTIME]);
                if (executionTime > maxExecutionTime)
                {
                    maxExecutionTime = executionTime;
                }
                int numExecutions = historyData.get(tc).size();
                // necessary check to ensure that test cases that only existed in previous versions are not considered "current"
                // for example if a test case exists in the current version, the first element of its history data should be considered
                // the most recent execution of all test cases, but if we discover a test case later that
                if (ind > numExecutions)
                {
                    for (int i = numExecutions; i < ind; i++)
                    {
                        historyData.get(tc).add(i, TestCaseExecutionResult.NULL_EXECUTION);
                    }
                }
                // if we have padded test matrix with null executions and now we find an execution that should "slot in" to a location
                // this should never happen with the current history file structure, since we systematically move backwards in executions
                // but this guards against the case where we find execution -10, pad 9 NULL_EXECUTION objects and then find version -5, for example
                if (historyData.get(tc).size() > ind && historyData.get(tc).get(ind) == TestCaseExecutionResult.NULL_EXECUTION)
                {
                    historyData.get(tc).remove(ind);
                }
                historyData.get(tc).add(
                        ind,
                        new TestCaseExecutionResult(executionTime, lineParts[TEST_OUTCOME].equals("pass"), cause)
                );
                if (historyData.get(tc).size() > maxExecutions)
                {
                    maxExecutions = historyData.get(tc).size();
                }
            }
            bar.complete();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static final List<Class<?>> primitives = asList(new Class[]{Integer.class, int.class, Float.class,
            float.class, Double.class, double.class,
            Short.class, short.class,
            Long.class, long.class,
            Byte.class, byte.class,
            Character.class, char.class,
            Boolean.class, boolean.class,
            String.class});

    private boolean areAllPrimitive(Class<?>[] types)
    {
        return primitives.containsAll(asList(types));
    }

    private Object mapArgument(Class<?> type)
    {
        if (type.equals(String.class))
        {
            return "";
        }
        else if (type.equals(Boolean.class) || type.equals(boolean.class))
        {
            return false;
        }
        else
        {
            return 0;
        }
    }

    public int getNumExecutions(TestCase tc)
    {
        return !historyData.containsKey(tc) ? 0 : historyData.get(tc).size();
    }

    public int getNumFailures(TestCase tc)
    {
        return !historyData.containsKey(tc) ? 0
                : (int) historyData.get(tc).stream().filter(ex -> !ex.isPassed()).count();
    }

    public boolean hasFailed(TestCase tc)
    {
        return historyData.containsKey(tc) && historyData.get(tc).stream()
                .anyMatch(ex -> !ex.isPassed());
    }

    public int getNumberOfTestCases()
    {
        return historyData.size();
    }

    public int getMaxExecutions()
    {
        return maxExecutions;
    }

    public long getMaxExecutionTime()
    {
        return maxExecutionTime;
    }

    public List<Boolean> getResults(TestCase tc)
    {
        if (!historyData.containsKey(tc))
        {
            return Collections.emptyList();
        }
        return historyData.get(tc).stream().map(TestCaseExecutionResult::isPassed).collect(Collectors.toList());
    }

    public List<Long> getRuntimes(TestCase tc)
    {
        if (!historyData.containsKey(tc))
        {
            return Collections.emptyList();
        }
        return historyData.get(tc).stream().map(TestCaseExecutionResult::getExecutionTime)
                .collect(Collectors.toList());
    }

    public int getTimeSinceLastFailure(TestCase tc)
    {
        if (!hasFailed(tc))
        {
            return Integer.MAX_VALUE;
        }
        return historyData.get(tc)
                .indexOf(historyData.get(tc).stream().filter(ex -> !ex.isPassed()).findFirst().get());
    }

    public int getTimeSinceLastFailure()
    {
        int executionNo = 0;
        while (!anyFail(executionNo))
        {
            executionNo++;
        }
        return executionNo;
    }

    private boolean anyFail(int executionNo)
    {
        return !historyData.values().stream().allMatch(l -> l.size() > executionNo && l.get(executionNo).isPassed());
    }

    public List<TestCase> getFailingTestCases(int executionNo)
    {
        return historyData.entrySet().stream().filter(entry -> entry.getValue().size() > executionNo && !entry.getValue().get(
                executionNo).isPassed()).map(
                Entry::getKey).collect(Collectors.toList());
    }

    public Throwable getCause(TestCase testCase, int executionNo)
    {
        if (historyData.get(testCase).size() < executionNo || historyData.get(testCase).get(executionNo).isPassed())
        {
            return null;
        }
        return historyData.get(testCase).get(executionNo).getFailureCause();
    }


}

