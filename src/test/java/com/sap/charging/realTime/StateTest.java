package com.sap.charging.realTime;

import static org.junit.Assert.assertEquals;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;


import com.sap.charging.dataGeneration.DataGeneratorFromFile;
import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.opt.InstanceEmpty;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.JSONKeys;

public class StateTest {

	
	@SuppressWarnings("unchecked")
	@Test
	public void testStateToJSON() {
		
		Simulation.verbosity = 0; 
		
		DataGeneratorRandom data = new DataGeneratorRandom(0, false);
		data.generateEnergyPriceHistory(96)
			.generateChargingStations(20)
			.generateCars(40)
			.generateFuseTree(5, true);
		
		Strategy strategy = new StrategyAlgorithmic();
		
		Simulation sim = new Simulation(data, strategy);
		sim.init();
		
		for (int i=0;i<40000;i++) {
			sim.simulateNextStep();
		}
		
		// Complete export
		InstanceEmpty instance = new InstanceEmpty(data);
		JSONObject jsonProblemInstance = instance.toJSONObject();
		JSONObject jsonState = sim.getState().toJSONObject();
		
		JSONObject export = new JSONObject();
		export.put(JSONKeys.JSON_KEY_PROBLEM_INSTANCE, jsonProblemInstance);
		export.put(JSONKeys.JSON_KEY_STATE, jsonState);
		
		//ileIO.writeFile("gen/export.json", export);
		
		
		
		DataGeneratorFromFile dataImport = new DataGeneratorFromFile(export);
		dataImport.generateAll();
		
		State state2 = State.fromPreviousData(jsonState, dataImport);
		
		JSONObject jsonState2 = state2.toJSONObject();
		
		//System.out.println(jsonState);
		//System.out.println(jsonState2);
		
		assertEquals(jsonState, jsonState2);
		
	}
	
}
