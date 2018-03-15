package org.kanonizo.algorithms;

import java.util.List;
import org.kanonizo.algorithms.stoppingconditions.StoppingCondition;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;

public interface SearchAlgorithm {
  void setSearchProblem(SystemUnderTest problem);

  TestSuite getCurrentOptimal();

  void setCurrentOptimal(TestSuite chr);

  long getStartTime();

  int getAge();

  void start();

  int getFitnessEvaluations();

  void addStoppingCondition(StoppingCondition cond);

  void removeStoppingCondition(StoppingCondition cond);

  List<StoppingCondition> getStoppingConditions();

  long getTotalTime();

  default boolean needsFitnessFunction(){
    return true;
  }

  default double getFitness(TestSuite chr) {
    // TODO remove default implementation once search algorithms have
    // implemented their own fitness functions
    return 0.0;
  }

  default double getFitness(TestCase chr) {
    // TODO remove default implementation once search algorithms have
    // implemented their own fitness functiosn
    return 0.0;
  }
}
