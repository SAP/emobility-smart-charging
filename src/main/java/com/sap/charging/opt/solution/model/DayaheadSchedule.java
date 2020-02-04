package com.sap.charging.opt.solution.model;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.charging.dataGeneration.DataGeneratorFromFile;
import com.sap.charging.model.Car;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.opt.lp.Variable;
import com.sap.charging.realTime.State;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.JSONKeys;
import com.sap.charging.util.Loggable;

public class DayaheadSchedule implements Loggable {
	
	public int getVerbosity() {
		return Simulation.verbosity;
	}
	
	private MultiKeyMap<Integer, Variable> variablesX;
	private MultiKeyMap<Integer, Variable> variablesP;
	private EnergyPriceHistory energyPriceHistory;
	//private List<Car> cars;
	private DataGeneratorFromFile data;
	
	
	/**
	 *  Construct schedule from JSON object "solution"
	 */
	public DayaheadSchedule(JSONObject solvedProblemInstance) {
		JSONObject problemInstance = (JSONObject) solvedProblemInstance.get(JSONKeys.JSON_KEY_PROBLEM_INSTANCE);
		JSONObject solution = (JSONObject) solvedProblemInstance.get(JSONKeys.JSON_KEY_SOLUTION);
		
		this.variablesX = new MultiKeyMap<>();
		this.variablesP = new MultiKeyMap<>();
		
		this.addEnergyPriceHistory(problemInstance);
		this.addData(problemInstance);
		this.addVariables(solution);
	}
	
	public int getNTimeslots() {
		return getEnergyPriceHistory().getNTimeslots();
	}
	
	public EnergyPriceHistory getEnergyPriceHistory() {
		return energyPriceHistory;
	}
	
	
	private void addEnergyPriceHistory(JSONObject problemInstance) {
		JSONObject jsonHistory = (JSONObject) problemInstance.get(JSONKeys.JSON_KEY_ENERGY_PRICE_HISTORY);
		this.energyPriceHistory = EnergyPriceHistory.fromJSON(jsonHistory);
	}
	
	private void addData(JSONObject problemInstance) {
		data = new DataGeneratorFromFile(problemInstance);
		data.generateAll();
	}
	
	/*private void addCars(JSONObject problemInstance) {
		cars = new ArrayList<Car>();
		JSONArray jsonCars = (JSONArray) problemInstance.get(JSONKeys.JSON_KEY_CARS);
		for (int n=0;n<jsonCars.size();n++) {
			JSONObject jsonCar = (JSONObject) jsonCars.get(n);
			Car car = Car.fromJSON(jsonCar, this.getNTimeslots());
			cars.add(car);
		}
	}
	*/
	private Car getPlannedCar(int n) {
		for (Car car : data.getCars()) {
			if (car.getId() == n)
				return car;
		}
		return null;
	}
	
	
	private void addVariables(JSONObject solution) {
		JSONArray variables = (JSONArray) solution.get(JSONKeys.JSON_KEY_VARIABLES);
		
		for (Object jsonVariable : variables) {
			Variable variable = Variable.fromJSON((JSONObject) jsonVariable);
			if (variable.getName().equals("X") && variable.getValue() == 1) {
				variablesX.put(variable.getIndex("i"), 
							   variable.getIndex("n"), 
							   variable);
			}
			if (variable.getName().equals("P")) {
				if (variable.getValue() <= 0 && variable.getValue() >= -Math.pow(10, -6)) {
					variable.setValue(0);
					log(2, "Correcting variable " + variable.getNameWithIndices() + " from " + variable.getValue() + " to 0.");
				}
				else if (variable.getValue() <= 0) {
					throw new RuntimeException("ERROR: Negative current in plan: " + variable.getValue());
				}
				
				variablesP.put(variable.getIndex("i"),
							   variable.getIndex("j"),
							   variable.getIndex("k"), 
							   variable);
			}
		}
	}
	
	public Variable getVariableX(int i, int n) {
		return variablesX.get(i, n);
	}
	/**
	 * This is unambigious, since a car is assigned to at most 
	 * one charging station
	 * @param n
	 * @return
	 */
	public Variable getVariableX(int n) {
		for (Variable variable : variablesX.values()) {
			if (variable.getIndex("n") == n)
				return variable;
		}
		return null;
	}
	public MultiKeyMap<Integer, Variable> getVariablesX() {
		return variablesX;
	}
	
	public Variable getVariableP(int i, int j, int k) {
		return variablesP.get(i, j, k);
	}
	/**
	 * Get list of P variables for given charging station i
	 * ONLY for first phase (j=1), sorted ascending by k (small first)
	 * Example: [-242.13*P_i0_j1_k0, -241.205*P_i0_j1_k1]
	 * 
	 * NOTE: This list of P variables may not exhaustively cover
	 * all possible values of k. 
	 * 
	 * @param i
	 * @return
	 */
	public List<Variable> getVariablesP(int i) {
		return variablesP.values().stream()
				.filter(v -> v.getIndex("i")==i && v.getIndex("j") == 1)
				.sorted(new Comparator<Variable>() {
					@Override
					public int compare(Variable v1, Variable v2) {
						// Sort on k
						return (v1.getIndex("k") > v2.getIndex("k")) ? 1 : -1;
					}
				})
				.collect(Collectors.toList());
	}
	
	public List<Variable> getVariablesPByTimeslot(int k) {
		return variablesP.values().stream()
				.filter(v -> v.getIndex("k")==k)
				.sorted(new Comparator<Variable>() {
					@Override
					public int compare(Variable v1, Variable v2) {
						// Sort on k
						return (v1.getIndex("i") > v2.getIndex("i")) ? 1 : -1;
					}
				})
				.collect(Collectors.toList());
	}
	
	/**
	 * Creates a complete charging plan for a given charging station i
	 * If any variableP is missing, the missing value is set to 0
	 * @param i
	 * @return
	 */
	public double[] getChargingStationPlan(int i) {
		double[] result = new double[getNTimeslots()];
		for (int k=0;k<getNTimeslots();k++) {
			Variable variableP = getVariableP(i, 1, k);
			if (variableP == null) {
				// If there is no variableP for the given timeslot k
				// Set charging plan for this timeslot to 0
				result[k] = 0;
			}
			else {
				result[k] = variableP.getValue();
			}
		}
		return result;
	}
		
	@Override
	public String toString() {
		String result = "variablesX: ";
		for (Variable variableX : variablesX.values()) {
			result += variableX.getNameWithIndices() + "=" + variableX.getValue() + ", ";
		}
		return result;
	}
	
	
	public int getReservedSpotsAtTimeslot(State state, int allowedLatenessSeconds) {
		// A charging station is reserved for the whole day if it is reserved sometime during the day
		// Unless an EV (that has not yet arrived) is late by at least allowedLatenessSeconds
		boolean[] reservedStations = new boolean[data.getChargingStations().size()];
		for (Variable variableX : variablesX.values()) {
			int i = variableX.getIndex("i");
			int n = variableX.getIndex("n");
			
			// A station indexI=i is reserved if a planned car is not late by at least allowedLatenessSecond
			Car car = this.getPlannedCar(n);
			boolean hasArrived = state.getCurrentCarAssignment(n) == null ? false : true;
			
			// Each variableX represents one reservation from car's first to last available timeslot (inclusive)
			// Get how late the car is for the current reservation
			int latenessSeconds = state.currentTimeSeconds - car.getFirstAvailableTimeslot()*15*60;
			if (latenessSeconds <= allowedLatenessSeconds || // This condition: If the car has not yet arrived, i.e. is planned to arrive
				hasArrived == true) { 						 // This condition: If the car has actually arrived
				// If the car is late by more than allowedLatenessSeconds the reservation is "cancelled" and the station is NOT reserved for this car
				reservedStations[i] = true;
			}
			/*else {
				// This is only valid if the car hasn't arrived
				log(1, "car id=" + n + " is late for charging station i=" + i + " by " + latenessSeconds + "s");
				System.out.println(car);
			}*/
			
			// Don't check via indexI because original charging station might not have been chosen
		}
		
		int nReserved = 0;
		for (boolean reserved : reservedStations) {
			if (reserved == true)
				nReserved++;
		}
		return nReserved;
	}
	
	
	
	
	
	
}
