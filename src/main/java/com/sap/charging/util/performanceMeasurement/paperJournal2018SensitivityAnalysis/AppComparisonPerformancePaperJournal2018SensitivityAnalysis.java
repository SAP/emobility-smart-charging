package com.sap.charging.util.performanceMeasurement.paperJournal2018SensitivityAnalysis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.opt.heuristics.InstanceHeuristicRelSoCLP;
import com.sap.charging.opt.lp.Equation;
import com.sap.charging.opt.lp.InstanceLP;
import com.sap.charging.realTime.StrategyFromDayahead;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.configuration.Options;
import com.sap.charging.util.sqlite.SQLiteDB;

public class AppComparisonPerformancePaperJournal2018SensitivityAnalysis {

	public static final int verbosity = 2;
	
	public static final int maxSCIPThreads = 10;
	public static AtomicInteger currentSCIPThreads = new AtomicInteger(0);
	
	
	public static void main(String[] args) {
		InstanceLP.verbosity = 2;
		Simulation.verbosity = 0;
		
		int nThreads = 10;
		System.out.println("AppComparisonPerformancePaperJournal2018SensitivityAnalysis::main Init with nThreads=" + nThreads + "...");
		Options.set(24, 15, 0.85);
		
		double fuseSize = 999999; // Constant
		
		CONSTANTS.FUSE_LEVEL_0_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_1_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_2_SIZE = fuseSize;
		
		List<PerformanceMeasurementPaperJournal2018SensitivityAnalysis> measurementJobs = new ArrayList<>();
		// Fill list of jobs

		int[] nCarsParams = new int[]{25};  // Constant 
		int[] nChargingStationsParams = new int[] {25}; // Constant 
		double[] randomnessParams = new double[] {0}; // Variable
		
		int[] seeds = IntStream.range(0, 0+1).toArray(); // Variable
		
		//double[] weightSettings = new double[] {0, 1, 10, 100};
		
		double[] weightSettings = new double[] {0, 2.5, 6.3, 15.8, 39.4, 99.5};
		
		System.out.println("Running nCarsParams=" + nCarsParams.length + ", nChargingStations=" + nChargingStationsParams.length + ", seeds=" + seeds.length);
		
		// Construct restrictions only once
		PerformanceMeasurementPaperJournal2018SensitivityAnalysis jobBasic = new PerformanceMeasurementPaperJournal2018SensitivityAnalysis(nCarsParams[0], 
				nChargingStationsParams[0], randomnessParams[0], InstanceHeuristicRelSoCLP.getMethodStatic(), StrategyFromDayahead.getMethodStatic(),
				1, 0, 0, 0, seeds[0]);
		DataGenerator data = MeasurementExecutorPaperJournal2018SensitivityAnalysis.getDataDayhead(jobBasic);
		InstanceHeuristicRelSoCLP instance = new InstanceHeuristicRelSoCLP(data);
		instance.prepareInstanceLP();
		ArrayList<Equation> allRestrictions = instance.getInstanceLP().constructAllRestrictions();
		
		for (int nCars : nCarsParams) {
			for (int nChargingStations: nChargingStationsParams) {
				for (double randomness : randomnessParams) {
					for (int seed : seeds) {
						
						for (double weightFairShare : weightSettings) {
							for (double weightEnergyCosts : weightSettings) {
								for (double weightPeakShaving : weightSettings) {
									for (double weightLoadImbalance : weightSettings) {
										//System.out.println("Running weights: " + weightFairShare + "," + weightEnergyCosts + "," + weightPeakShaving + "," + weightLoadImbalance);
										
										// Add uncoordinated
										//PerformanceMeasurementPaperJournal2018SensitivityAnalysis job1 = new PerformanceMeasurementPaperJournal2018SensitivityAnalysis(nCars, 
										//		nChargingStations, randomness, null, StrategyGreedy.getMethodStatic(), 
										//		-1, -1, -1, 1, seed);
										
										if (isInLongSolvingTimes(weightFairShare, weightEnergyCosts, weightPeakShaving, weightLoadImbalance)) {
											continue;
										}
										
										PerformanceMeasurementPaperJournal2018SensitivityAnalysis job = new PerformanceMeasurementPaperJournal2018SensitivityAnalysis(nCars, 
												nChargingStations, randomness, InstanceHeuristicRelSoCLP.getMethodStatic(), StrategyFromDayahead.getMethodStatic(),
												weightFairShare, weightEnergyCosts, weightPeakShaving, weightLoadImbalance, seed);
										
										measurementJobs.add(job);
									}
								}
							}
						}
						
						
					}
				}
			}
		}
		System.out.println("Running/checking " + measurementJobs.size() + " jobs");
		//Collections.shuffle(measurementJobs); // Shuffle so that jobs are not done round robin but randomly
		
		// Stream jobs in parallel
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "" + nThreads);
		SQLiteDB db = new SQLiteDB("jdbc:sqlite:gen/performanceMeasurements.db");
		//measurementJobs.parallelStream().forEach(new MeasurementExecutorRandom2018_06(db, false));	
	
		measurementJobs.stream().forEach(new MeasurementExecutorPaperJournal2018SensitivityAnalysis(db, false, allRestrictions));	
	}
	
	private static double[][] longSolvingTimes = {
			{100, 0, 1, 1},
			{100, 0, 1, 100},
			{100, 0, 0, 10}
			
	};
	
	private static boolean isInLongSolvingTimes(double w1, double w2, double w3, double w4) {
		for (double[] longSolvingTime : longSolvingTimes) {
			if (longSolvingTime[0] == w1 && longSolvingTime[1] == w2 && longSolvingTime[2] == w3 && longSolvingTime[3] == w4) {
				return true;
			}
		}
		return false;
	}
	
	
}
