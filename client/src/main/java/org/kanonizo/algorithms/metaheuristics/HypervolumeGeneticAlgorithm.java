package org.kanonizo.algorithms.metaheuristics;

import java.util.Collections;
import java.util.List;
import org.kanonizo.algorithms.AbstractSearchAlgorithm;
import org.kanonizo.algorithms.TestSuitePrioritiser;
import org.kanonizo.framework.objects.TestSuite;

public class HypervolumeGeneticAlgorithm extends TestSuitePrioritiser {

	@Override
	public String readableName() {
		return "hypervolumega";
	}

	@Override
	protected List<TestSuite> generateInitialPopulation() {
		return Collections.emptyList();
	}

	@Override
	protected List<TestSuite> evolve() {
		return Collections.emptyList();
	}
}
