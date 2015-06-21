package org.nusco.narjillos.genomics;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nusco.narjillos.core.utilities.RanGen;

/**
 * A pool of DNA strands.
 */
public abstract class GenePool {

	private long dnaSerialId = 0;
	private final List<Long> currentPool = new LinkedList<>();

	public DNA createDNA(String dna) {
		DNA result = new DNA(nextSerialId(), dna);
		currentPool.add(result.getId());
		return result;
	}

	public DNA createRandomDNA(RanGen ranGen) {
		DNA result = DNA.random(nextSerialId(), ranGen);
		currentPool.add(result.getId());
		return result;
	}

	public DNA mutateDNA(DNA parent, RanGen ranGen) {
		DNA result = parent.mutate(nextSerialId(), ranGen);
		currentPool.add(result.getId());
		return result;
	}

	public void remove(DNA dna) {
		currentPool.remove(dna.getId());
	}

	public abstract DNA getDna(Long id);

	public abstract List<DNA> getAncestry(DNA dna);

	abstract Set<Long> getHistoricalPool();
	
	public abstract DNA getMostSuccessfulDNA();

	public abstract int getGenerationOf(DNA dna);

	public long getCurrentSerialId() {
		return dnaSerialId;
	}

	abstract Map<Long, Long> getChildrenToParents();

	List<Long> getCurrentPool() {
		return currentPool;
	}

	private long nextSerialId() {
		return ++dnaSerialId;
	}
}
