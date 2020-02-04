package com.sap.charging.util.performanceMeasurement.paperJournal2019;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.configuration.Options;
import com.sap.charging.util.r.RConnector;
import com.sap.charging.util.sqlite.SQLiteDB;

public class AppComparisonPerformancePaperJournal2019 {

	public static int verbosity = 2;
	public static AtomicInteger currentSimulationIndex = new AtomicInteger(0);
	public static int totalNumberOfSimulations = -1;
	
	public static void main(String[] args) {
		Simulation.verbosity = 0;
		RConnector.verbosity = 0;
		
		int nThreads = 30;
		System.out.println("AppComparisonPerformancePaperJournal2019::main Init with nThreads=" + nThreads + "...");
		Options.set(24, 15, 0.85);
		
		double fuseSize = 900;
		
		CONSTANTS.FUSE_LEVEL_0_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_1_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_2_SIZE = fuseSize;
		
		List<PerformanceMeasurementPaperJournal2019> measurementJobs = new ArrayList<>();

		int[] nCarsParams = Arrays.stream(IntStream.range(1, 41).toArray()).map(x -> x*10).toArray();
		//int[] nCarsParams = Arrays.stream(IntStream.range(1, 2).toArray()).map(x -> x*10).toArray();
		int[] nChargingStationsParams = new int[] {-1}; // Constant 
		
		// forecastLM
		// medianTimestamp86398
		String[] forecastingMethods = new String[]{"uncoordinated", "oracle", "medianTimestamp61398", "medianTimestamp1", "forecastLMAll", "forecastXGBoost", "forecastNN"};  // 
		//String[] forecastingMethods = new String[]{"oracle"};  // 
		
		
		
		int[] seeds = IntStream.range(0, 10).toArray(); // Variable
		
		int total = nCarsParams.length*nChargingStationsParams.length*forecastingMethods.length*seeds.length;
		totalNumberOfSimulations = total;
		System.out.println("Running total=" + total);
		
		for (int nCars : nCarsParams) {
			for (int seed : seeds) {
				//System.out.println("Generating for nCars=" + nCars + ", seed=" + seed);
				//List<CarProcessData> carProcessData = CarProcessData.sampleHistoricalCarProcesses(nCars, seed);
				
				for (String forecastingMethod : forecastingMethods) {
					PerformanceMeasurementPaperJournal2019 job = new PerformanceMeasurementPaperJournal2019(nCars, nCars, forecastingMethod, fuseSize, seed);
					//job.setCarProcessData(carProcessData);
					measurementJobs.add(job);
				}
			}
			
		}
		
		//Collections.shuffle(measurementJobs); // Shuffle so that jobs are not done round robin but randomly
		
		// Stream jobs in parallel
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "" + nThreads);
		SQLiteDB db = new SQLiteDB("jdbc:sqlite:gen/performanceMeasurements.db");
		measurementJobs.parallelStream().forEach(new MeasurementExecutorPaperJournal2019(db, false));	
	
		System.out.println("Ran total=" + total);
		//measurementJobs.stream().forEach(new MeasurementExecutorPaperJournal2019(db, false));	
	}
	
	public static String getSimulationIndexString() {
		return currentSimulationIndex.get() +  "/" + totalNumberOfSimulations;
	}
	
}
