package com.sap.charging.util.performanceMeasurement.paperJournal2018Time;

import com.sap.charging.util.Util;
import com.sap.charging.util.performanceMeasurement.PerformanceMeasurement;
import com.sap.charging.util.sqlite.SQLiteAttributeKey;

public class PerformanceMeasurementPaperJournal2018Time extends PerformanceMeasurement<PerformanceMeasurementPaperJournal2018Time> {
	
	
	@SQLiteAttributeKey
	public final int nCars;
	
	@SQLiteAttributeKey
	public final int nChargingStations;
	
	@SQLiteAttributeKey
	public final double randomness;
	
	@SQLiteAttributeKey
	public final String methodDayahead;
	
	@SQLiteAttributeKey
	public final String methodRealtime;
	
	@SQLiteAttributeKey
	public final int seed;
	
	public PerformanceMeasurementPaperJournal2018Time(int nCars, int nChargingStations, double randomness, String methodDayahead, String methodRealtime, int seed) {
		this(null, null, null, nCars, nChargingStations, randomness, methodDayahead, methodRealtime, seed);
	}

	public PerformanceMeasurementPaperJournal2018Time(String guid, String filePath, String method, int nCars, int nChargingStations, double randomness, String methodDayahead, String methodRealtime, int seed) {
		super(guid, filePath, method);
		this.nCars = nCars;
		this.nChargingStations = nChargingStations;
		this.randomness = randomness;
		this.methodDayahead = methodDayahead;
		this.methodRealtime = methodRealtime;
		this.seed = seed;
	}
	

	@Override
	public PerformanceMeasurementPaperJournal2018Time cloneWithMethod(String method) {
		String guid = Util.generateGUID();
		String filePath = "gen/performance/" + guid + ".json";
		return new PerformanceMeasurementPaperJournal2018Time(guid, filePath, method, nCars, nChargingStations, randomness, methodDayahead, methodRealtime, seed);
	}

	@Override
	public String getTableName() {
		return "performanceMeasurementsPaperJournal2018Time";
	}
	
}
