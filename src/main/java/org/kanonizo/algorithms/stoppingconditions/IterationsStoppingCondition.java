package org.kanonizo.algorithms.stoppingconditions;

import org.kanonizo.Properties;
import org.kanonizo.algorithms.SearchAlgorithm;

public class IterationsStoppingCondition implements StoppingCondition {

	@Override
	public boolean shouldFinish(SearchAlgorithm algorithm) {
		return algorithm.getAge() >= Properties.MAX_ITERATIONS;
	}

}
