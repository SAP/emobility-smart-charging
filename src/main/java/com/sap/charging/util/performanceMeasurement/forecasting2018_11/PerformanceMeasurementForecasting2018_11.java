package com.sap.charging.util.performanceMeasurement.forecasting2018_11;

import com.sap.charging.util.Util;
import com.sap.charging.util.performanceMeasurement.PerformanceMeasurement;
import com.sap.charging.util.sqlite.SQLiteAttributeKey;

public class PerformanceMeasurementForecasting2018_11 extends PerformanceMeasurement<PerformanceMeasurementForecasting2018_11> {
	
	@SQLiteAttributeKey
	public final int nCars;
	
	@SQLiteAttributeKey
	public final int nChargingStations;
	
	@SQLiteAttributeKey
	public final String forecastingMethod;
	
	@SQLiteAttributeKey
	public final int seed;
	
	public PerformanceMeasurementForecasting2018_11() {
		this (null, null, null, -1, -1, null, -1);
	}
	
	public PerformanceMeasurementForecasting2018_11(int nCars, int nChargingStations, String forecastingMethod, int seed) {
		this(null, null, null, nCars, nChargingStations, forecastingMethod, seed);
	}

	public PerformanceMeasurementForecasting2018_11(String guid, String filePath, String method, int nCars, int nChargingStations, String forecastingMethod, int seed) {
		super(guid, filePath, method);
		this.nCars = nCars;
		this.nChargingStations = nChargingStations;
		this.forecastingMethod = forecastingMethod;
		this.seed = seed;
	}
	

	@Override
	public PerformanceMeasurementForecasting2018_11 cloneWithMethod(String method) {
		String guid = Util.generateGUID();
		String filePath = "gen/performance/" + guid + ".json";
		return new PerformanceMeasurementForecasting2018_11(guid, filePath, method, nCars, nChargingStations, forecastingMethod, seed);
	}

	@Override
	public String getTableName() {
		return "performanceMeasurementsForecasting2018_11";
	}
	
	
	
	
}
