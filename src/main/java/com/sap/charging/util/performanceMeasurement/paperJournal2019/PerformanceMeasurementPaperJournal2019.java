package com.sap.charging.util.performanceMeasurement.paperJournal2019;

import java.util.List;

import com.sap.charging.model.CarProcessData;
import com.sap.charging.util.Util;
import com.sap.charging.util.performanceMeasurement.PerformanceMeasurement;
import com.sap.charging.util.sqlite.SQLiteAttributeIgnore;
import com.sap.charging.util.sqlite.SQLiteAttributeKey;

public class PerformanceMeasurementPaperJournal2019 extends PerformanceMeasurement<PerformanceMeasurementPaperJournal2019> {
	
	
	@SQLiteAttributeKey
	public final int nCars;
	
	@SQLiteAttributeKey
	public final int nChargingStations;
	
	@SQLiteAttributeKey
	public final String forecastingMethod;
	
	@SQLiteAttributeKey
	public final double fuseSize;
	
	@SQLiteAttributeKey
	public final int seed;
	
	@SQLiteAttributeIgnore
	private List<CarProcessData> carProcessData;
	
	public PerformanceMeasurementPaperJournal2019(int nCars, int nChargingStations, String forecastingMethod, double fuseSize, int seed) {
		this(null, null, null, nCars, nChargingStations, forecastingMethod, fuseSize, seed);
	}

	public PerformanceMeasurementPaperJournal2019(String guid, String filePath, String method, int nCars, int nChargingStations, String forecastingMethod, double fuseSize, int seed) {
		super(guid, filePath, method);
		this.nCars = nCars;
		this.nChargingStations = nChargingStations;
		this.seed = seed;
		this.fuseSize = fuseSize;
		this.forecastingMethod = forecastingMethod;
	}
	

	@Override
	public PerformanceMeasurementPaperJournal2019 cloneWithMethod(String method) {
		String guid = Util.generateGUID();
		String filePath = "gen/performance/" + guid + ".json";
		return new PerformanceMeasurementPaperJournal2019(guid, filePath, method, nCars, nChargingStations, forecastingMethod, fuseSize, seed);
	}

	@Override
	public String getTableName() {
		return "performanceMeasurementsPaperJournal2019";
	}

	public void setCarProcessData(List<CarProcessData> carProcessData) {
		this.carProcessData = carProcessData;
	}
	
	public List<CarProcessData> getCarProcessData() {
		return this.carProcessData;
	}
	
	
}
