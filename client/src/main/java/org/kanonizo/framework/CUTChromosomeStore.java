package org.kanonizo.framework;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CUTChromosomeStore {
	private static final Logger logger = LogManager.getLogger(CUTChromosomeStore.class);
	private static Map<String, CUTChromosome> cuts = new HashMap<String, CUTChromosome>();

	public static void add(String name, CUTChromosome chrom) {
		cuts.put(name, chrom);
	}

	public static CUTChromosome get(String name) {
		return cuts.get(name.replaceAll("/", "."));
	}

	public static CUTChromosome get(int id) {
		Optional<CUTChromosome> cut = cuts.values().stream().filter(cl -> cl.getId() == id).findFirst();
		if(cut.isPresent()){
			return cut.get();
		}
		logger.error("Trying to return CUT that doesn't exist!");
		return null;
	}
}
