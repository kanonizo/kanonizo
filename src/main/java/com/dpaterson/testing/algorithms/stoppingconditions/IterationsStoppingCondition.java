package com.dpaterson.testing.algorithms.stoppingconditions;

import com.dpaterson.testing.Properties;
import com.dpaterson.testing.algorithms.SearchAlgorithm;

public class IterationsStoppingCondition implements StoppingCondition {

	@Override
	public boolean shouldFinish(SearchAlgorithm algorithm) {
		return algorithm.getAge() >= Properties.MAX_ITERATIONS;
	}

}
