package com.sap.charging.util.performanceMeasurement.forecasting2018_11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.configuration.Options;
import com.sap.charging.util.sqlite.SQLiteDB;

public class AppComparisonPerformanceForecasting2018_11 {

	
	public static void main(String[] args) {
		Simulation.verbosity = 0;
		
		int nThreads = 30;
		System.out.println("AppComparisonPerformanceForecasting::main Init with nThreads=" + nThreads + "...");
		Options.set(24, 15, 0.85);
		
		double fuseSize = 300;
		
		CONSTANTS.FUSE_LEVEL_0_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_1_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_2_SIZE = fuseSize;
		
		List<PerformanceMeasurementForecasting2018_11> measurementJobs = new ArrayList<>();
		// Fill list of jobs

		int[] nCarsParams = new int[]{10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 
				210, 220, 230, 240, 250, 260, 270, 280, 290, 300,
				310, 320, 330, 340, 350, 360, 370, 380, 390, 400,
				410, 420, 430, 440, 450, 460, 470, 480, 490, 500}; //, 500, 600, 700, 800, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000}; // 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 
		int[] nChargingStationsParams = new int[] {50};
		
		
		String[] forecastingMethods = new String[]{"median51", "median60", "median69", "median78", "forecastLMOfGenerated", "forecastLMOfReal", "oracle"}; //"forecastXGBoost", 
		
		
		int[] seeds = IntStream.range(0, 20).toArray();
		
		System.out.println("Running nCarsParams=" + nCarsParams.length + ", forecastingMethods=" + forecastingMethods.length + ", seeds=" + seeds.length);
		
		for (int nCars : nCarsParams) {
			for (int nChargingStations : nChargingStationsParams) {
				for (String forecastingMethod : forecastingMethods) {
					for (int seed : seeds) {
						PerformanceMeasurementForecasting2018_11 job = new PerformanceMeasurementForecasting2018_11(nCars, nChargingStations, forecastingMethod, seed);
						measurementJobs.add(job);
					}
				}
			}
			
		}
		//PerformanceMeasurementForecasting job = new PerformanceMeasurementForecasting(50, "forecastXGBoost", 0);
		//measurementJobs.add(job);
		
		Collections.shuffle(measurementJobs); // Shuffle so that jobs are not done round robin but randomly
		
		// Stream jobs in parallel
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "" + nThreads);
		SQLiteDB db = new SQLiteDB("jdbc:sqlite:gen/performanceMeasurements.db");
		//measurementJobs.stream().forEach(new MeasurementExecutor(db));
		//measurementJobs.stream().forEach(new MeasurementExecutorINDIN2018(db));
		measurementJobs.parallelStream().forEach(new MeasurementExecutorForecasting2018_11(db, false));	
	}
	
}
