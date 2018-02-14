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

public class CUTCollector
		implements Collector<CUTChromosome, Map<CUTChromosome, List<Integer>>, Map<CUTChromosome, List<Integer>>> {

	@Override
	public BiConsumer<Map<CUTChromosome, List<Integer>>, CUTChromosome> accumulator() {
		return (t, c) -> t.put(c, c.getCoverableLines());
	}

	@Override
	public Set<java.util.stream.Collector.Characteristics> characteristics() {
		return EnumSet.of(Characteristics.UNORDERED);
	}

	@Override
	public BinaryOperator<Map<CUTChromosome, List<Integer>>> combiner() {
		return (left, right) -> {
			left.putAll(right);
			return left;
		};
	}

	@Override
	public Function<Map<CUTChromosome, List<Integer>>, Map<CUTChromosome, List<Integer>>> finisher() {
		return t -> t;
	}

	@Override
	public Supplier<Map<CUTChromosome, List<Integer>>> supplier() {
		return new Supplier<Map<CUTChromosome, List<Integer>>>() {

			@Override
			public Map<CUTChromosome, List<Integer>> get() {
				return new HashMap<CUTChromosome, List<Integer>>();
			}

		};
	}

}
