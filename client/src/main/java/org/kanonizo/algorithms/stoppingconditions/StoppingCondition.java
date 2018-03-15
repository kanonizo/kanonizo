package org.kanonizo.algorithms.stoppingconditions;

import org.kanonizo.algorithms.SearchAlgorithm;

public interface StoppingCondition {

	boolean shouldFinish(SearchAlgorithm algorithm);
}
