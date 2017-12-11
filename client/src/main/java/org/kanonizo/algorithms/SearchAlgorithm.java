package org.kanonizo.algorithms;

import org.kanonizo.algorithms.stoppingconditions.StoppingCondition;
import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestSuiteChromosome;

public interface SearchAlgorithm {
  void setSearchProblem(TestSuiteChromosome problem);

  TestSuiteChromosome getCurrentOptimal();

  void setCurrentOptimal(TestSuiteChromosome chr);

  long getStartTime();

  int getAge();

  void start();

  int getFitnessEvaluations();

  void addStoppingCondition(StoppingCondition cond);

  long getTotalTime();

  default boolean needsFitnessFunction(){
    return true;
  }

  default double getFitness(TestSuiteChromosome chr) {
    // TODO remove default implementation once search algorithms have
    // implemented their own fitness functions
    return 0.0;
  }

  default double getFitness(TestCaseChromosome chr) {
    // TODO remove default implementation once search algorithms have
    // implemented their own fitness functiosn
    return 0.0;
  }
}
