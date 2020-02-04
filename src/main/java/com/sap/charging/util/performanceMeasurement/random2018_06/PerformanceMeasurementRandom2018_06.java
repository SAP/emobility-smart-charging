package com.sap.charging.util.performanceMeasurement.random2018_06;

import com.sap.charging.util.Util;
import com.sap.charging.util.performanceMeasurement.PerformanceMeasurement;
import com.sap.charging.util.sqlite.SQLiteAttributeKey;

public class PerformanceMeasurementRandom2018_06 extends PerformanceMeasurement<PerformanceMeasurementRandom2018_06>  {

	
	@SQLiteAttributeKey
	public final int nCars;
	
	@SQLiteAttributeKey
	public final int nChargingStations;
	
	@SQLiteAttributeKey
	public final int seed;
	
	public PerformanceMeasurementRandom2018_06(int nCars, int nChargingStations, int seed) {
		this(null, null, null, nCars, nChargingStations, seed);
	}

	public PerformanceMeasurementRandom2018_06(String guid, String filePath, String method, int nCars, int nChargingStations, int seed) {
		super(guid, filePath, method);
		this.nCars = nCars;
		this.nChargingStations = nChargingStations;
		this.seed = seed;
	}
	

	@Override
	public PerformanceMeasurementRandom2018_06 cloneWithMethod(String method) {
		String guid = Util.generateGUID();
		String filePath = "gen/performance/" + guid + ".json";
		return new PerformanceMeasurementRandom2018_06(guid, filePath, method, nCars, nChargingStations, seed);
	}

	@Override
	public String getTableName() {
		return "performanceMeasurementsRandom2018_06";
	}
	
}
