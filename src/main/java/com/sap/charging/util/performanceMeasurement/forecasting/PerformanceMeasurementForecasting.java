package com.sap.charging.util.performanceMeasurement.forecasting;

import com.sap.charging.util.Util;
import com.sap.charging.util.performanceMeasurement.PerformanceMeasurement;
import com.sap.charging.util.sqlite.SQLiteAttributeKey;

public class PerformanceMeasurementForecasting extends PerformanceMeasurement<PerformanceMeasurementForecasting> {
	
	@SQLiteAttributeKey
	public final int nCars;
	
	@SQLiteAttributeKey
	public final String forecastingMethod;
	
	@SQLiteAttributeKey
	public final int seed;
	
	public PerformanceMeasurementForecasting(int nCars, String forecastingMethod, int seed) {
		this(null, null, null, nCars, forecastingMethod, seed);
	}

	public PerformanceMeasurementForecasting(String guid, String filePath, String method, int nCars, String forecastingMethod, int seed) {
		super(guid, filePath, method);
		this.nCars = nCars;
		this.forecastingMethod = forecastingMethod;
		this.seed = seed;
	}
	

	@Override
	public PerformanceMeasurementForecasting cloneWithMethod(String method) {
		String guid = Util.generateGUID();
		String filePath = "gen/performance/" + guid + ".json";
		return new PerformanceMeasurementForecasting(guid, filePath, method, nCars, forecastingMethod, seed);
	}

	@Override
	public String getTableName() {
		return "performanceMeasurementsForecasting";
	}
	
	
}
