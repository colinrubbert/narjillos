package org.nusco.narjillos;

import org.nusco.narjillos.application.Version;
import org.nusco.narjillos.core.utilities.Configuration;
import org.nusco.narjillos.experiment.Experiment;
import org.nusco.narjillos.experiment.environment.Ecosystem;
import org.nusco.narjillos.genomics.GenePool;
import org.nusco.narjillos.persistence.VolatileDNALog;
import org.nusco.narjillos.persistence.VolatileHistoryLog;

public class SimpleExperiment extends Experiment {

	public SimpleExperiment() {
		super(1234, new Ecosystem(Configuration.ECOSYSTEM_BLOCKS_PER_EDGE_IN_APP * 1000, false), "simple_experiment-" + Version.read());
		setGenePool(new GenePool(new VolatileDNALog()));
		setHistoryLog(new VolatileHistoryLog());
		populate();
	}
}
