package org.kanonizo.algorithms.stoppingconditions;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import org.kanonizo.algorithms.SearchAlgorithm;

public class TimeStoppingCondition implements StoppingCondition {

	/**
	 * The maximum amount of time allowed for execution of a metaheuristic
	 * search algorithm
	 */
	@Parameter(key = "max_execution_time", description = "The maximum amount of time a GA is allowed to run for", category = "TCP")
	public static long MAX_EXECUTION_TIME = 60 * 1000L;

	/**
	 * Whether or not to use time as a stopping condition for a metaheuristic
	 * search algorithm.
	 */
	@Parameter(key = "use_time_stopping_condition", description = "Whether or not to use a time stopping condition. If true, then the value of MAX_EXECUTION_TIME will be used to stop the algorithm", category = "TCP")
	public static boolean USE_TIME = true;

	@Override
	public boolean shouldFinish(SearchAlgorithm algorithm) {
		return System.currentTimeMillis() - algorithm.getStartTime() > MAX_EXECUTION_TIME;
	}

}
