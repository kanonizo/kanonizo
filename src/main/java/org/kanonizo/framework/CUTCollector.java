package org.kanonizo.framework;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import com.scythe.instrumenter.instrumentation.objectrepresentation.Line;

public class CUTCollector
		implements Collector<CUTChromosome, Map<CUTChromosome, List<Line>>, Map<CUTChromosome, List<Line>>> {

	@Override
	public BiConsumer<Map<CUTChromosome, List<Line>>, CUTChromosome> accumulator() {
		return (t, c) -> t.put(c, c.getCoverableLines());
	}

	@Override
	public Set<java.util.stream.Collector.Characteristics> characteristics() {
		return EnumSet.of(Characteristics.UNORDERED);
	}

	@Override
	public BinaryOperator<Map<CUTChromosome, List<Line>>> combiner() {
		return (left, right) -> {
			left.putAll(right);
			return left;
		};
	}

	@Override
	public Function<Map<CUTChromosome, List<Line>>, Map<CUTChromosome, List<Line>>> finisher() {
		return t -> t;
	}

	@Override
	public Supplier<Map<CUTChromosome, List<Line>>> supplier() {
		return new Supplier<Map<CUTChromosome, List<Line>>>() {

			@Override
			public Map<CUTChromosome, List<Line>> get() {
				return new HashMap<CUTChromosome, List<Line>>();
			}

		};
	}

}
