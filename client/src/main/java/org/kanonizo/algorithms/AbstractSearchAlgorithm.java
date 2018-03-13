package org.kanonizo.algorithms;

import com.scythe.instrumenter.InstrumentationProperties;
import com.scythe.instrumenter.analysis.task.AbstractTask;
import com.scythe.instrumenter.analysis.task.Task;
import com.scythe.instrumenter.analysis.task.TaskTimer;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.Framework;
import org.kanonizo.algorithms.stoppingconditions.StoppingCondition;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;

public abstract class AbstractSearchAlgorithm implements SearchAlgorithm {
  protected SystemUnderTest problem;
  protected TestSuite currentOptimal;
  private static final Logger LOGGER = LogManager.getLogger(AbstractSearchAlgorithm.class);

  protected int age;
  protected List<StoppingCondition> stoppingConditions = new ArrayList<>();
  protected long startTime;
  protected long totalTime;
  protected int fitnessEvaluations;

  protected Framework fw = Framework.getInstance();

  public int getFitnessEvaluations() {
    return fitnessEvaluations;
  }

  @Override
  public long getTotalTime() {
    return totalTime;
  }

  @Override
  public long getStartTime() {
    return startTime;
  }

  @Override
  public void addStoppingCondition(StoppingCondition cond) {
    stoppingConditions.add(cond);
  }

  public void removeStoppingCondition(StoppingCondition cond){
    stoppingConditions.remove(cond);
  }

  public List<StoppingCondition> getStoppingConditions(){
    return stoppingConditions;
  }

  protected boolean shouldFinish() {
    for (StoppingCondition cond : stoppingConditions) {
      if (cond.shouldFinish(this)) {
        LOGGER.info("Algorithm terminated by " + cond.getClass().getName());
        return true;
      }
    }
    return false;
  }

  @Override
  public int getAge() {
    return age;
  }

  @Override
  public void setSearchProblem(SystemUnderTest problem) {
    this.problem = problem;
    setCurrentOptimal(problem.getTestSuite());
  }

  @Override
  public TestSuite getCurrentOptimal() {
    return currentOptimal;
  }

  @Override
  public void setCurrentOptimal(TestSuite chr) {
    this.currentOptimal = chr;
  }

  @Override
  public void start() {
    startTime = System.currentTimeMillis();
    Task timerTask = new AbstractTask() {
      @Override
      public String asString() {
        return "Running " + getClass().getSimpleName();
      }
    };
    if (InstrumentationProperties.LOG) {
      TaskTimer.taskStart(timerTask);
    }
    generateSolution();
    TaskTimer.taskEnd(timerTask);
    BufferedOutputStream stream = null;
    try {
      File file = new File(InstrumentationProperties.LOG_DIR + "/algorithm_time.dat");
      File logdir = new File(InstrumentationProperties.LOG_DIR);
      if (!logdir.exists()) {
        logdir.mkdirs();
      }
      if (!file.exists()) {
        file.createNewFile();
      }
      stream = new BufferedOutputStream(new FileOutputStream(file));
      stream.write(String.valueOf(System.currentTimeMillis() - startTime).getBytes());
    } catch (final IOException e) {
      e.printStackTrace();
    } finally {
      totalTime = System.currentTimeMillis() - startTime;
      if (stream != null) {
        try {
          stream.close();
        } catch (final IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  protected abstract void generateSolution();

  protected class FitnessComparator implements Comparator<TestCase> {

    public FitnessComparator() {
    }

    @Override
    public int compare(TestCase o1, TestCase o2) {
      return ((Double) getFitness(o2)).compareTo(getFitness(o1));
    }

  }

}
