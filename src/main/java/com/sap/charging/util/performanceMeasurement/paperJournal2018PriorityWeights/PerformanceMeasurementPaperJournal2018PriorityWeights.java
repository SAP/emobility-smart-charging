package com.sap.charging.util.performanceMeasurement.paperJournal2018PriorityWeights;

import com.sap.charging.util.Util;
import com.sap.charging.util.performanceMeasurement.PerformanceMeasurement;
import com.sap.charging.util.sqlite.SQLiteAttributeKey;

public class PerformanceMeasurementPaperJournal2018PriorityWeights extends PerformanceMeasurement<PerformanceMeasurementPaperJournal2018PriorityWeights> {
		
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
	public final double weightFairShare;
	
	@SQLiteAttributeKey
	public final double weightCosts;
	
	@SQLiteAttributeKey
	public final double weightPeakShaving;
	
	@SQLiteAttributeKey
	public final double weightLoadImbalance;
	
	
	@SQLiteAttributeKey
	public final int seed;
	
	public PerformanceMeasurementPaperJournal2018PriorityWeights(int nCars, int nChargingStations, double randomness, String methodDayahead, String methodRealtime, 
			double weightFairShare, double weightCosts, double weightPeakShaving, double weightLoadImbalance, int seed) {
		this(null, null, null, nCars, nChargingStations, randomness, methodDayahead, methodRealtime, weightFairShare, weightCosts, weightPeakShaving, weightLoadImbalance, seed);
	}

	public PerformanceMeasurementPaperJournal2018PriorityWeights(String guid, String filePath, String method, int nCars, int nChargingStations, double randomness, String methodDayahead, String methodRealtime, 
			double weightFairShare, double weightCosts, double weightPeakShaving, double weightLoadImbalance, int seed) {
		super(guid, filePath, method);
		this.nCars = nCars;
		this.nChargingStations = nChargingStations;
		this.randomness = randomness;
		this.methodDayahead = methodDayahead;
		this.methodRealtime = methodRealtime;
		this.weightFairShare = weightFairShare;
		this.weightCosts = weightCosts;
		this.weightPeakShaving = weightPeakShaving;
		this.weightLoadImbalance = weightLoadImbalance;
		this.seed = seed;
	}
	

	@Override
	public PerformanceMeasurementPaperJournal2018PriorityWeights cloneWithMethod(String method) {
		String guid = Util.generateGUID();
		String filePath = "gen/performance/" + guid + ".json";
		return new PerformanceMeasurementPaperJournal2018PriorityWeights(guid, filePath, method, nCars, nChargingStations, randomness, methodDayahead, methodRealtime, weightFairShare, weightCosts, weightPeakShaving, weightLoadImbalance, seed);
	}

	@Override
	public String getTableName() {
		return "performanceMeasurementsPaperJournal2018PriorityWeights";
	}
	

}
