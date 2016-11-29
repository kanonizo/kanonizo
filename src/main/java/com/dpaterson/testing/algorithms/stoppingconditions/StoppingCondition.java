package com.dpaterson.testing.algorithms.stoppingconditions;

import com.dpaterson.testing.algorithms.SearchAlgorithm;

public interface StoppingCondition {

	boolean shouldFinish(SearchAlgorithm algorithm);
}
