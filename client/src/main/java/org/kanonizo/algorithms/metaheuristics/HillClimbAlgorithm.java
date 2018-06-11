package org.kanonizo.algorithms.metaheuristics;

import java.util.List;
import org.kanonizo.algorithms.AbstractSearchAlgorithm;
import org.kanonizo.algorithms.TestSuitePrioritiser;
import org.kanonizo.framework.objects.TestSuite;

public class HillClimbAlgorithm extends TestSuitePrioritiser {

	@Override
	protected List<TestSuite> generateInitialPopulation() {
		return null;
	}

	@Override
	protected void evolve() {

	}

	@Override
	public String readableName() {
		return "hillclimb";
	}
}
