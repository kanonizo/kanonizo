package org.kanonizo.algorithms.stoppingconditions;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import org.kanonizo.algorithms.SearchAlgorithm;

public class IterationsStoppingCondition implements StoppingCondition {

	/**
	 * Whether or not to use iterations as a stopping condition for a
	 * metaheuristic search algorithm
	 */
	@Parameter(key = "use_iterations_stopping_condition", description = "Whether or not to use an iterations stopping condition. If true, then the value of MAX_ITERATIONS will be used to stop the algorithm", category = "TCP")
	public static boolean USE_ITERATIONS = true;

	/**
	 * The maximum number of iterations used for execution of a metaheuristic
	 * search algorithm.
	 */
	@Parameter(key = "max_iterations", description = "The maximum number of iterations a GA is allowed before it finishes", category = "TCP")
	public static int MAX_ITERATIONS = 10000;
	@Override
	public boolean shouldFinish(SearchAlgorithm algorithm) {
		return algorithm.getAge() >= MAX_ITERATIONS;
	}

}
