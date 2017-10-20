package org.kanonizo.algorithms.stoppingconditions;

import org.kanonizo.Properties;
import org.kanonizo.algorithms.SearchAlgorithm;

public class TimeStoppingCondition implements StoppingCondition {
	@Override
	public boolean shouldFinish(SearchAlgorithm algorithm) {
		return System.currentTimeMillis() - algorithm.getStartTime() > Properties.MAX_EXECUTION_TIME;
	}

}
