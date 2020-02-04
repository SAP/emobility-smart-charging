package com.sap.charging;

import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.realTime.Strategy;
import com.sap.charging.realTime.StrategyGreedy;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.random.Distribution;
import com.sap.charging.util.random.NormalDistribution;

public class AppProblemInstanceExportJSON {
	
	public static void main(String[] args) {
		Simulation.verbosity = 1;
		
		int seed = 0;
		
		double fuseSize = 1000000;
		
		CONSTANTS.FUSE_LEVEL_0_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_1_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_2_SIZE = fuseSize;
		
		
		
		DataGeneratorRandom data = new DataGeneratorRandom(seed, false);
		
		data.setIdealCars(true);
		data.setIdealChargingStations(true);
		
		Distribution normalDistribution = new NormalDistribution(data.getRandom(), 0.2, 0.2/3);
		data.setCurCapacityDistribution(normalDistribution);
		
		
		data.generateEnergyPriceHistory(96)
			.generateCars(20000)
			.generateChargingStations(100)
			.generateFuseTree(50, true);
			
		double[] energyPrices = data.getEnergyPriceHistory().getPrices();
		for (int i=0;i<energyPrices.length;i++) 
			energyPrices[i] = 100-i/2;
	
		//Strategy strategy = new StrategyAlgorithmic(new CarDepartureForecastLinearModel(45967.9706, 0.2767));
		Strategy strategy = new StrategyGreedy();
		//Strategy strategy = new StrategyAlgorithmic(new CarDepartureOracle());
		
		Simulation sim = new Simulation(data, strategy);
		sim.init();
		sim.simulate();
			
		FileIO.writeFile("gen/data/problemInstanceData.json", sim.getSimulationResult().getSolvedProblemInstanceJSON());
		
	}
	
	
}
