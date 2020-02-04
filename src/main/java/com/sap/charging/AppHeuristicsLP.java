package com.sap.charging;

import java.util.List;

import org.json.simple.JSONObject;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorDeterministic;
import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.opt.heuristics.InstanceHeuristicGreedyLP;
import com.sap.charging.util.FileIO;

public class AppHeuristicsLP {
	
	public static void main(String[] args) {
		int seed = 0;
		boolean useToyExample = false;
		System.out.println("Using toy example: " + useToyExample);

		DataGenerator dataGenerator;
		if (useToyExample == true) {
			dataGenerator = new DataGeneratorDeterministic();
		}
		else {
			dataGenerator = new DataGeneratorRandom(seed, false);
		}

		int nTimeslots = 96;
		int nCars = 50;
		int nChargingStations = 25;
		int nChargingStationsToy = 9;
		boolean doRotatePhases = true;
		
		dataGenerator.generateEnergyPriceHistory(nTimeslots)
			.generateCars(nCars)
			.generateChargingStations(useToyExample ? nChargingStationsToy : nChargingStations)
			.generateFuseTree(100, doRotatePhases);
		
		dataGenerator.setIdealCars(true);
		dataGenerator.setIdealChargingStations(true);
		
		EnergyPriceHistory energyPriceHistory = dataGenerator.getEnergyPriceHistory();
		List<Car> cars = dataGenerator.getCars();
		List<ChargingStation> chargingStations = dataGenerator.getChargingStations();
		//FuseTree fuseTree = dataGenerator.getFuseTree();

		for (Car car : cars) {
			car.setIdealCar(true);
			System.out.println(car);
		}
		
		System.out.println("Price history length: " + energyPriceHistory.getNTimeslots());
		System.out.println("Number of cars: " + cars.size());
		System.out.println("Number of charging stations: " + chargingStations.size());
		
		
		InstanceHeuristicGreedyLP instance = new InstanceHeuristicGreedyLP(dataGenerator);
		//instance.getInstanceLP().objectiveEnergyCosts.setWeight(0);
		//instance.getInstanceLP().objectiveLoadImbalance.setWeight(1e4);
		
		
		JSONObject result = instance.getSolvedProblemInstanceJSON();
		FileIO.writeFile("vis/data/solution_greedyLP.json", result);
	}
	
}
