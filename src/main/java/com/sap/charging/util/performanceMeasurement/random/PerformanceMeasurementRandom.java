package com.sap.charging.util.performanceMeasurement.random;

public class PerformanceMeasurementRandom extends PerformanceMeasurementRandomTemplate {
	
	
	private final String method;
	
	public PerformanceMeasurementRandom(int nCars, 
			int nChargingStations, int nTimeslots, int seed, boolean solveLP, String method) {
		super(nCars, nChargingStations, nTimeslots, seed, solveLP);
		this.method = method;
		
	}
	
	
	public String getMethod() {
		return method;
	}
	
	
	@Override
	public String toString() {
		return super.toString() + " method=" + getMethod(); 
	}
	
}
