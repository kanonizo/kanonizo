package com.dpaterson.testing.algorithms;

import com.dpaterson.testing.algorithms.stoppingconditions.StoppingCondition;
import com.dpaterson.testing.framework.TestCaseChromosome;
import com.dpaterson.testing.framework.TestSuiteChromosome;

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
