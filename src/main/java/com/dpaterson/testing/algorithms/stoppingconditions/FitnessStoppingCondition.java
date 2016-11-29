package com.dpaterson.testing.algorithms.stoppingconditions;

import com.dpaterson.testing.algorithms.SearchAlgorithm;

public class FitnessStoppingCondition implements StoppingCondition {

	@Override
	public boolean shouldFinish(SearchAlgorithm algorithm) {
		return algorithm.getAge() > 0 && algorithm.getCurrentOptimal().getFitness() <= 0.0;
	}

}
