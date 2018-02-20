package org.kanonizo.framework;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.kanonizo.Framework;

public class CUTCollector
		implements Collector<CUTChromosome, Map<CUTChromosome, Set<Integer>>, Map<CUTChromosome, Set<Integer>>> {

	@Override
	public BiConsumer<Map<CUTChromosome, Set<Integer>>, CUTChromosome> accumulator() {
		return (t, c) -> t.put(c, Framework.getInstrumenter().getLines(c));
	}

	@Override
	public Set<java.util.stream.Collector.Characteristics> characteristics() {
		return EnumSet.of(Characteristics.UNORDERED);
	}

	@Override
	public BinaryOperator<Map<CUTChromosome, Set<Integer>>> combiner() {
		return (left, right) -> {
			left.putAll(right);
			return left;
		};
	}

	@Override
	public Function<Map<CUTChromosome, Set<Integer>>, Map<CUTChromosome, Set<Integer>>> finisher() {
		return t -> t;
	}

	@Override
	public Supplier<Map<CUTChromosome, Set<Integer>>> supplier() {
		return new Supplier<Map<CUTChromosome, Set<Integer>>>() {

			@Override
			public Map<CUTChromosome, Set<Integer>> get() {
				return new HashMap<CUTChromosome, Set<Integer>>();
			}

		};
	}

}
