package com.sap.charging.util.performanceMeasurement.paperINDIN2018;

import java.util.ArrayList;
import java.util.List;

import com.sap.charging.sim.Simulation;
import com.sap.charging.util.configuration.Options;

public class AppComparisonPerformanceINDIN2018 {

	public static void main(String[] args) {
		Simulation.verbosity = 1;
		
		int nThreads = 30;
		System.out.println("AppComparisonPerformanceINDIN2018::main Init with nThreads=" + nThreads + "...");
		Options.set(24, 15, 0.85);
		
		List<PerformanceMeasurementINDIN2018Template> measurementJobs = new ArrayList<>();
		// Fill list of jobs

		//int[] nCarsParams = new int[]{}; // 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1600, 
		List<Integer> nCarsParams = new ArrayList<>();
		for (int i=2100;i<=10000;i=i+100) {
			nCarsParams.add(i);
		}
		double[] proportionEVsParams = new double[]{0.75};
		double[] gridConnectionParams = new double[]{1450};
		
		for (int nCars : nCarsParams) {
			for (double proportionEVs : proportionEVsParams) {
				for (double gridConnection : gridConnectionParams) {
					measurementJobs.add(new PerformanceMeasurementINDIN2018Template(nCars, proportionEVs, gridConnection));
				}
			}
		}
		//measurementJobs.add(new PerformanceMeasurementINDIN2018Template(100, 0.75, 4000.2));
		
		// Stream jobs in parallel
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "" + nThreads);
		DBINDIN2018 db = new DBINDIN2018();
		//measurementJobs.stream().forEach(new MeasurementExecutor(db));
		//measurementJobs.stream().forEach(new MeasurementExecutorINDIN2018(db));
		measurementJobs.parallelStream().forEach(new MeasurementExecutorINDIN2018(db));
		
			
	}

}
