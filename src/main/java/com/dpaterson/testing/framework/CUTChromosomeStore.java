package com.dpaterson.testing.framework;

import java.util.HashMap;
import java.util.Map;

public class CUTChromosomeStore {
	private static Map<String, CUTChromosome> cuts = new HashMap<String, CUTChromosome>();

	public static void add(String name, CUTChromosome chrom) {
		cuts.put(name, chrom);
	}

	public static CUTChromosome get(String name) {
		return cuts.get(name.replaceAll("/", "."));
	}
}
