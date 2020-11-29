package org.kanonizo.algorithms;

import com.scythe.instrumenter.analysis.task.AbstractTask;
import com.scythe.instrumenter.analysis.task.Task;
import com.scythe.instrumenter.analysis.task.TaskTimer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.algorithms.stoppingconditions.StoppingCondition;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.display.Display;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.listeners.TestOrderChangedListener;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public abstract class AbstractSearchAlgorithm implements SearchAlgorithm
{
    private static final Logger LOGGER = LogManager.getLogger(AbstractSearchAlgorithm.class);

    private final KanonizoConfigurationModel configurationModel;
    private final List<TestOrderChangedListener> testOrderChangedListeners;
    private final Instrumenter instrumenter;
    private final Display display;
    protected SystemUnderTest problem;
    protected TestSuite currentOptimal;
    protected int age;
    protected List<StoppingCondition> stoppingConditions = new ArrayList<>();
    protected Instant startTime;
    protected Duration totalTime;
    protected int fitnessEvaluations;

    protected AbstractSearchAlgorithm(
            KanonizoConfigurationModel configurationModel,
            List<TestOrderChangedListener> testOrderChangedListeners,
            Instrumenter instrumenter,
            Display display
    )
    {
        this.configurationModel = configurationModel;
        this.testOrderChangedListeners = testOrderChangedListeners;
        this.instrumenter = instrumenter;
        this.display = display;
    }

    public int getFitnessEvaluations()
    {
        return fitnessEvaluations;
    }

    @Override
    public Duration getTotalTime()
    {
        return totalTime;
    }

    @Override
    public Instant getStartTime()
    {
        return startTime;
    }

    @Override
    public void addStoppingCondition(StoppingCondition cond)
    {
        stoppingConditions.add(cond);
    }

    public void removeStoppingCondition(StoppingCondition cond)
    {
        stoppingConditions.remove(cond);
    }

    public List<StoppingCondition> getStoppingConditions()
    {
        return stoppingConditions;
    }

    protected boolean shouldFinish()
    {
        Optional<StoppingCondition> terminalStoppingCondition = stoppingConditions.stream().filter(cond -> cond.shouldFinish(this)).findFirst();
        terminalStoppingCondition.map(StoppingCondition::getClass).ifPresent(cond -> LOGGER.info("Algorithm terminated by " + cond.getSimpleName()));
        return terminalStoppingCondition.isPresent();
    }

    @Override
    public int getAge()
    {
        return age;
    }

    @Override
    public void setSearchProblem(SystemUnderTest problem)
    {
        this.problem = problem;
        setCurrentOptimal(problem.getTestSuite());
    }

    @Override
    public TestSuite getCurrentOptimal()
    {
        return currentOptimal;
    }

    @Override
    public void setCurrentOptimal(TestSuite chr)
    {
        this.currentOptimal = chr;
    }

    @Override
    public TestSuite run()
    {
        startTime = Instant.now();
        Task timerTask = new AbstractTask()
        {
            @Override
            public String asString()
            {
                return "Running " + getClass().getSimpleName();
            }
        };
        if (InstrumentationProperties.LOG)
        {
            TaskTimer.taskStart(timerTask);
        }
        display.notifyTaskStart("Running Test Prioritisation", false);
        TestSuite solution = generateSolution();
        TaskTimer.taskEnd(timerTask);
        BufferedOutputStream stream = null;
        try
        {
            File file = new File(InstrumentationProperties.LOG_DIR + "/algorithm_time.dat");
            File logdir = new File(InstrumentationProperties.LOG_DIR);
            if (!logdir.exists())
            {
                logdir.mkdirs();
            }
            if (!file.exists())
            {
                file.createNewFile();
            }
            stream = new BufferedOutputStream(new FileOutputStream(file));
            stream.write(String.valueOf(Duration.between(startTime, Instant.now())).getBytes());
        }
        catch (final IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            totalTime = Duration.between(startTime, Instant.now());
            if (stream != null)
            {
                try
                {
                    stream.close();
                }
                catch (final IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        return solution;
    }

    protected abstract TestSuite generateSolution();

    protected void notifyTestSuiteOrderingChanged(List<TestCase> newTestSuite)
    {
        testOrderChangedListeners.forEach(listener -> listener.testOrderingChanged(newTestSuite));
    }

    protected class FitnessComparator implements Comparator<TestCase>
    {

        public FitnessComparator()
        {
        }

        @Override
        public int compare(TestCase o1, TestCase o2)
        {
            return Double.compare(getFitness(o2), getFitness(o1));
        }

    }

}
