package com.sap.charging.realTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.Fuse;
import com.sap.charging.model.FuseTree;
import com.sap.charging.model.FuseTreeNode;
import com.sap.charging.opt.InstanceEmpty;
import com.sap.charging.realTime.exception.CarAlreadyAssignedException;
import com.sap.charging.realTime.exception.CarAlreadyAssignedPowerException;
import com.sap.charging.realTime.exception.CarNotAssignedException;
import com.sap.charging.realTime.exception.CarNotAvailableException;
import com.sap.charging.realTime.exception.ChargingStationAlreadyOccupiedException;
import com.sap.charging.realTime.exception.ChargingStationNotOccupiedException;
import com.sap.charging.realTime.model.CarAssignment;
import com.sap.charging.realTime.model.PowerAssignment;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.JSONKeys;
import com.sap.charging.util.JSONSerializable;
import com.sap.charging.util.Loggable;
import com.sap.charging.util.TimeUtil;

public class State implements Loggable, JSONSerializable {
	
	public int getVerbosity() {
		return Simulation.verbosity;
	}
	
	private final List<ChargingStation> chargingStationsFree;
	private final List<ChargingStation> chargingStationsOccupied;
	public final int nChargingStations;
	
	public FuseTree fuseTree;
	public final List<Car> cars;
	public EnergyPriceHistory energyPriceHistory;
	
	private final List<CarAssignment> currentCarAssignments;
	private final HashMap<Car, CarAssignment> currentCarAssignmentsByCar;
	private final HashMap<ChargingStation, CarAssignment> currentCarAssignmentsByChargingStation;
	// List retrieval: O(1) by CarAssignment, O(n) by car or charging station
	
	private final List<Car> currentUnassignedCars;
	
	
	
	private final List<PowerAssignment> currentPowerAssignments;
	
	private final List<CarAssignment> allCarAssignments;
	//private final List<PowerAssignment> allPowerAssignments;
	
	public final int startTimeSeconds;
	public int currentTimeSeconds;
	public int currentTimeslot;
	
	
	/**
	 * Captures a state of the parking lot (timeless):
	 * Infrastructure (charging stations + fuses + configuration)
	 * Cars 
	 * Assignments
	 * Current power consumption
	 * 
	 */
	public State(int startTimeSeconds, 
			List<ChargingStation> chargingStations, 
			FuseTree fuseTree,
			List<Car> cars,
			EnergyPriceHistory energyPriceHistory) {
		this.startTimeSeconds = startTimeSeconds;
		this.currentTimeSeconds = startTimeSeconds;
		this.currentTimeslot = TimeUtil.getTimeslotFromSeconds(currentTimeSeconds-startTimeSeconds);
		
		this.chargingStationsFree = new ArrayList<>();
		this.chargingStationsFree.addAll(chargingStations);
		this.chargingStationsOccupied = new ArrayList<>();
		this.nChargingStations = chargingStations.size();
		
		this.fuseTree = fuseTree;
		this.energyPriceHistory = energyPriceHistory;
		this.cars = cars;
		
		this.currentCarAssignments = new ArrayList<CarAssignment>();
		this.currentCarAssignmentsByCar = new HashMap<>();
		this.currentCarAssignmentsByChargingStation = new HashMap<>();
		
		this.currentUnassignedCars = new ArrayList<Car>();
		
		this.currentPowerAssignments = new ArrayList<PowerAssignment>();
		this.allCarAssignments = new ArrayList<CarAssignment>();
		//this.allPowerAssignments = new ArrayList<PowerAssignment>();
		
	}
	
	public State(int startTimeSeconds, DataGenerator dataGenerator) {
		this(startTimeSeconds, dataGenerator.getChargingStations(), dataGenerator.getFuseTree(), dataGenerator.getCars(), dataGenerator.getEnergyPriceHistory());
	}
	
	
	public void setTimeSeconds(int timeSeconds) {
		this.currentTimeSeconds = timeSeconds;
		this.currentTimeslot = TimeUtil.getTimeslotFromSeconds(currentTimeSeconds-startTimeSeconds);
	}
	
	/**
	 * Increments the time in seconds by 1 second
	 */
	public void incrementTimeSeconds() {
		int newTimeSeconds = this.currentTimeSeconds+1;
		this.setTimeSeconds(newTimeSeconds);
	}
	

	public void setEnergyPriceHistory(EnergyPriceHistory newEnergyPriceHistory) {
		this.energyPriceHistory = newEnergyPriceHistory;
	}
	public List<Car> getCars() {
		return cars; 
	}
	
	public List<CarAssignment> getCurrentCarAssignments() {
		return currentCarAssignments;
	}
	public List<PowerAssignment> getCurrentPowerAssignments() {
		return currentPowerAssignments;
	}
	public List<CarAssignment> getAllCarAssignments() {
		return allCarAssignments;
	}
	/*public List<PowerAssignment> getAllPowerAssignments() {
		return allPowerAssignments;
	}*/
	
	public boolean isAnyChargingStationFree() {
		return this.chargingStationsFree.size() > 0;
	}
	
	public ChargingStation getFirstFreeChargingStation() {
		return this.chargingStationsFree.get(0);
	}
	
	/**
	 * Designed to be able to be called during simulation
	 * @param chargingStation
	 */
	public void addFreeChargingStation(ChargingStation chargingStation) {
		if (this.chargingStationsFree.contains(chargingStation) == true) {
			throw new RuntimeException("Attempted to add free chargingStation, but already contained in list chargingStationsFree.");
		}
		else if (this.chargingStationsOccupied.contains(chargingStation) == true) {
			throw new RuntimeException("Attempted to add free chargingStation, but already contained in list chargingStationsOccupied.");
		}
		else {
			this.chargingStationsFree.add(chargingStation);
		}
	}
	
	public List<ChargingStation> getChargingStationsFree() {
		return this.chargingStationsFree;
	}
	public List<ChargingStation> getChargingStationsOccupied() {
		return this.chargingStationsOccupied;
	}
	
	/**
	 * I is index of chargingStation
	 * Returns null if the index does not exist or if it is not free
	 * @param i
	 * @return
	 */
	public ChargingStation getFreeChargingStation(int chargingStationID) {
		for (ChargingStation chargingStation : chargingStationsFree) {
			if (chargingStation.getId() == chargingStationID) {
				return chargingStation;
			}
		}
		return null;
	}
	public ChargingStation getOccupiedChargingStation(int chargingStationID) {
		for (ChargingStation chargingStation : chargingStationsOccupied) {
			if (chargingStation.getId() == chargingStationID) {
				return chargingStation;
			}
		}
		return null;
	}
	public ChargingStation getChargingStation(int chargingStationID) {
		ChargingStation result = getFreeChargingStation(chargingStationID); 
		if (result == null) {
			result = getOccupiedChargingStation(chargingStationID); 
		}
		return result; 
	}
	
	
	private void setChargingStationOccupied(ChargingStation chargingStation) {
		if (chargingStationsFree.contains(chargingStation) == false || 
			chargingStationsOccupied.contains(chargingStation) == true) {
			throw new ChargingStationAlreadyOccupiedException(chargingStation);
		}
		
		chargingStationsFree.remove(chargingStation);
		chargingStationsOccupied.add(chargingStation);
	}
	private void setChargingStationFree(ChargingStation chargingStation) {
		if (chargingStationsFree.contains(chargingStation) == true || 
			chargingStationsOccupied.contains(chargingStation) == false) {
			throw new ChargingStationNotOccupiedException(chargingStation);
		}
		
		chargingStationsOccupied.remove(chargingStation);
		chargingStationsFree.add(chargingStation);
	}
	
	
	public Car getCar(int carID) {
		for (Car car : cars) {
			if (car.getId() == carID) {
				return car;
			}
		}
		return null;
	}
	
	public boolean isCarCurrentlyAssigned(Car car) {
		return currentCarAssignmentsByCar.get(car) != null ?
				true : 
				false;
	}
	public boolean isChargingStationCurrentlyAssigned(ChargingStation chargingStation) {
		return currentCarAssignmentsByChargingStation.get(chargingStation) != null ?
				true :
				false;
	}
	
	
	public CarAssignment getCurrentCarAssignment(Car car) {
		/*for (CarAssignment carAssignment : currentCarAssignments) {
			if (carAssignment.car.equals(car)) 
				return carAssignment;
		}
		return null;*/
		return currentCarAssignmentsByCar.get(car);
	}
	public CarAssignment getCurrentCarAssignment(int indexN) {
		for (CarAssignment carAssignment : currentCarAssignments) {
			if (carAssignment.car.getId() == indexN) 
				return carAssignment;
		}
		return null;
	}
	
	public CarAssignment getCurrentCarAssignment(ChargingStation chargingStation) {
		/*for (CarAssignment carAssignment : currentCarAssignments) {
			if (carAssignment.chargingStation.equals(chargingStation)) 
				return carAssignment;
		}
		return null;*/
		return currentCarAssignmentsByChargingStation.get(chargingStation);
	}
	
	public CarAssignment addCarAssignment(Car car, ChargingStation chargingStation) {
		if (isCarCurrentlyAssigned(car) == true ||
			isChargingStationCurrentlyAssigned(chargingStation)) {
			// If car is already assigned to a different charging station
			throw new CarAlreadyAssignedException(car, chargingStation);
		}
		if (currentTimeSeconds < car.timestampArrival.toSecondOfDay() ||
			(currentTimeSeconds > car.timestampDeparture.toSecondOfDay() && car.timestampDeparture.toSecondOfDay() != 0) // Only do the latter check if a timestampDeparture is actually assigned
			) {
			// If a car is not available yet
			throw new CarNotAvailableException(car, currentTimeSeconds);
		}
		
		setChargingStationOccupied(chargingStation);
		
		CarAssignment assignment = new CarAssignment(car, chargingStation);
		currentCarAssignments.add(assignment);
		currentCarAssignmentsByCar.put(car, assignment);
		currentCarAssignmentsByChargingStation.put(chargingStation, assignment);
		allCarAssignments.add(assignment);
		return assignment;
	}
	public void removeCarAssignment(Car car) {
		int removeIndex = -1;
		for (int i=0;i<currentCarAssignments.size();i++) {
			if (currentCarAssignments.get(i).car.equals(car)) 
				removeIndex = i;
		}
		
		if (removeIndex == -1) {
			throw new CarNotAssignedException(car);
		}
		
		CarAssignment assignment = currentCarAssignments.get(removeIndex);
		
		setChargingStationFree(assignment.chargingStation);
		currentCarAssignments.remove(removeIndex);
		currentCarAssignmentsByCar.remove(car);
		currentCarAssignmentsByChargingStation.remove(assignment.chargingStation);
	}
	
	/**
	 * Adds an unassigned car, i.e. there was no space for this car to park at. 
	 * @param car
	 * @return
	 */
	public void addUnassignedCar(Car newCar) {
		if (isCarCurrentlyAssigned(newCar)) {
			throw new CarAlreadyAssignedException(newCar);
		}
		
		this.currentUnassignedCars.add(newCar);
	}
	
	public void removeUnassignedCar(Car car) {
		if (isCarCurrentlyAssigned(car)) {
			throw new CarAlreadyAssignedException(car);
		}
		
		int removeIndex = -1;
		for (int i=0;i<currentUnassignedCars.size();i++) {
			if (currentUnassignedCars.get(i).equals(car)) 
				removeIndex = i;
		}
		if (removeIndex == -1) {
			throw new CarNotAssignedException(car);
		}
		this.currentUnassignedCars.remove(removeIndex);
	}
	public boolean isCarCurrentlyUnassigned(Car car) {
		boolean inCarAssignments = this.getCurrentCarAssignment(car) != null; 
		return this.currentUnassignedCars.contains(car) && inCarAssignments == false;
	}
	
	
	public CarAssignment getCarAssignmentFromAll(Car car) {
		for (CarAssignment carAssignment : allCarAssignments) {
			if (carAssignment.car.equals(car)) 
				return carAssignment;
		}
		return null;
	}
	
	/**
	 * Get all car assignments related to a given fuse.
	 * @param fuse
	 * @return
	 */
	public List<CarAssignment> getCarAssignmentsByFuse(FuseTreeNode fuseTreeNode) {
		ArrayList<ChargingStation> chargingStations = null; 
		if (fuseTreeNode instanceof Fuse) {
			chargingStations = ((Fuse) fuseTreeNode).getChargingStationChildren();
		}
		else if (fuseTreeNode instanceof ChargingStation) {
			chargingStations = new ArrayList<>(); 
			chargingStations.add((ChargingStation) fuseTreeNode); 
		}
		
		
		// Get all car assignments relevant to this violation
		ArrayList<CarAssignment> carAssignments = new ArrayList<>();
		for (ChargingStation chargingStation : chargingStations) {
			CarAssignment carAssignment = getCurrentCarAssignment(chargingStation);
			if (carAssignment != null) 
				carAssignments.add(carAssignment);
		}
		return carAssignments;		
	}
	
	
	
	/**
	 * Returns true if there is a variable powerAssignment containing
	 * car in currentPowerAssignments. The value 
	 * of the variable itself can be set to 0.
	 * @param car
	 * @return
	 */
	public boolean isCarPowerAssigned(Car car) {
		for (PowerAssignment powerAssignment : currentPowerAssignments) {
			if (powerAssignment.car.equals(car)) 
				return true;
		}
		return false;
	}
	public PowerAssignment getCurrentPowerAssignment(Car car) {
		for (PowerAssignment powerAssignment : currentPowerAssignments) {
			if (powerAssignment.car.equals(car)) 
				return powerAssignment;
		}
		return null;
	}
	public PowerAssignment getCurrentPowerAssignment(ChargingStation chargingStation) {
		for (PowerAssignment powerAssignment : currentPowerAssignments) {
			if (powerAssignment.chargingStation.equals(chargingStation)) 
				return powerAssignment;
		}
		return null;
	}
	
	
	public PowerAssignment addPowerAssignment(Car car, ChargingStation chargingStation, 
			double phase1, double phase2, double phase3) {
		if (isCarCurrentlyAssigned(car)) {
			CarAssignment carAssignment = getCurrentCarAssignment(car);
			// If assigned to other charging station  throw exception
			if (carAssignment.car.equals(car) == false || 
				carAssignment.chargingStation.equals(chargingStation) == false) {
				throw new CarAlreadyAssignedException(car, chargingStation);
			}
		}
		else {
			// If car not assigned, throw exception
			throw new CarNotAssignedException(car);
		}
		
		if (isCarPowerAssigned(car)) {
			// If power assignment already exists throw exception
			throw new CarAlreadyAssignedPowerException(car);
		}
		
		PowerAssignment assignment = new PowerAssignment(chargingStation, car, phase1, phase2, phase3);
		currentPowerAssignments.add(assignment);
		
		return assignment;
		//allPowerAssignments.add(assignment);
	}
	public void removePowerAssignment(Car car) {
		int removeIndex = -1;
		for (int i=0;i<currentPowerAssignments.size();i++) {
			if (currentPowerAssignments.get(i).car.equals(car)) 
				removeIndex = i;
		}
		log(2, "Removing powerAssignment for car n=" + car.getId() + " at t=" + currentTimeSeconds + ". RemoveIndex=" + removeIndex);
		if(removeIndex>=0){
			currentPowerAssignments.remove(removeIndex);
		}
	}

	
	/**
	 * Returns a JSON representation of the current state
	 */
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJSONObject() {
		JSONObject result = new JSONObject();
		result.put(JSONKeys.JSON_KEY_STATE_CURRENT_TIMESLOT, currentTimeslot);
		result.put(JSONKeys.JSON_KEY_STATE_START_TIME_SECONDS, startTimeSeconds);
		result.put(JSONKeys.JSON_KEY_STATE_CURRENT_TIME_SECONDS, currentTimeSeconds);
		
		JSONArray jsonCurrentCarAssignments = new JSONArray();
		for (CarAssignment carAssignment : currentCarAssignments) {
			jsonCurrentCarAssignments.add(carAssignment.toJSONObject());
		}
		result.put(JSONKeys.JSON_KEY_STATE_CURRENT_CAR_ASSIGNMENTS, jsonCurrentCarAssignments);
		
		JSONArray jsonCurrentPowerAssignments = new JSONArray();
		for (PowerAssignment powerAssignment : currentPowerAssignments) {
			jsonCurrentPowerAssignments.add(powerAssignment.toJSONObject());
		}
		result.put(JSONKeys.JSON_KEY_STATE_CURRENT_POWER_ASSIGNMENTS, jsonCurrentPowerAssignments);
		
		JSONArray jsonCurrentUnassignedCars = new JSONArray();
		for (Car car : currentUnassignedCars) {
			JSONObject smallCarJSON = new JSONObject();
			smallCarJSON.put(JSONKeys.JSON_KEY_INDEX_N, car.getId());
			jsonCurrentUnassignedCars.add(smallCarJSON);
		}
		result.put(JSONKeys.JSON_KEY_STATE_CURRENT_UNASSIGNED_CARS, jsonCurrentUnassignedCars);
		
		return result;
	}
	
	@JsonIgnore
	public JSONObject getProblemInstanceJSON() {
		List<ChargingStation> chargingStations = new ArrayList<>();
		chargingStations.addAll(chargingStationsFree);
		chargingStations.addAll(chargingStationsOccupied);
		InstanceEmpty instance = new InstanceEmpty(cars, chargingStations, energyPriceHistory, fuseTree);
		return instance.toJSONObject();
	}
	
	public static State fromPreviousData(JSONObject jsonState, DataGenerator data) {
		
		if (jsonState.containsKey(JSONKeys.JSON_KEY_STATE)) {
			jsonState = (JSONObject) jsonState.get(JSONKeys.JSON_KEY_STATE);
		}
		
		int startTimeSeconds = getJSONAttributeAsInt(jsonState.get(JSONKeys.JSON_KEY_STATE_START_TIME_SECONDS));
		int currentTimeSeconds = getJSONAttributeAsInt(jsonState.get(JSONKeys.JSON_KEY_STATE_CURRENT_TIME_SECONDS));
		State state = new State(startTimeSeconds, data);
		state.setTimeSeconds(currentTimeSeconds);
	
		JSONArray jsonCarAssignments = (JSONArray) jsonState.get(JSONKeys.JSON_KEY_STATE_CURRENT_CAR_ASSIGNMENTS);
		for (Object objectCarAssignment : jsonCarAssignments) {
			JSONObject jsonCarAssignment = (JSONObject) objectCarAssignment;
			int indexI = getJSONAttributeAsInt(jsonCarAssignment.get(JSONKeys.JSON_KEY_INDEX_I));
			int indexN = getJSONAttributeAsInt(jsonCarAssignment.get(JSONKeys.JSON_KEY_INDEX_N));
			ChargingStation chargingStation = data.getChargingStation(indexI); 
			Car car = data.getCar(indexN);
			
			state.addCarAssignment(car, chargingStation);
		}
		
		JSONArray jsonPowerAssignments = (JSONArray) jsonState.get(JSONKeys.JSON_KEY_STATE_CURRENT_POWER_ASSIGNMENTS);
		for (Object objectPowerAssignment : jsonPowerAssignments) {
			JSONObject jsonPowerAssignment = (JSONObject) objectPowerAssignment;
			int indexI = getJSONAttributeAsInt(jsonPowerAssignment.get(JSONKeys.JSON_KEY_INDEX_I));
			int indexN = getJSONAttributeAsInt(jsonPowerAssignment.get(JSONKeys.JSON_KEY_INDEX_N));
			ChargingStation chargingStation = data.getChargingStation(indexI); 
			Car car = data.getCar(indexN);
			double phase1 = (double) jsonPowerAssignment.get(JSONKeys.JSON_KEY_PHASE_1);
			double phase2 = (double) jsonPowerAssignment.get(JSONKeys.JSON_KEY_PHASE_2);
			double phase3 = (double) jsonPowerAssignment.get(JSONKeys.JSON_KEY_PHASE_3);
			
			state.addPowerAssignment(car, chargingStation, phase1, phase2, phase3);
		}
		
		JSONArray unassignedCars = (JSONArray) jsonState.get(JSONKeys.JSON_KEY_STATE_CURRENT_UNASSIGNED_CARS);
		for (Object objectUnassignedCar : unassignedCars) {
			JSONObject jsonUnassignedCar = (JSONObject) objectUnassignedCar;
			int indexN = getJSONAttributeAsInt(jsonUnassignedCar.get(JSONKeys.JSON_KEY_INDEX_N));
			Car car = data.getCar(indexN);
			
			state.addUnassignedCar(car);
		}
		
		return state;
	}
	
	private static int getJSONAttributeAsInt(Object value) {
		if (value instanceof Integer) {
				return (int) value;
		}
		else if (value instanceof Long) {
			return Math.toIntExact((long) value);
		}
		throw new RuntimeException("Weird value: " + value);
	}
	
}
