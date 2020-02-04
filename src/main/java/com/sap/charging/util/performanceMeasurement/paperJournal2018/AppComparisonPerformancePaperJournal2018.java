package com.sap.charging.util.performanceMeasurement.paperJournal2018;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.opt.heuristics.InstanceHeuristicRelSoCLP;
import com.sap.charging.opt.lp.InstanceLP;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.StrategyFromDayahead;
import com.sap.charging.realTime.StrategyGreedy;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.configuration.Options;
import com.sap.charging.util.sqlite.SQLiteDB;

public class AppComparisonPerformancePaperJournal2018 {

	public static final int maxSCIPThreads = 5;
	public static AtomicInteger currentSCIPThreads = new AtomicInteger(0);
	
	public static int verbosity = 2;

	public static void main(String[] args) {
		InstanceLP.verbosity = 1;
		Simulation.verbosity = 0;
		
		int nThreads = 20;
		System.out.println("AppComparisonPerformancePaperJournal2018::main Init with nThreads=" + nThreads + "...");
		Options.set(24, 15, 0.85);
		
		double fuseSize = 150; // Constant 125 is good for 10 vs 5
		
		CONSTANTS.FUSE_LEVEL_0_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_1_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_2_SIZE = fuseSize;
		
		List<PerformanceMeasurementPaperJournal2018> measurementJobs = new ArrayList<>();
		// Fill list of jobs

		int[] nCarsParams = new int[]{50};  // Constant 
		int[] nChargingStationsParams = new int[] {25}; // Constant 
		//double[] randomnessParams = new double[] {0.1, 0.3, 0.5, 0.7, 0.9}; // Variable
		//double[] randomnessParams = new double[] {0}; // Variable
		double[] randomnessParams = new double[21];
		for (int i=0;i<randomnessParams.length;i++) {
			randomnessParams[i] = 1.0*i/(randomnessParams.length-1);
		}
		
		int[] seeds = IntStream.range(0, 19+1).toArray(); // Variable
		
		System.out.println("Running nCarsParams=" + nCarsParams.length + ", nChargingStations=" + nChargingStationsParams.length + ", seeds=" + seeds.length);
		
		for (int nCars : nCarsParams) {
			for (int nChargingStations: nChargingStationsParams) {
				for (double randomness : randomnessParams) {
					for (int seed : seeds) {
						// Uncoordinated (real-time)
						PerformanceMeasurementPaperJournal2018 job = new PerformanceMeasurementPaperJournal2018(nCars, nChargingStations, randomness, null, StrategyGreedy.getMethodStatic(), seed);
						
						// Day-ahead planning
						PerformanceMeasurementPaperJournal2018 job2 = new PerformanceMeasurementPaperJournal2018(nCars, nChargingStations, randomness, InstanceHeuristicRelSoCLP.getMethodStatic(), StrategyFromDayahead.getMethodStatic(), seed);
						
						// Schedule guided heuristic
						PerformanceMeasurementPaperJournal2018 job3 = new PerformanceMeasurementPaperJournal2018(nCars, nChargingStations, randomness, null, StrategyAlgorithmic.getMethodWithoutScheduleStatic(), seed);
						
						// Day-ahead planning + schedule guided heuristic
						PerformanceMeasurementPaperJournal2018 job4 = new PerformanceMeasurementPaperJournal2018(nCars, nChargingStations, randomness, InstanceHeuristicRelSoCLP.getMethodStatic(), StrategyAlgorithmic.getMethodWithScheduleStatic(), seed);
						
						measurementJobs.add(job);
						measurementJobs.add(job2);
						measurementJobs.add(job3);
						measurementJobs.add(job4);
					}
				}
			}
		}
		//PerformanceMeasurementForecasting job = new PerformanceMeasurementForecasting(50, "forecastXGBoost", 0);
		//measurementJobs.add(job);
		
		//Collections.shuffle(measurementJobs); // Shuffle so that jobs are not done round robin but randomly
		
		// Stream jobs in parallel
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "" + nThreads);
		SQLiteDB db = new SQLiteDB("jdbc:sqlite:gen/performanceMeasurements.db");
		//measurementJobs.parallelStream().forEach(new MeasurementExecutorRandom2018_06(db, false));	
	
		measurementJobs.stream().forEach(new MeasurementExecutorPaperJournal2018(db, false));	
	}
	
	
}
