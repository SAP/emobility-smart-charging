package com.sap.charging.opt.lp;

import static com.sap.charging.util.JSONKeys.JSON_KEY_CARS;
import static com.sap.charging.util.JSONKeys.JSON_KEY_PROBLEM_INSTANCE;
import static com.sap.charging.util.JSONKeys.JSON_KEY_SOLUTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorReal;
import com.sap.charging.dataGeneration.common.DefaultDataGenerator;
import com.sap.charging.opt.lp.util.SolverSCIP;
import com.sap.charging.util.JSONKeys;

public class InstanceLPMinPriceTest {

	@BeforeEach
	public void setup() {
		InstanceLP.verbosity = 0; 
	}
	
	//@Test
	public void testWrongOrder() {
		
		DataGenerator data = DefaultDataGenerator.getToyDataGenerator();
		InstanceLP instanceLP = new InstanceLP(data);
		
		try {
			instanceLP.constructProblem();
			fail("This should have thrown a NullPointerException");
		}
		catch (Exception e) {
			assertTrue(e instanceof NullPointerException);
		}
		
	}
	@Disabled
	@Test
	public void testCompleteExampleRandom() {
		DataGenerator data = DefaultDataGenerator.getToyDataGenerator();
		
		InstanceLP instanceLP = new InstanceLP(data);
		
		
		instanceLP.objectiveEnergyCosts.setWeight(0);
		instanceLP.objectiveLoadImbalance.setWeight(0);
		
		instanceLP.constructVariables();
		
		// Check variables constructed
		int nVariablesXMin = data.getCars().size()*data.getChargingStations().size();
		int nVariablesPMin = data.getChargingStations().size()*3*data.getEnergyPriceHistory().getNTimeslots();
		assertEquals(nVariablesXMin,
				     instanceLP.getVariablesX().values().size());
		assertEquals(nVariablesPMin, 
					 instanceLP.getVariablesP().values().size());
	
		
		instanceLP.constructProblem();
		instanceLP.setSolver(new SolverSCIP());
		instanceLP.solveProblem();
		
		// Check result JSON
		JSONObject result = instanceLP.getSolvedProblemInstanceJSON();
		JSONObject problemInstance = (JSONObject) result.get(JSON_KEY_PROBLEM_INSTANCE);
		JSONArray cars = (JSONArray) problemInstance.get(JSON_KEY_CARS);
		JSONArray chargingStations = (JSONArray) problemInstance.get(JSONKeys.JSON_KEY_CHARGING_STATIONS);
		JSONObject energyPriceHistory = (JSONObject) problemInstance.get(JSONKeys.JSON_KEY_ENERGY_PRICE_HISTORY);
		JSONArray energyPrices = (JSONArray) energyPriceHistory.get(JSONKeys.JSON_KEY_ENERGY_PRICES);
		
		assertEquals(data.getCars().size(), cars.size());
		assertEquals(data.getChargingStations().size(), chargingStations.size());
		assertEquals(data.getEnergyPriceHistory().getNTimeslots(), energyPrices.size());
		
		JSONObject solution = (JSONObject) result.get(JSON_KEY_SOLUTION);
		assertNotNull(solution.get(JSONKeys.JSON_KEY_TIME_PROBLEM_CONSTRUCTION));
		assertNotNull(solution.get(JSONKeys.JSON_KEY_TIME_SOLUTION));
		
		JSONArray variables = (JSONArray) solution.get(JSONKeys.JSON_KEY_VARIABLES);
		assertTrue(variables.size() >= (nVariablesXMin+nVariablesPMin));
		
		//FileIO.writeFile("vis/data/solution_lp.json", instanceLP.getSolvedProblemInstanceJSON());
	}
	
	//@Test
	public void testCompleteExampleReal() {
		DataGeneratorReal data = new DataGeneratorReal("2016-12-03", 0, true, true);
		data.generateAll();
		data.getCars().get(0).setIdealCar(true);
		InstanceLP instanceLP = new InstanceLP(data);
		instanceLP.constructVariables();
		instanceLP.constructProblem();
		instanceLP.setSolver(new SolverSCIP());
		instanceLP.solveProblem();
		
		JSONObject result = instanceLP.getSolvedProblemInstanceJSON();
		JSONObject solution = (JSONObject) result.get(JSON_KEY_SOLUTION);
		assertNotNull(solution.get(JSONKeys.JSON_KEY_TIME_PROBLEM_CONSTRUCTION));
		assertNotNull(solution.get(JSONKeys.JSON_KEY_TIME_SOLUTION));
		
		JSONArray variables = (JSONArray) solution.get(JSONKeys.JSON_KEY_VARIABLES);
		// Check that the single car is assigned to the single charging station
		boolean isSet = false;
		for (Object variableJSON : variables) {
			Variable variable = Variable.fromJSON((JSONObject) variableJSON);
			//System.out.println(variable);
			if (variable.getName().equals("X")) {
				assertEquals(1, variable.getValue(), 1e-8);
				isSet = true;
			}
		}
		assertTrue(isSet);
		
	}

}











