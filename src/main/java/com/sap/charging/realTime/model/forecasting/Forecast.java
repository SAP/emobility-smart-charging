package com.sap.charging.realTime.model.forecasting;

import com.sap.charging.sim.Simulation;
import com.sap.charging.util.Loggable;

public abstract class Forecast implements Loggable {

	@Override
	public int getVerbosity() {
		return Simulation.verbosity;
	}
	
	
}
