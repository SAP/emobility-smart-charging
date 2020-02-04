package com.sap.charging.util.performanceMeasurement.paperJournal2020NonlinearCharging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.configuration.Options;
import com.sap.charging.util.sqlite.SQLiteDB;

public class AppComparisonPerformancePaperJournal2020NonlinearCharging {

	public static int verbosity = 2;
	public static AtomicInteger currentSimulationIndex = new AtomicInteger(0);
	public static int totalNumberOfSimulations = -1;

	public static void main(String[] args) {
		Simulation.verbosity = 1;

		int nThreads = 5;
		System.out.println("AppComparisonPerformancePaperJournal2020NonlinearCharging::main Init with nThreads=" + nThreads + "...");
		Options.set(24, 15, 0.85);

		double fuseSize = 90;

		CONSTANTS.FUSE_LEVEL_0_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_1_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_2_SIZE = fuseSize;

		List<PerformanceMeasurementPaperJournal2020NonlinearCharging> measurementJobs = new ArrayList<>();

		int[] nCarsParams = Arrays.stream(IntStream.range(21, 41).toArray()).map(x -> x*10).toArray(); // NORMAL OPERATION
		//int[] nCarsParams = Arrays.stream(IntStream.range(15, 16).toArray()).map(x -> x*10).toArray(); // SENSITIVITY ANALYIS
		//int[] nCarsParams = Arrays.stream(IntStream.range(1, 2).toArray()).toArray(); // TESTING
		
		int[] nChargingStationsParams = new int[] { -1 }; // Constant

		boolean[] rescheduleCarsWith0AParams = new boolean[] {
				false
		};
		
		String[] methods = new String[] { 
				//"uncoordinated", 
				//StrategyAlgorithmic.getMethodWithoutScheduleStatic() + "Linear", 
				StrategyAlgorithmic.getMethodWithoutScheduleStatic() + "Nonlinear"
				};
		//String[] methods = new String[] { StrategyAlgorithmic.getMethodWithoutScheduleStatic() + "Nonlinear" };

		//boolean[] nonlinearChargingParams = new boolean[] { false, true };

		//double[] startSoCParams = new double[] { 0.2, 0.8 }; // NORMAL OPERATION (Graph over  nCars)
		//double[] startSoCParams = new double[] { 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9 }; // sensitivity analysis (graph over initial SoC)
		double[] startSoCParams = new double[] { 0.8 }; // COMPUTATION TIME and TESTING
		
		
		int[] seeds = IntStream.range(1, 2).toArray(); // Variable
		

		int total = nCarsParams.length * nChargingStationsParams.length * seeds.length * methods.length * startSoCParams.length * rescheduleCarsWith0AParams.length;
		totalNumberOfSimulations = total;
		System.out.println("Running total=" + total);

		for (int seed : seeds) {
			
			for (double startSoC : startSoCParams) {
				for (int nCars : nCarsParams) {
					for (boolean rescheduleCarsWith0A : rescheduleCarsWith0AParams) {
						for (String method : methods) {
							PerformanceMeasurementPaperJournal2020NonlinearCharging job = new PerformanceMeasurementPaperJournal2020NonlinearCharging(
									method, nCars, nCars, rescheduleCarsWith0A, startSoC, fuseSize, seed);
							measurementJobs.add(job);
						}
					}

				}
			}

		}

		// Collections.shuffle(measurementJobs); // Shuffle so that jobs are not done
		// round robin but randomly

		// Stream jobs in parallel
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "" + nThreads);
		SQLiteDB db = new SQLiteDB("jdbc:sqlite:gen/performanceMeasurements.db");
		measurementJobs.parallelStream().forEach(new MeasurementExecutorPaperJournal2020NonlinearCharging(db, false));
		//measurementJobs.stream().forEach(new MeasurementExecutorPaperJournal2020NonlinearCharging(db, false));

		System.out.println("Ran total=" + total);
	}

	public static synchronized String getSimulationIndexString() {
		return currentSimulationIndex.get() + "/" + totalNumberOfSimulations;
	}

}
