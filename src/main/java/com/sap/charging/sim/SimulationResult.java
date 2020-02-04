package com.sap.charging.sim;

import java.util.HashMap;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.opt.Instance;
import com.sap.charging.opt.lp.Variable;
import com.sap.charging.realTime.State;
import com.sap.charging.realTime.Strategy;
import com.sap.charging.realTime.model.CarAssignment;
import com.sap.charging.sim.util.SimulationListenerCSV;
import com.sap.charging.sim.util.SimulationListenerJSON;
import com.sap.charging.util.JSONKeys;
import com.sap.charging.util.Loggable;

public class SimulationResult extends Instance implements Loggable {

	@Override
	public int getVerbosity() {
		return Simulation.verbosity;
	}
	
	private final State state;
	private MultiKeyMap<Integer, Variable> variablesP;
	private HashMap<Car, double[]> carInputCurrents;
	private HashMap<ChargingStation, double[]> chargingStationOutputCurrents;
	private Strategy strategy;
	private SimulationListenerCSV simulationListenerCSV; // Can be null
	private SimulationListenerJSON simulationListenerJSON; // Can be null

	public SimulationResult(DataGenerator dataGenerator, State state, Strategy strategy) {
		super(dataGenerator);
		this.state = state;
		this.variablesP = new MultiKeyMap<>();
		this.carInputCurrents = new HashMap<>();
		this.chargingStationOutputCurrents = new HashMap<>();
		this.strategy = strategy;
	}
	
	/**
	 * Returns a single variable: Unit is Ampere (A)
	 * .getValue() will return the AVERAGE Amps over the 15 mins timeslot
	 * @param i Charging station 
	 * @param j Phase
	 * @param k Timeslot
	 * @return
	 */
	public Variable getVariableP(int i, int j, int k) {
		return variablesP.get(i, j, k);
	}
	/**
	 * 
	 * @param i
	 * @param j
	 * @param k
	 * @param ampereHours Ah, not A! ampereHours loaded in 15 minutes
	 */
	public void addVariableP(int i, int j, int k, double ampereHours) {
		Variable variableP = getVariableP(i, j, k);
		if (variableP == null) {
			variableP = new Variable("P", false);
			variableP.setIndex("i", i);
			variableP.setIndex("j", j);
			variableP.setIndex("k", k);
			variableP.setValue(0);
			variablesP.put(i, j, k, variableP);
		}
		// Divide by 0.25 hours to get from Ah to A. NOT efficiency since this is what is used by infrastructure, not what is loaded by cars
		// Example: Charged 3Ah in 15 mins -> 12A during these 15 mins
		double current = ampereHours / 0.25;
		
		if (current < 0) {
			throw new RuntimeException("ERROR: Attemping to add negative current for variableP=" + variableP.getNameWithIndices() + "=" + current);
		}
		
		variableP.setValue(variableP.getValue() + current);
	}
	
	/**
	 * 
	 * @param car
	 * @param k Timeslot
	 * @param ampereHours Ah, not A! ampereHours loaded in 15 minutes
	 */
	public void addToCarInputCurrents(Car car, int k, double ampereHours) {
		double[] inputCurrents = carInputCurrents.get(car);
		if (inputCurrents == null) {
			inputCurrents = new double[this.energyPriceHistory.getNTimeslots()];
			carInputCurrents.put(car, inputCurrents);
		}
		// Divide by 0.25 hours to get from Ah to A. NOT efficiency since this is what is used by infrastructure, not what is loaded by cars
		double current = ampereHours / 0.25;
		inputCurrents[k] += current;
	}
	
	/**
	 * 
	 * @param chargingStation
	 * @param k Timeslot
	 * @param ampereHours Ah, not A! ampereHours loaded in 15 minutes
	 */
	public void addToChargingStationOutputCurrents(ChargingStation chargingStation, int k, double ampereHours) {
		double[] outputCurrents = chargingStationOutputCurrents.get(chargingStation);
		if (outputCurrents == null) {
			outputCurrents = new double[this.energyPriceHistory.getNTimeslots()];
			chargingStationOutputCurrents.put(chargingStation, outputCurrents);
		}
		// Divide by 0.25 hours to get from Ah to A. NOT efficiency since this is what is used by infrastructure, not what is loaded by cars
		double current = ampereHours / 0.25;
		outputCurrents[k] += current;
	}
	
	
	
	/**
	 * Returns the amount charged in total (Ah)
	 * @return
	 */
	public double getSumCharged() {
		// Divide by 4 to get from A*h to A*timeslot
		return variablesP.values().stream()
				.mapToDouble(v -> v.getValue())
				.sum() * CONSTANTS.CHARGING_EFFICIENCY / 4;
	}
	/**
	 * Returns the amount charged in total for a car (Ah)
	 * @param car
	 * @return
	 */
	public double getSumCharged(Car car) {
		CarAssignment carAssignment = state.getCarAssignmentFromAll(car);
		if (carAssignment == null) {
			return 0;
		}
		// Divide by 4 to get from A*h to A*timeslot
		int i = carAssignment.chargingStation.getId();
		int kMin = carAssignment.car.getFirstAvailableTimeslot();
		int kMax = carAssignment.car.getLastAvailableTimeslot();
		return variablesP.values().stream()
				.filter(v -> v.getIndex("i")==i && v.getIndex("k") >= kMin && v.getIndex("k") <= kMax)
				.mapToDouble(v -> v.getValue())
				.sum() * CONSTANTS.CHARGING_EFFICIENCY / 4;
	}
	
	
	/**
	 * Constructs a JSON version of the original problem instance's parameters
	 * (indepedent of which method is being used to solve it)
	 * JSON key that should be used: problemInstance. 
	 * 
	 * Overriden in this class to add carInputCurrents and chargingStationOutputCurrents
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
			JSONObject chargingStationJSON = chargingStation.toJSONObject();
			
			double[] outputCurrents = chargingStationOutputCurrents.get(chargingStation);
			if (outputCurrents != null) {
				JSONArray outputCurrentsJSON = new JSONArray();
				for (double outputCurrent : outputCurrents) {
					outputCurrentsJSON.add(outputCurrent);
				}
				chargingStationJSON.put(JSONKeys.JSON_KEY_CHARGING_STATION_OUTPUT_CURRENTS, outputCurrentsJSON);
			}
			
			chargingStationsJSON.add(chargingStationJSON);
		}
		problemInstance.put(JSONKeys.JSON_KEY_CHARGING_STATIONS, chargingStationsJSON);
		
		// Cars
		JSONArray carsJSON = new JSONArray();
		for (Car car : this.getCars()) {
			JSONObject carJSON = car.toJSONObject();
			
			double[] inputCurrents = carInputCurrents.get(car);
			if (inputCurrents != null) {
				JSONArray inputCurrentsJSON = new JSONArray();
				for (double inputCurrent : inputCurrents) {
					inputCurrentsJSON.add(inputCurrent);
				}
				carJSON.put(JSONKeys.JSON_KEY_CAR_INPUT_CURRENTS, inputCurrentsJSON);
			}
			
			carsJSON.add(carJSON);
		}		
		problemInstance.put(JSONKeys.JSON_KEY_CARS, carsJSON);
		
		
		// Fuse tree
		problemInstance.put(JSONKeys.JSON_KEY_FUSE_TREE, this.getFuseTree().toJSONObject());
		
		return problemInstance;
	}
	
	
	
	
		
	@SuppressWarnings("unchecked")
	@Override
	protected JSONObject getSolutionJSON() {
		JSONObject solution = new JSONObject();
		JSONArray variables = new JSONArray();
		
		// Add variableX
		for (CarAssignment carAssignment : state.getAllCarAssignments()) {
			variables.add(carAssignment.toVariableX().toJSONObject());
		}
		
		log(1, "Added " + variables.size() + " variableX.");
		log(1, variablesP.values().size() + " variableP variables are available.");
		
		// Add variableP
		for (Variable variableP : variablesP.values()) {
			variables.add(variableP.toJSONObject());
		}
		
		solution.put(JSONKeys.JSON_KEY_VARIABLES, variables);
		
		if (getSimulationListenerCSV() != null) {
			solution.put(JSONKeys.JSON_KEY_CSV_RESULT, getSimulationListenerCSV().getCSVString());
		}

		if (getSimulationListenerJSON() != null) {
			solution.put(JSONKeys.JSON_KEY_JSON_RESULT, getSimulationListenerJSON().getJSONData());
		}
		
		return solution;
	}
	
	

	@Override
	public String getMethod() {
		return strategy.getMethod();
	}

	public SimulationListenerCSV getSimulationListenerCSV() {
		return simulationListenerCSV;
	}

	public SimulationListenerJSON getSimulationListenerJSON() {
		return simulationListenerJSON;
	}

	public void setSimulationListenerCSV(SimulationListenerCSV simulationListenerCSV) {
		this.simulationListenerCSV = simulationListenerCSV;
	}

	public void setSimulationListenerJSON(SimulationListenerJSON simulationListenerJSON) {
		this.simulationListenerJSON = simulationListenerJSON;
	}

}
