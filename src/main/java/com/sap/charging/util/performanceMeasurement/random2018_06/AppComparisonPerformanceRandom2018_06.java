package com.sap.charging.util.performanceMeasurement.random2018_06;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import com.sap.charging.opt.lp.InstanceLP;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.configuration.Options;
import com.sap.charging.util.sqlite.SQLiteDB;

public class AppComparisonPerformanceRandom2018_06 {

	
	public static final int maxSCIPThreads = 10;
	public static AtomicInteger currentSCIPThreads = new AtomicInteger(0);
	
	
	public static void main(String[] args) {
		
		InstanceLP.verbosity = 2;
		Simulation.verbosity = 0;
		
		int nThreads = 20;
		System.out.println("AppComparisonPerformanceForecasting::main Init with nThreads=" + nThreads + "...");
		Options.set(24, 15, 0.85);
		
		/*double fuseSize = 300;
		
		CONSTANTS.FUSE_LEVEL_0_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_1_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_2_SIZE = fuseSize;*/
		
		List<PerformanceMeasurementRandom2018_06> measurementJobs = new ArrayList<>();
		// Fill list of jobs

		//int[] nCarsParams = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}; 
		//int[] nCarsParams = new int[] {30};
		int[] nCarsParams = IntStream.range(1, 100+1).toArray();
		//int[] nChargingStationsParams = new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
		int[] nChargingStationsParams = new int[] {5};
		
		
		int[] seeds = IntStream.range(0, 19+1).toArray();
		
		System.out.println("Running nCarsParams=" + nCarsParams.length + ", nChargingStations=" + nChargingStationsParams.length + ", seeds=" + seeds.length);
		
		for (int nCars : nCarsParams) {
			for (int nChargingStations: nChargingStationsParams) {
				for (int seed : seeds) {
					PerformanceMeasurementRandom2018_06 job = new PerformanceMeasurementRandom2018_06(nCars, nChargingStations, seed);
					measurementJobs.add(job);
				}
			}
		}
		//PerformanceMeasurementForecasting job = new PerformanceMeasurementForecasting(50, "forecastXGBoost", 0);
		//measurementJobs.add(job);
		
		Collections.shuffle(measurementJobs); // Shuffle so that jobs are not done round robin but randomly
		
		// Stream jobs in parallel
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "" + nThreads);
		SQLiteDB db = new SQLiteDB("jdbc:sqlite:gen/performanceMeasurements.db");
		//measurementJobs.parallelStream().forEach(new MeasurementExecutorRandom2018_06(db, false));	
		measurementJobs.stream().forEach(new MeasurementExecutorRandom2018_06(db, false));	
	}

}
