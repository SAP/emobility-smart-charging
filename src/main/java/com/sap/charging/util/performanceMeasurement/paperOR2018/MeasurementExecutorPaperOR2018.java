package com.sap.charging.util.performanceMeasurement.paperOR2018;

import org.json.simple.JSONObject;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.opt.heuristics.InstanceHeuristicRelSoCLP;
import com.sap.charging.opt.solution.model.DayaheadSchedule;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.StrategyGreedy;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.performanceMeasurement.MeasurementExecutor;
import com.sap.charging.util.random.Distribution;
import com.sap.charging.util.random.NormalDistribution;
import com.sap.charging.util.sqlite.SQLiteDB;

public class MeasurementExecutorPaperOR2018 extends MeasurementExecutor<PerformanceMeasurementPaperOR2018> {

	public MeasurementExecutorPaperOR2018(SQLiteDB db) {
		super(db);
	}
	public MeasurementExecutorPaperOR2018(SQLiteDB db, boolean forceMeasurement) {
		super(db, forceMeasurement);
	}
	
	@Override
	public void accept(PerformanceMeasurementPaperOR2018 measurement) {

		DataGeneratorRandom data = new DataGeneratorRandom(measurement.seed, false);
		data.setIdealCars(true);
		data.setIdealChargingStations(true);
		
		Distribution normalDistribution = new NormalDistribution(data.getRandom(), 0.2, 0.2/3);
		data.setCurCapacityDistribution(normalDistribution);
		
		data.generateEnergyPriceHistory(96)
			.generateCars(measurement.nCars)
			.generateChargingStations(measurement.nCars)
			.generateFuseTree(measurement.nCars, true);
		
		
		PerformanceMeasurementPaperOR2018 measurementGreedy = measurement.cloneWithMethod(StrategyGreedy.getMethodStatic());
		if (forceMeasurement == true && db.rowExists(measurementGreedy) == true) {
			db.deleteRow(measurementGreedy);
		}
		if (db.rowExists(measurementGreedy) == false) {
			DataGenerator dataForGreedy = data.clone();
			StrategyGreedy strategyGreedy = new StrategyGreedy();
			Simulation simGreedy = new Simulation(dataForGreedy, strategyGreedy);
			simGreedy.init();
			simGreedy.simulate();
			
			FileIO.writeFile(measurementGreedy.filePath, simGreedy.getSimulationResult().getSolvedProblemInstanceJSON());
			db.insert(measurementGreedy);
		}
		else {
			System.out.println(measurementGreedy.toString() + " already exists.");
		}
		
		
		PerformanceMeasurementPaperOR2018 measurementWithoutDayahead = measurement.cloneWithMethod(StrategyAlgorithmic.getMethodWithoutScheduleStatic());
		if (forceMeasurement == true && db.rowExists(measurementWithoutDayahead) == true) {
			db.deleteRow(measurementWithoutDayahead);
		}
		if (db.rowExists(measurementWithoutDayahead) == false) {
			DataGenerator dataForWithoutDayahead = data.clone();
			StrategyAlgorithmic strategyWithoutDayahead = new StrategyAlgorithmic();
			Simulation simWithoutDayahead = new Simulation(dataForWithoutDayahead, strategyWithoutDayahead);
			simWithoutDayahead.init();
			simWithoutDayahead.simulate();
			
			FileIO.writeFile(measurementWithoutDayahead.filePath, simWithoutDayahead.getSimulationResult().getSolvedProblemInstanceJSON());
			db.insert(measurementWithoutDayahead);
		}
		else {
			System.out.println(measurementWithoutDayahead.toString() + " already exists.");
		}
		
		
		PerformanceMeasurementPaperOR2018 measurementWithDayahead = measurement.cloneWithMethod(StrategyAlgorithmic.getMethodWithScheduleStatic());
		if (forceMeasurement == true && db.rowExists(measurementWithDayahead) == true) {
			db.deleteRow(measurementWithDayahead);
		}
		if (db.rowExists(measurementWithDayahead) == false) {
			System.out.println("Running measurement with nCars=" + measurement.nCars + ", nChargingStations=" + measurement.nChargingStations + ", seed=" + measurement.seed +
					 		   " in thread=" + Thread.currentThread().getId());
			
			DataGenerator dataForDayahead = data.clone();
			InstanceHeuristicRelSoCLP instance = new InstanceHeuristicRelSoCLP(dataForDayahead);
			instance.constructProblem();
			//instance.getInstanceLP().objectiveEnergyCosts.setWeight(0);
			//instance.getInstanceLP().objectivePeakShaving.setWeight(1e4);
			
			
			JSONObject scheduleDayahead = instance.getSolvedProblemInstanceJSON();
			StrategyAlgorithmic strategyWithDayahead = new StrategyAlgorithmic(new DayaheadSchedule(scheduleDayahead));
			
			Simulation simWithDayAhead = new Simulation(dataForDayahead, strategyWithDayahead);
			simWithDayAhead.init();
			simWithDayAhead.simulate();
			
			FileIO.writeFile(measurementWithDayahead.filePath, simWithDayAhead.getSimulationResult().getSolvedProblemInstanceJSON());
			db.insert(measurementWithDayahead);
		}
		else {
			System.out.println(measurementWithDayahead.toString() + " already exists.");
		}
		
		
		
		
		
		
		
		
		
	}
	
	
}
