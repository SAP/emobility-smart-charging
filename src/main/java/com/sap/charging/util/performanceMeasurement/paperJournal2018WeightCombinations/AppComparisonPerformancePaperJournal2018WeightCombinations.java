package com.sap.charging.util.performanceMeasurement.paperJournal2018WeightCombinations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.opt.heuristics.InstanceHeuristicAbsSoCLP;
import com.sap.charging.opt.lp.InstanceLP;
import com.sap.charging.realTime.StrategyFromDayahead;
import com.sap.charging.realTime.StrategyGreedy;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.configuration.Options;
import com.sap.charging.util.sqlite.SQLiteDB;

public class AppComparisonPerformancePaperJournal2018WeightCombinations {

	public static final int verbosity = 2;
	
	
	public static final int maxSCIPThreads = 5;
	public static AtomicInteger currentSCIPThreads = new AtomicInteger(0);
	
	
	public static void main(String[] args) {
		InstanceLP.verbosity = 2;
		Simulation.verbosity = 0;
		
		int nThreads = 20;
		System.out.println("AppComparisonPerformancePaperJournal2018WeightCombinations::main Init with nThreads=" + nThreads + "...");
		Options.set(24, 15, 0.85);
		
		double fuseSize = 999999; // Constant
		
		CONSTANTS.FUSE_LEVEL_0_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_1_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_2_SIZE = fuseSize;
		
		List<PerformanceMeasurementPaperJournal2018WeightCombinations> measurementJobs = new ArrayList<>();
		// Fill list of jobs

		int[] nCarsParams = new int[]{25};  // Constant 
		int[] nChargingStationsParams = new int[] {25}; // Constant 
		double[] randomnessParams = new double[] {0}; // Variable
		
		int[] seeds = IntStream.range(0, 0+1).toArray(); // Variable
		
		//if (true) return;
		
		System.out.println("Running nCarsParams=" + nCarsParams.length + ", nChargingStations=" + nChargingStationsParams.length + ", seeds=" + seeds.length);
		
		for (int nCars : nCarsParams) {
			for (int nChargingStations: nChargingStationsParams) {
				for (double randomness : randomnessParams) {
					for (int seed : seeds) {
							
						double weightFairShare = 1e8;
						
						PerformanceMeasurementPaperJournal2018WeightCombinations job1 = new PerformanceMeasurementPaperJournal2018WeightCombinations(nCars, 
								nChargingStations, randomness, null, StrategyGreedy.getMethodStatic(),
								-1, -1, -1, -1, seed);
						
						
						// Cost minimization
						PerformanceMeasurementPaperJournal2018WeightCombinations job2 = new PerformanceMeasurementPaperJournal2018WeightCombinations(nCars, 
								nChargingStations, randomness, InstanceHeuristicAbsSoCLP.getMethodStatic(), StrategyFromDayahead.getMethodStatic(),
								weightFairShare, 1, 0, 1e-3, seed);
						
						// Peak demand minimization
						PerformanceMeasurementPaperJournal2018WeightCombinations job3 = new PerformanceMeasurementPaperJournal2018WeightCombinations(nCars, 
								nChargingStations, randomness, InstanceHeuristicAbsSoCLP.getMethodStatic(), StrategyFromDayahead.getMethodStatic(),
								weightFairShare, 0, 1, 1e-3, seed);
						
						// Load imbalance minimization
						PerformanceMeasurementPaperJournal2018WeightCombinations job4 = new PerformanceMeasurementPaperJournal2018WeightCombinations(nCars, 
								nChargingStations, randomness, InstanceHeuristicAbsSoCLP.getMethodStatic(), StrategyFromDayahead.getMethodStatic(),
								weightFairShare, 0, 1e-3, 1, seed);
						
						
						//if (measurementJobs.size() >= 6) continue;
						
						measurementJobs.add(job1);
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
	
		measurementJobs.stream().forEach(new MeasurementExecutorPaperJournal2018WeightCombinations(db, false));	
	}
	
	
	
}
