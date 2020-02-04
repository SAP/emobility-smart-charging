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
import com.sap.charging.opt.lp.InstanceLP;
import com.sap.charging.opt.lp.util.SolverSCIP;
import com.sap.charging.opt.solution.model.DayaheadSchedule;
import com.sap.charging.realTime.StrategyFromDayahead;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.FileIO;

/**
 * LP for linear programming. Full knowledge of vehicle schedules is required.
 */
public class AppLP {

	public static void main(String[] args) throws IOException, InterruptedException {
		// Example: data from 18.10.2017 intraday germany, weighted average used
		// https://www.epexspot.com/en/market-data/intradaycontinuous/intraday-table/-/DE
		
		int seed = 0;
		boolean useToyExample = false;
		boolean doSolve = false;
		
		System.out.println("Using toy example: " + useToyExample);
		
		DataGenerator dataGenerator;
		if (useToyExample == true) {
			dataGenerator = new DataGeneratorDeterministic();
		}
		else {
			dataGenerator = new DataGeneratorRandom(seed, false);
		}

		int nTimeslots = 96;
		int nCars = 4;
		int nChargingStations = 5;
		int nChargingStationsToy = 3;
		boolean doRotatePhases = true;
		
		dataGenerator.generateEnergyPriceHistory(nTimeslots)
			.generateCars(nCars)
			.generateChargingStations(useToyExample ? nChargingStationsToy : nChargingStations)
			.generateFuseTree(100, doRotatePhases);
		
		
		EnergyPriceHistory energyPriceHistory = dataGenerator.getEnergyPriceHistory();
		List<Car> cars = dataGenerator.getCars();
		List<ChargingStation> chargingStations = dataGenerator.getChargingStations();
		FuseTree fuseTree = dataGenerator.getFuseTree();
		System.out.println(fuseTree);
		
		for (Car car : cars) {
			car.setIdealCar(true);
			//car.setCurrentCapacity(car.getMaxCapacity()*0.9);
			System.out.println(car);
		}
		
		System.out.println("Price history length: " + energyPriceHistory.getNTimeslots());
		System.out.println("Number of cars: " + cars.size());
		System.out.println("Number of charging stations: " + chargingStations.size());
		
		
		/*Car car1 = CarFactory.builder()
				.set(CarModel.NISSAN_LEAF_2016)
				.id(0)
				.currentCapacity(EnergyUtil.calculateIFromP(20, 1)) // max 30
				.availableTimeslots(0, 95, nTimeslots)
				.availableTimestamps(LocalTime.of(0, 0), LocalTime.of(0, 15))
				.carType(CarType.PHEV)
				.build();
		cars.remove(0);
		cars.add(car1);*/
		
		InstanceLP.verbosity = 3;
		InstanceLP instance = new InstanceLP(cars, chargingStations, energyPriceHistory, fuseTree);
		//instance.setNormalizingCoefficients(true);
		//System.out.println(instance.getCars().get(0));
		/*instance.objectiveFairShare.setWeight(100);
		instance.objectiveEnergyCosts.setWeight(1);
		instance.objectivePeakShaving.setWeight(1);
		instance.objectiveLoadImbalance.setWeight(1);*/
		instance.constructVariables();
		instance.constructProblem();
		
		/**
		 * Time solving the problem
		 */
		for (int iteration=0;iteration<1 && doSolve;iteration++) {
			long startTime = System.currentTimeMillis();
			instance.setSolver(new SolverSCIP());
			instance.solveProblem();
			
			JSONObject result = instance.getSolvedProblemInstanceJSON();
			FileIO.writeFile("vis/data/solution_lp.json", result);
			// CONSTANTS.PATH_GEN_SOLUTION_JSON
			
			long endTime = System.currentTimeMillis();
			double duration = (endTime - startTime) / 1000.0; 
			System.out.println(duration);
			
			Simulation.verbosity = 0;
			Simulation sim = new Simulation(dataGenerator, new StrategyFromDayahead(new DayaheadSchedule(instance.getSolvedProblemInstanceJSON())));
			//System.out.println(car1);
			sim.simulate();
			
		}
		
		
	}
	
}
















