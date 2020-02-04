package com.sap.charging.util.performanceMeasurement.random;

import java.util.ArrayList;
import java.util.List;

public class AppComparisonPerformanceRandom {
	
	
	public static void main(String[] args) {
		System.out.println("AppComparisonPerformanceRandom::main Init...");
		
		/*int nThreads = 1;
		
		
		int nMin = 3;
		int nCars = 5;
		int nMax = 10; 
		
		int iMin = 3;
		int nChargingStations = 5;
		int iMax = 10;
		
		int kMin = 10;
		int nTimeslots = 10;
		int kMax = 15;*/
		
		int nIterations = 20;
		
		List<PerformanceMeasurementRandomTemplate> measurementJobs = new ArrayList<>();
		// Fill list of jobs
		
		/************
		 * With LP solving
		 ***********/
		// Increasing cars
		/*for (int n=nMin;n<=nMax;n++) { 
			for (int seed=0;seed<nIterations;seed++) {
				measurementJobs.add(new PerformanceMeasurementTemplate(n, 
						nChargingStations, nTimeslots, seed, true));
			}
		}
		// Increasing charging stations
		for (int i=iMin;i<=iMax;i++) {
			if (i == nChargingStations) {
				continue; // skip this loop since it is covered by first loop
			}
			for (int seed=0;seed<nIterations;seed++) {
				measurementJobs.add(new PerformanceMeasurementTemplate(nCars, 
						i, nTimeslots, seed, true));
			}
		}
		// Increasing timeslots
		for (int k=kMin;k<=kMax;k++) {
			for (int seed=0;seed<nIterations;seed++) {
				measurementJobs.add(new PerformanceMeasurementTemplate(nCars, 
						nChargingStations, k, seed, true));
			}
		}*/
		
		/*****************
		 * Without LP solving
		 *****************/
		/*for (int seed = 0; seed<nIterations;seed++) {
			measurementJobs.add(new PerformanceMeasurementTemplate(20, 10, 36, seed, false));
		}
		for (int seed = 0; seed<nIterations;seed++) {
			measurementJobs.add(new PerformanceMeasurementTemplate(30, 15, 36, seed, false));
		}
		for (int seed = 0; seed<nIterations;seed++) {
			measurementJobs.add(new PerformanceMeasurementTemplate(40, 20, 36, seed, false));
		}
		for (int seed = 0; seed<nIterations;seed++) {
			measurementJobs.add(new PerformanceMeasurementTemplate(50, 25, 36, seed, false));
		}
		for (int seed = 0; seed<nIterations;seed++) {
			measurementJobs.add(new PerformanceMeasurementTemplate(60, 30, 36, seed, false));
		}*/
		/*for (int seed = 0; seed<nIterations;seed++) {
			measurementJobs.add(new PerformanceMeasurementTemplate(70, 35, 36, seed, false));
		}*/
		for (int seed = 0; seed<nIterations;seed++) {
			measurementJobs.add(new PerformanceMeasurementRandomTemplate(80, 40, 36, seed, false));
		}
		/*for (int seed = 0; seed<nIterations;seed++) {
			measurementJobs.add(new PerformanceMeasurementTemplate(90, 45, 36, seed, false));
		}
		for (int seed = 0; seed<nIterations;seed++) {
			measurementJobs.add(new PerformanceMeasurementTemplate(100, 50, 36, seed, false));
		}*/
		
		
		
		
		
		System.out.println("Number of jobs:" + getMeasurementJobsSize(measurementJobs));
		
		// Stream jobs in parallel
		//System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "" + nThreads);
		DBRandom db = new DBRandom();
		//measurementJobs.stream().forEach(new MeasurementExecutor(db));
		measurementJobs.stream().forEach(new MeasurementExecutorRandom(db));
		
		
		/*for (PerformanceMeasurementTemplate measurement : measurementJobs) {
			MeasurementExecutor e = new MeasurementExecutor(db);
			e.accept(measurement);
		}*/	
		
		/*long start = System.currentTimeMillis();
		IntStream s = IntStream.range(0, 20);
		s.parallel().forEach(i -> {
		    try { Thread.sleep(100); } catch (Exception ignore) {}
		    System.out.print((System.currentTimeMillis() - start) + " ");
		});*/
		
	}
	
	private static int getMeasurementJobsSize(List<PerformanceMeasurementRandomTemplate> list) {
		int result = 0;
		for (PerformanceMeasurementRandomTemplate measurementJob : list) {
			result += measurementJob.getSolveLP() ? 4 : 3;
		}
		return result;
	}
	
}
























