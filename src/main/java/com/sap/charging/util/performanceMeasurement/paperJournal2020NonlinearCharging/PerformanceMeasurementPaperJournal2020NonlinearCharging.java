package com.sap.charging.util.performanceMeasurement.paperJournal2020NonlinearCharging;

import com.sap.charging.util.Util;
import com.sap.charging.util.performanceMeasurement.PerformanceMeasurement;
import com.sap.charging.util.sqlite.SQLiteAttributeKey;

public class PerformanceMeasurementPaperJournal2020NonlinearCharging extends PerformanceMeasurement<PerformanceMeasurementPaperJournal2020NonlinearCharging>  {
	
	@SQLiteAttributeKey
	public final int nCars;
	
	@SQLiteAttributeKey
	public final int nChargingStations;
	
	//@SQLiteAttributeKey
	//public final boolean nonlinearCharging;
	
	@SQLiteAttributeKey
	public final double fuseSize;
	
	@SQLiteAttributeKey
	public final double startSoC;
	
	@SQLiteAttributeKey
	public final int seed;
	
	@SQLiteAttributeKey
	public final boolean rescheduleCarsWith0A;
	
	
	public PerformanceMeasurementPaperJournal2020NonlinearCharging(String method, int nCars, int nChargingStations, 
			boolean rescheduleCarsWith0A, 
			double startSoC,
			double fuseSize, int seed) {
		this(null, null, method, nCars, nChargingStations, rescheduleCarsWith0A , startSoC, fuseSize, seed);
	}

	public PerformanceMeasurementPaperJournal2020NonlinearCharging(String guid, String filePath, String method, int nCars, 
			int nChargingStations, 
			boolean rescheduleCarsWith0A, 
			double startSoC,
			double fuseSize, int seed) {
		super(guid, filePath, method);
		this.nCars = nCars;
		this.nChargingStations = nChargingStations;
		this.rescheduleCarsWith0A = rescheduleCarsWith0A;
		this.seed = seed;
		this.fuseSize = fuseSize;
		this.startSoC = startSoC;
	}
	

	@Override
	public PerformanceMeasurementPaperJournal2020NonlinearCharging cloneWithMethod(String method) {
		String guid = Util.generateGUID();
		String filePath = "gen/performance/" + guid + ".json";
		return new PerformanceMeasurementPaperJournal2020NonlinearCharging(guid, filePath, method, 
				nCars, nChargingStations, rescheduleCarsWith0A, startSoC, fuseSize, seed);
	}

	@Override
	public String getTableName() {
		return "performanceMeasurementsPaperJournal2020NonlinearCharging";
	}

	
	
}
