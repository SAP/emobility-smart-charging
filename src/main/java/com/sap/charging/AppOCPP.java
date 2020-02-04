package com.sap.charging;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.realTime.Strategy;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.FileIO;

public class AppOCPP {
	
	
	public static void main(String[] args) {
		
		
		DataGenerator data = new DataGeneratorRandom(0, false);
		data.generateEnergyPriceHistory(96)
			.generateChargingStations(1)
			.generateCars(1)
			.generateFuseTree(999, true);
		Strategy strategy = new StrategyAlgorithmic();
		
		Simulation sim = new Simulation(data, strategy);
		sim.init();
		
		for (int i=0;i<40000;i++) {
			sim.simulateNextStep();
		}
		
		FileIO.writeFile("state.json", sim.getState().toJSONObject());
		
		
		
		
		
		
	}
	
}
