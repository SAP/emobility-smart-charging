package com.sap.charging.util.performanceMeasurement.paperINDIN2018;

public class PerformanceMeasurementINDIN2018 extends PerformanceMeasurementINDIN2018Template {

	private final String method;

	
	public PerformanceMeasurementINDIN2018(int nCars, double proportionEVs, double gridConnection, String method) {
		super(nCars, proportionEVs, gridConnection);
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
