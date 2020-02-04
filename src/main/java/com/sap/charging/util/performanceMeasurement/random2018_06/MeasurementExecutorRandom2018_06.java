package com.sap.charging.util.performanceMeasurement.random2018_06;

import org.json.simple.JSONObject;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.opt.heuristics.InstanceHeuristicRelSoCLP;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.StrategyGreedy;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.performanceMeasurement.MeasurementExecutor;
import com.sap.charging.util.random.Distribution;
import com.sap.charging.util.random.NormalDistribution;
import com.sap.charging.util.sqlite.SQLiteDB;

public class MeasurementExecutorRandom2018_06 extends MeasurementExecutor<PerformanceMeasurementRandom2018_06> {

	public MeasurementExecutorRandom2018_06(SQLiteDB db) {
		super(db);
	}
	public MeasurementExecutorRandom2018_06(SQLiteDB db, boolean forceMeasurement) {
		super(db, forceMeasurement);
	}
	
	@Override
	public void accept(PerformanceMeasurementRandom2018_06 measurement) {

		DataGeneratorRandom data = new DataGeneratorRandom(measurement.seed, false);
		data.setIdealCars(true);
		data.setIdealChargingStations(true);
		
		Distribution normalDistribution = new NormalDistribution(data.getRandom(), 0.2, 0.2/3);
		data.setCurCapacityDistribution(normalDistribution);
		
		data.generateEnergyPriceHistory(96)
			.generateCars(measurement.nCars)
			.generateChargingStations(measurement.nCars)
			.generateFuseTree(measurement.nCars, true);
		
		
		DataGenerator dataForDayAhead = data.clone();
		InstanceHeuristicRelSoCLP instance = new InstanceHeuristicRelSoCLP(dataForDayAhead);
		instance.getInstanceLP().objectiveEnergyCosts.setWeight(0);
		instance.getInstanceLP().objectivePeakShaving.setWeight(1e4);
		PerformanceMeasurementRandom2018_06 measurementHeuristic = measurement.cloneWithMethod(instance.getMethod());
		if (forceMeasurement == true && db.rowExists(measurementHeuristic) == true) {
			db.deleteRow(measurementHeuristic);
		}
		if (db.rowExists(measurementHeuristic) == false && measurement.nCars <= 50) {
			System.out.println("Running measurement with nCars=" + measurement.nCars + ", nChargingStations=" + measurement.nChargingStations + ", seed=" + measurement.seed +
					 		   " in thread=" + Thread.currentThread().getId());
			
			
			instance.constructProblem();
			
			// Start SCIP in another thread! 
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					while (AppComparisonPerformanceRandom2018_06.currentSCIPThreads.get() >= AppComparisonPerformanceRandom2018_06.maxSCIPThreads) {
						try {
							//System.out.println("Waiting for SCIP threads to be available...");
							Thread.sleep(1000);
						} catch (InterruptedException e) { e.printStackTrace(); }
					}
					
					AppComparisonPerformanceRandom2018_06.currentSCIPThreads.incrementAndGet();
					
					System.out.println("Running final step in other thread...");
					
					JSONObject solvedProblemInstance = instance.getSolvedProblemInstanceJSON();
					db.insert(measurementHeuristic);
					FileIO.writeFile(measurementHeuristic.filePath, solvedProblemInstance);
					
					AppComparisonPerformanceRandom2018_06.currentSCIPThreads.decrementAndGet();
				}
			});
			
			thread.start(); // run is synchronous, start starts a new thread
			
		}
		else {
			System.out.println(measurementHeuristic.toString() + " already exists.");
		}
		
		
		PerformanceMeasurementRandom2018_06 measurementGreedy = measurement.cloneWithMethod(StrategyGreedy.getMethodStatic());
		if (forceMeasurement == true && db.rowExists(measurementGreedy) == true) {
			db.deleteRow(measurementGreedy);
		}
		if (db.rowExists(measurementGreedy) == false) {
			System.out.println("Running measurement with nCars=" + measurement.nCars + ", nChargingStations=" + measurement.nChargingStations + ", seed=" + measurement.seed +
					 		   " in thread=" + Thread.currentThread().getId());
			
			DataGenerator dataGreedy = data.clone();
			
			StrategyGreedy strategyGreedy = new StrategyGreedy();
			Simulation simGreedy = new Simulation(dataGreedy, strategyGreedy);
			simGreedy.init();
			simGreedy.simulate();
			
			JSONObject solvedProblemInstance = simGreedy.getSimulationResult().getSolvedProblemInstanceJSON();
			
			db.insert(measurementGreedy);
			FileIO.writeFile(measurementGreedy.filePath, solvedProblemInstance);
		}
		else {
			System.out.println(measurementGreedy.toString() + " already exists.");
		}
		
		
		
		
		
		PerformanceMeasurementRandom2018_06 measurementAlgorithmic = measurement.cloneWithMethod(StrategyAlgorithmic.getMethodWithoutScheduleStatic());
		if (forceMeasurement == true && db.rowExists(measurementAlgorithmic) == true) {
			db.deleteRow(measurementAlgorithmic);
		}
		if (db.rowExists(measurementAlgorithmic) == false) {
			System.out.println("Running measurement with nCars=" + measurement.nCars + ", nChargingStations=" + measurement.nChargingStations + ", seed=" + measurement.seed +
					 		   " in thread=" + Thread.currentThread().getId());
			
			DataGenerator dataAlgorithmic = data.clone();
			
			StrategyAlgorithmic strategyAlgorithmic = new StrategyAlgorithmic();
			Simulation simAlgorithmic = new Simulation(dataAlgorithmic, strategyAlgorithmic);
			simAlgorithmic.init();
			simAlgorithmic.simulate();
			
			JSONObject solvedProblemInstance = simAlgorithmic.getSimulationResult().getSolvedProblemInstanceJSON();
			
			db.insert(measurementAlgorithmic);
			FileIO.writeFile(measurementAlgorithmic.filePath, solvedProblemInstance);
		}
		else {
			System.out.println(measurementAlgorithmic.toString() + " already exists.");
		}
		
		
		
		
		
		
		
	}

	
	
	
}
