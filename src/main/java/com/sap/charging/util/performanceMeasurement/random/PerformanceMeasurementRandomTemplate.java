package com.sap.charging.util.performanceMeasurement.random;

import com.sap.charging.util.performanceMeasurement.PerformanceMeasurementTemplate;

public class PerformanceMeasurementRandomTemplate extends PerformanceMeasurementTemplate<PerformanceMeasurementRandom> {

	private final int nCars;
	private final int nChargingStations;
	private final int nTimeslots;
	private final int seed;
	
	private final boolean solveLP;
	
	public PerformanceMeasurementRandomTemplate(int nCars, int nChargingStations, int nTimeslots, int seed, boolean solveLP) {
		this.nCars = nCars;
		this.nChargingStations = nChargingStations;
		this.nTimeslots = nTimeslots;
		this.seed = seed;
		
		this.solveLP = solveLP;
	}
	
	public int getNCars() {
		return nCars;
	}
	public int getNChargingStations() {
		return nChargingStations;
	}
	public int getNTimeslots() {
		return nTimeslots;
	}
	public int getSeed() {
		return seed;
	}
	public boolean getSolveLP() {
		return solveLP;
	}
	
	@Override
	public PerformanceMeasurementRandom cloneWithMethod(String method) {
		PerformanceMeasurementRandom result = new PerformanceMeasurementRandom(this.getNCars(), 
				this.getNChargingStations(), this.getNTimeslots(), 
				this.getSeed(), this.getSolveLP(), method);
		return result;
	} 
	
	@Override
	public String toString() {
		return "Measurement: nCars=" + nCars +
							" nChargingStations=" + nChargingStations + 
							" nTimeslots=" + nTimeslots +
							" seed=" + seed +
							" solveLP=" + solveLP;
	}

	
	
}
