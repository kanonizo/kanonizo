package org.kanonizo.algorithms.metaheuristics;

import java.util.Collections;
import java.util.List;
import org.kanonizo.algorithms.AbstractSearchAlgorithm;
import org.kanonizo.algorithms.TestSuitePrioritiser;
import org.kanonizo.framework.objects.TestSuite;

public class EpistaticGeneticAlgorithm extends TestSuitePrioritiser {


	@Override
	protected List<TestSuite> generateInitialPopulation() {
		return Collections.emptyList();
	}

	@Override
	protected List<TestSuite> evolve() {
		return Collections.emptyList();
	}

	@Override
	public String readableName() {
		return "epistaticga";
	}
}
