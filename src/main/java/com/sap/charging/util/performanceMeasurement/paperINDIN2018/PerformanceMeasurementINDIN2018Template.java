package com.sap.charging.util.performanceMeasurement.paperINDIN2018;

import com.sap.charging.util.performanceMeasurement.PerformanceMeasurementTemplate;

public class PerformanceMeasurementINDIN2018Template extends PerformanceMeasurementTemplate<PerformanceMeasurementINDIN2018> {
	
	private final int nCars;
	private final double proportionEVs;
	private final double gridConnection;

	
	public PerformanceMeasurementINDIN2018Template(int nCars, double proportionEVs, double gridConnection) {
		this.nCars = nCars;
		this.proportionEVs = proportionEVs;
		this.gridConnection = gridConnection;
	}
	
	public PerformanceMeasurementINDIN2018 cloneWithMethod(String method) {
		return new PerformanceMeasurementINDIN2018(nCars, proportionEVs, gridConnection, method);
	}
	
	public int getNCars() {
		return nCars;
	}
	public double getProportionEVs() {
		return proportionEVs;
	}
	public double getGridConnection() {
		return gridConnection;
	}
	@Override
	public String toString() {
		return "Measurement: nCars=" + nCars + ", proportionEVs=" + proportionEVs + ", gridConnection=" + gridConnection;
	}
	
}
