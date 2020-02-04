package com.sap.charging.opt;

import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.FuseTree;
import com.sap.charging.opt.util.MethodTimerState;
import com.sap.charging.util.JSONKeys;
import com.sap.charging.util.JSONSerializable;

public abstract class Instance implements JSONSerializable {

	protected List<Car> cars;
	protected List<ChargingStation> chargingStations;
	protected EnergyPriceHistory energyPriceHistory;
	protected FuseTree fuseTree;

    public MethodTimerState timeProblemConstruction;
	public MethodTimerState timeSolution;
	
	public Instance(
			@JsonProperty("cars") List<Car> cars, 
			@JsonProperty("chargingStations") List<ChargingStation> chargingStations, 
			@JsonProperty("energyPriceHistory") EnergyPriceHistory energyPriceHistory,
			@JsonProperty("fuseTree") FuseTree fuseTree) {
		this.cars = cars;
		this.chargingStations = chargingStations;
		this.energyPriceHistory = energyPriceHistory;
		this.fuseTree = fuseTree;
		
		this.timeProblemConstruction = new MethodTimerState();
		this.timeSolution = new MethodTimerState();
	}

	/**
	 * Overloaded constructor calling the other
	 * @param data
	 */
	public Instance(DataGenerator data) {
		this(data.getCars(),
				data.getChargingStations(),
				data.getEnergyPriceHistory(),
				data.getFuseTree());
	}
	
	public List<ChargingStation> getChargingStations() {
		return this.chargingStations;
	}

	public List<Car> getCars() {
		return this.cars;
	}

	public EnergyPriceHistory getEnergyPriceHistory() {
		return this.energyPriceHistory;
	}

	public FuseTree getFuseTree() {
		return this.fuseTree;
	}
	
	
	public int getEarliestCarTimeslot() {
		int earliestTimeslot = Integer.MAX_VALUE;
		for (Car car : cars) {
			if (car.getFirstAvailableTimeslot() < earliestTimeslot) 
				earliestTimeslot = car.getFirstAvailableTimeslot();
		}
		return earliestTimeslot;
	}
	
	public int getLatestCarTimeslot() {
		int latestTimeslot = -1;
		for (Car car : cars) {
			if (car.getLastAvailableTimeslot() > latestTimeslot)
				latestTimeslot = car.getLastAvailableTimeslot();
		}
		return latestTimeslot;
	}
	
	/**
	 * Assignment of car to charging station: for variable X_{i,n} of charging station (i) to car (n)
	 * @param indexI
	 * @param indexN
	 * @param value
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public JSONObject buildJSONVariableX(int indexI, int indexN, int value) {
		JSONObject variableX = new JSONObject();
		String name = "X_i" + indexI + "_n" + indexN;
		variableX.put(JSONKeys.JSON_KEY_VARIABLE_NAME, name);
		variableX.put(JSONKeys.JSON_KEY_VARIABLE_VALUE, value);
		return variableX;
	}
	
	/**
	 * Power for charging station (i) on phase (j) during timeslot (k)
	 * @param indexI
	 * @param indexJ
	 * @param indexK
	 * @param value
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public JSONObject buildJSONVariableP(int indexI, int indexJ, int indexK, double value) {
		JSONObject variableP = new JSONObject();
		String name = "P_i" + indexI + "_j" + indexJ + "_k" + indexK;
		variableP.put(JSONKeys.JSON_KEY_VARIABLE_NAME, name);
		variableP.put(JSONKeys.JSON_KEY_VARIABLE_VALUE, value);
		return variableP;
	}
	
	
	/**
	 * Constructs a JSON version of the original problem instance's parameters
	 * (indepedent of which method is being used to solve it)
	 * JSON key that should be used: problemInstance
	 * @param instance
	 * @return
	 */
	@Override
	@SuppressWarnings("unchecked")
	public JSONObject toJSONObject() {
		JSONObject problemInstance = new JSONObject();
		
		// EnergyPriceHistory
		problemInstance.put(JSONKeys.JSON_KEY_ENERGY_PRICE_HISTORY, this.getEnergyPriceHistory().toJSONObject());
		
		// Charging stations
		JSONArray chargingStationsJSON = new JSONArray();
		for (ChargingStation chargingStation : this.getChargingStations()) {
			chargingStationsJSON.add(chargingStation.toJSONObject());
		}
		problemInstance.put(JSONKeys.JSON_KEY_CHARGING_STATIONS, chargingStationsJSON);
		
		// Cars
		JSONArray carsJSON = new JSONArray();
		for (Car car : this.getCars()) {
			carsJSON.add(car.toJSONObject());
		}
		problemInstance.put(JSONKeys.JSON_KEY_CARS, carsJSON);
		
		// Fuse tree
		problemInstance.put(JSONKeys.JSON_KEY_FUSE_TREE, this.getFuseTree().toJSONObject());
		
		return problemInstance;
	}

	/**
	 * Returns a JSON object of JSONKeys, problemInstance and solution
	 * Assignments: 
	 * car 2 -> charging station 0
	 * car 1 -> charging station 1
	 * car 0 -> charging station 2
	 * 
	 * Power (in amps) per charging station (i) per phase (j) per timeslot (k):
	 * 
	 * {"variables": 
	 * 	[
	 * 		{"variableName": "X_i0_n2", "variableValue": 1},
	 *  	{"variableName": "X_i1_n1", "variableValue": 1},
	 *  	{"variableName": "X_i2_n0", "variableValue": 1},
	 *  	{"variableName": "P_i0_j1_k1", "variableValue": 32},
	 *  	{"variableName": "P_i0_j2_k1", "variableValue": 32},
	 *  	{"variableName": "P_i0_j3_k1", "variableValue": 32},
	 *  	{"variableName": "P_i0_j1_k2", "variableValue": 28},
	 *  	{"variableName": "P_i0_j2_k2", "variableValue": 28},
	 *  	{"variableName": "P_i0_j3_k2", "variableValue": 28},
	 *  	...
	 * 	]   
	 * }
	 * @param pathInputFileSolution
	 * @param pathOutputFileJSON
	 * @return
	 */
	protected abstract JSONObject getSolutionJSON();
	
	
	
	
	@SuppressWarnings("unchecked")
	public JSONObject getSolvedProblemInstanceJSON() {
		JSONObject result = new JSONObject();
		
		JSONObject solution = this.getSolutionJSON();
		solution.put(JSONKeys.JSON_KEY_TIME_PROBLEM_CONSTRUCTION, this.timeProblemConstruction.getTime());
		solution.put(JSONKeys.JSON_KEY_TIME_SOLUTION, this.timeSolution.getTime());
		solution.put(JSONKeys.JSON_KEY_TIME_SOLUTION_ARRAY, this.timeSolution.getExecutionTimes());
		solution.put(JSONKeys.JSON_KEY_METHOD, this.getMethod());
		result.put(JSONKeys.JSON_KEY_SOLUTION, solution);
		
		JSONObject problemInstance = this.toJSONObject();
		result.put(JSONKeys.JSON_KEY_PROBLEM_INSTANCE, problemInstance);
		
		result.put("KEYS", JSONKeys.exportConstants());
		
		return result;
	}
	
	
	public abstract String getMethod();
	
	
	
	
	
	
}