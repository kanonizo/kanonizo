package org.kanonizo.algorithms.stoppingconditions;

import org.kanonizo.algorithms.SearchAlgorithm;

public class FitnessStoppingCondition implements StoppingCondition {

	@Override
	public boolean shouldFinish(SearchAlgorithm algorithm) {
		return algorithm.getAge() > 0 && algorithm.getCurrentOptimal().getFitness() <= 0.0;
	}

}
