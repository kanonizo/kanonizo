package org.kanonizo.algorithms.stoppingconditions;

import org.kanonizo.algorithms.SearchAlgorithm;

@FunctionalInterface
public interface StoppingCondition {

	boolean shouldFinish(SearchAlgorithm algorithm);
}
