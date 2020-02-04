package com.sap.charging.util.performanceMeasurement.paperOR2018;

import com.sap.charging.util.Util;
import com.sap.charging.util.performanceMeasurement.PerformanceMeasurement;
import com.sap.charging.util.sqlite.SQLiteAttributeKey;

public class PerformanceMeasurementPaperOR2018 extends PerformanceMeasurement<PerformanceMeasurementPaperOR2018>{

	@SQLiteAttributeKey
	public final int nCars;
	
	@SQLiteAttributeKey
	public final int nChargingStations;
	
	@SQLiteAttributeKey
	public final int seed;
	
	
	public PerformanceMeasurementPaperOR2018(int nCars, int nChargingStations, int seed) {
		this(null, null, null, nCars, nChargingStations, seed);
	}

	public PerformanceMeasurementPaperOR2018(String guid, String filePath, String method, int nCars, int nChargingStations, int seed) {
		super(guid, filePath, method);
		this.nCars = nCars;
		this.nChargingStations = nChargingStations;
		this.seed = seed;
	}
	

	@Override
	public PerformanceMeasurementPaperOR2018 cloneWithMethod(String method) {
		String guid = Util.generateGUID();
		String filePath = "gen/performance/" + guid + ".json";
		return new PerformanceMeasurementPaperOR2018(guid, filePath, method, nCars, nChargingStations, seed);
	}

	@Override
	public String getTableName() {
		return "performanceMeasurementsPaperOR2018";
	}
	
}
