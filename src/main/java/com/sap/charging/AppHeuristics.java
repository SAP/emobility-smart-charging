package com.sap.charging;

import java.io.IOException;
import java.util.List;

import org.json.simple.JSONObject;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorDeterministic;
import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.FuseTree;
import com.sap.charging.opt.heuristics.InstanceHeuristic;
import com.sap.charging.opt.heuristics.InstanceHeuristicGreedy;
import com.sap.charging.util.FileIO;

public class AppHeuristics {
	public static void main(String[] args) throws IOException, InterruptedException {
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

		int nTimeslots = 10;
		int nCars = 20;
		int nChargingStations = 10;
		int nChargingStationsToy = 9;
		boolean doRotatePhases = true;
		
		dataGenerator.generateEnergyPriceHistory(nTimeslots)
			.generateCars(nCars)
			.generateChargingStations(useToyExample ? nChargingStationsToy : nChargingStations)
			.generateFuseTree(20, doRotatePhases);
		
		
		EnergyPriceHistory energyPriceHistory = dataGenerator.getEnergyPriceHistory();
		List<Car> cars = dataGenerator.getCars();
		List<ChargingStation> chargingStations = dataGenerator.getChargingStations();
		FuseTree fuseTree = dataGenerator.getFuseTree();
		System.out.println(fuseTree);
		
		System.out.println("Price history length: " + energyPriceHistory.getNTimeslots());
		System.out.println("Number of cars: " + cars.size());
		System.out.println("Number of charging stations: " + chargingStations.size());
		
		
		InstanceHeuristic instance = new InstanceHeuristicGreedy(cars, chargingStations, energyPriceHistory, fuseTree);
		JSONObject result = instance.getSolvedProblemInstanceJSON();
		
		FileIO.writeFile("vis/data/solution_greedy.json", result);
	}
}
