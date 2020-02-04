package com.sap.charging.util.performanceMeasurement.forecastingNextDayEVs;

import com.sap.charging.util.Util;
import com.sap.charging.util.performanceMeasurement.PerformanceMeasurement;
import com.sap.charging.util.sqlite.SQLiteAttributeKey;
import com.sap.charging.util.sqlite.SQLiteDB;

public class PerformanceMeasurementForecastingNextDayEVs extends PerformanceMeasurement<PerformanceMeasurementForecastingNextDayEVs> {

	
	@SQLiteAttributeKey
	public final int nCars;
	
	@SQLiteAttributeKey
	public final int nChargingStations;
	
	@SQLiteAttributeKey
	public final String carAssignmentMethod; // first-come-first-served (baseline), round robin, forecasting, reservations (oracle)
	
	@SQLiteAttributeKey 
	public final int day;
	
	@SQLiteAttributeKey
	public final int seed;
	
	public PerformanceMeasurementForecastingNextDayEVs(int nCars, int nChargingStations, String carAssignmentMethod, int day, int seed) {
		this(null, null, null, nCars, nChargingStations, carAssignmentMethod, day, seed);
	}

	public PerformanceMeasurementForecastingNextDayEVs(String guid, String filePath, 
			String method, int nCars, int nChargingStations, String carAssignmentMethod, int day, int seed) {
		super(guid, filePath, method);
		this.nCars = nCars;
		this.nChargingStations = nChargingStations;
		this.carAssignmentMethod = carAssignmentMethod;
		this.day = day;
		this.seed = seed;
	}
	
	@Override
	public PerformanceMeasurementForecastingNextDayEVs cloneWithMethod(String method) {
		String guid = Util.generateGUID();
		String filePath = "gen/performance/" + guid + ".json";
		return new PerformanceMeasurementForecastingNextDayEVs(guid, filePath, method, nCars, nChargingStations, carAssignmentMethod, day, seed);
	}
	
	public PerformanceMeasurementForecastingNextDayEVs getPreviousDayMeasurement(SQLiteDB db, int dayToRetrieve) {
		// Previous method will not have guid, filePath and method filled out
		PerformanceMeasurementForecastingNextDayEVs previousMeasurement = new PerformanceMeasurementForecastingNextDayEVs(
				nCars, nChargingStations, carAssignmentMethod, dayToRetrieve, seed);
		db.retrieveRow(previousMeasurement); // Changes values by call
		return previousMeasurement;
	}

	
	
	
	
	
	
	
	
	@Override
	public String getTableName() {
		return "performanceMeasurementsForecastingNextDayEVs";
	}
	
}
