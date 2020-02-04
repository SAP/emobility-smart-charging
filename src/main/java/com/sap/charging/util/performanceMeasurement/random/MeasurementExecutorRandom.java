package com.sap.charging.util.performanceMeasurement.random;

import java.util.List;
import java.util.function.Consumer;

import org.json.simple.JSONObject;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.FuseTree;
import com.sap.charging.opt.heuristics.InstanceHeuristic;
import com.sap.charging.opt.heuristics.InstanceHeuristicAbsSoCLP;
import com.sap.charging.opt.heuristics.InstanceHeuristicGreedy;
import com.sap.charging.opt.heuristics.InstanceHeuristicGreedyLP;
import com.sap.charging.opt.lp.InstanceLP;
import com.sap.charging.opt.lp.util.SolverSCIP;

public class MeasurementExecutorRandom implements Consumer<PerformanceMeasurementRandomTemplate> {

	private DBRandom db;
	public MeasurementExecutorRandom(DBRandom db) {
		this.db = db;
	}
	
	@Override
	public void accept(PerformanceMeasurementRandomTemplate measurement) {
		DataGenerator dataGenerator = new DataGeneratorRandom(measurement.getSeed(), false);
		
		dataGenerator.generateEnergyPriceHistory(measurement.getNTimeslots())
			.generateCars(measurement.getNCars())
			.generateChargingStations(measurement.getNChargingStations())
			.generateFuseTree(9, true); // max charging stations = 
		
		EnergyPriceHistory energyPriceHistory = dataGenerator.getEnergyPriceHistory();
		List<Car> cars = dataGenerator.getCars();
		
		// Create idealized list of cars
		for (Car car : cars) {
			car.setIdealCar(true);
		}
		
		List<ChargingStation> chargingStations = dataGenerator.getChargingStations();
		FuseTree fuseTree = dataGenerator.getFuseTree();
		
		System.out.println("========== In iteration seed=" + measurement.getSeed() + ": " + 
				cars.size() + " cars, " + 
				chargingStations.size() + " charging stations, " + 
				energyPriceHistory.getNTimeslots() + " timeslots =========");
		
		System.out.println("---------- Pure LP ----------");
		InstanceLP instanceLP = new InstanceLP(cars, chargingStations, energyPriceHistory, fuseTree);
		PerformanceMeasurementRandom measurementLP = measurement.cloneWithMethod(instanceLP.getMethod());
		if (!db.measurementExists(measurementLP) && measurementLP.getSolveLP() == true) {
			instanceLP.constructVariables();
			instanceLP.constructProblem();
			
			instanceLP.setSolver(new SolverSCIP());
			instanceLP.solveProblem();
			
			JSONObject resultLP = instanceLP.getSolvedProblemInstanceJSON();
			db.insertMeasurement(measurementLP, resultLP);
		}

		System.out.println("---------- Greedy ----------");
		InstanceHeuristic instanceGreedy = new InstanceHeuristicGreedy(cars, chargingStations, energyPriceHistory, fuseTree);
		PerformanceMeasurementRandom measurementGreedy = measurement.cloneWithMethod(instanceGreedy.getMethod());
		if (!db.measurementExists(measurementGreedy)) {
			JSONObject resultGreedy = instanceGreedy.getSolvedProblemInstanceJSON();
			db.insertMeasurement(measurementGreedy, resultGreedy);
		}

		System.out.println("---------- Greedy+LP -----------");
		InstanceHeuristicGreedyLP instanceGreedyLP = new InstanceHeuristicGreedyLP(cars, chargingStations, energyPriceHistory, fuseTree);
		PerformanceMeasurementRandom measurementGreedyLP = measurement.cloneWithMethod(instanceGreedyLP.getMethod());
		if (!db.measurementExists(measurementGreedyLP)) {
			JSONObject resultGreedyLP = instanceGreedyLP.getSolvedProblemInstanceJSON();
			db.insertMeasurement(measurementGreedyLP, resultGreedyLP);
		}
		
		System.out.println("---------- AbsSoC+LP -----------");
		InstanceHeuristicAbsSoCLP instanceAbsSoCLP = new InstanceHeuristicAbsSoCLP(cars, chargingStations, energyPriceHistory, fuseTree);
		PerformanceMeasurementRandom measurementAbsSoCLP = measurement.cloneWithMethod(instanceAbsSoCLP.getMethod());
		if (!db.measurementExists(measurementAbsSoCLP)) {
			JSONObject resultAbsSoCLP = instanceAbsSoCLP.getSolvedProblemInstanceJSON();
			db.insertMeasurement(measurementAbsSoCLP, resultAbsSoCLP);
		}
		
	}

}
