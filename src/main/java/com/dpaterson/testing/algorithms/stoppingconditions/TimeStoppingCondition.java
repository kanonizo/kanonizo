package com.dpaterson.testing.algorithms.stoppingconditions;

import com.dpaterson.testing.Properties;
import com.dpaterson.testing.algorithms.SearchAlgorithm;

public class TimeStoppingCondition implements StoppingCondition {
	@Override
	public boolean shouldFinish(SearchAlgorithm algorithm) {
		return System.currentTimeMillis() - algorithm.getStartTime() > Properties.MAX_EXECUTION_TIME;
	}

}
