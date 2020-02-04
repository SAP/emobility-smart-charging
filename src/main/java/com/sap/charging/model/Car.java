package com.sap.charging.model;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.charging.model.CarFactory.CarType;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.model.battery.BatteryData;
import com.sap.charging.model.battery.CarBattery;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.JSONKeys;
import com.sap.charging.util.JSONSerializable;
import com.sap.charging.util.Loggable;
import com.sap.charging.util.TimeUtil;
import com.sap.charging.util.Util;

public class Car implements JSONSerializable, Loggable {

	/*******************
	 * Car metadata
	 *******************/
	/**
	 * ID of car (n)
	 */
	private final int id;

	private final String name;

	private final String modelName;

	private final CarType carType;

	/*********************
	 * Car availabilities (time)
	 *********************/
	/**
	 * Describes which discretized timeslots the car is available for charging (in
	 * 15 minute intervals). true=is available, false=is not (d_k=1 or d_k=0)
	 */
	@JsonIgnore
	public final boolean[] availableTimeslots;
	/**
	 * Cache for start of the available timeslots (discrete)
	 */
	private Integer availStartCache = null;
	/**
	 * Cache for end of the available timeslots (discrete)
	 */
	private Integer availEndCache = null;
	/**
	 * Non-discrete/continuous Timestamp for arrival
	 */
	public LocalTime timestampArrival;
	/**
	 * Non-discrete/continuous Timestamp for departure
	 */
	public LocalTime timestampDeparture;

	/********************
	 * Car technical capabilities (charging)
	 *******************/

	/**
	 * Maximum current the car must be loaded with (in Amps), IN TOTAL.
	 */
	public final double minCurrent;
	/**
	 * Minimum power the car must be loaded with (in Amps), PER PHASE
	 */
	public final double minCurrentPerPhase;
	/**
	 * Maximum power the car can be loaded with (in Amps), IN TOTAL.
	 */
	public final double maxCurrent;
	/**
	 * Maximum power the car can be loaded with (in Amps), PER PHASE
	 */
	public final double maxCurrentPerPhase;
	/**
	 * The minimum capacity that should be charged (In Ah)
	 */
	public final double minLoadingState;

	/**
	 * Describes whether the car can load on phases 1 (j=1) (a_{1,n}). 0 <= a_{1,n}
	 * <= 1, real number
	 */
	public final double canLoadPhase1;
	/**
	 * Describes whether the car can load on phase 2 (j=2) (a_{2,n}).
	 */
	public final double canLoadPhase2;
	/**
	 * Describes whether the car can load on phase 3 (j=3) (a_{3,n}).
	 */
	public final double canLoadPhase3;

	/**
	 * How many phases are used? 1, 2 or 3
	 */
	@JsonIgnore
	public final int nrOfUsedPhases;
	/**
	 * What is the sum of canLoadPhase Example: 1+0.5+0.3 = 1.8
	 */
	@JsonIgnore
	public final double sumUsedPhases;

	/**
	 * Car battery object that decides how much charged capacity is added
	 */
	@JsonIgnore
	public final CarBattery carBattery;

	/************************
	 * Car individual technical characteristics/constraints
	 ************************/
	/**
	 * Can the charging of the car be interrupted.
	 */
	private boolean suspendable;
	/**
	 * Is the charging power variable or does the charging power need to be the same
	 * across all timeslots
	 */
	private boolean canUseVariablePower;
	/**
	 * Is it needed to start the charging immediately upon arrival. Also called
	 * "delayable charging" r_n
	 */
	public boolean immediateStart;

	/**************************
	 * Dynamic attributes changed during optimization
	 *************************/
	/**
	 * Set to true if the car already started charging.
	 */
	private boolean chargingStarted = false;

	/**
	 * The current charge plan used for charging the car. To be confirmed: Each
	 * value is currentPerPhase, for example to be given to OCPP.
	 */
	private double[] currentPlan = null;

	private CarProcessData carProcessData;

	@JsonCreator
	public Car(@JsonProperty(value="id", required=true) int id, @JsonProperty("name") String name,
			@JsonProperty("modelName") String modelName, 
			@JsonProperty(value="carType", required=true) CarType carType,
			@JsonProperty(value="startCapacity") double startCapacity, 
			@JsonProperty(value="timestampArrival", required=true) int timestampArrival,
			@JsonProperty("timestampDeparture") int timestampDeparture, 
			@JsonProperty(value="maxCapacity", required=true) double maxCapacity, 
			@JsonProperty(value="minCurrent", required=true) double minCurrent,
			@JsonProperty(value="minCurrentPerPhase", required=true) double minCurrentPerPhase, 
			@JsonProperty(value="maxCurrent", required=true) double maxCurrent, 
			@JsonProperty(value="maxCurrentPerPhase", required=true) double maxCurrentPerPhase, 
			
			@JsonProperty("suspendable") Boolean suspendable,
			@JsonProperty("canUseVariablePower") Boolean canUseVariablePower, 
			@JsonProperty("immediateStart") Boolean immediateStart, 
			
			@JsonProperty("minLoadingState") double minLoadingState, 
			@JsonProperty("canLoadPhase1") double canLoadPhase1,
			@JsonProperty("canLoadPhase2") double canLoadPhase2,
			@JsonProperty("canLoadPhase3") double canLoadPhase3, 
			
			@JsonProperty("nonlinearCharging") boolean nonlinearCharging, 
			@JsonProperty("batteryData") BatteryData batteryData,
			@JsonProperty("currentPlan") double[] currentPlan) {
		this.id = id;
		this.name = name;
		this.modelName = modelName;
		
		this.carType = carType;
		this.timestampArrival = TimeUtil.getTimestampFromSeconds(timestampArrival);
		this.timestampDeparture = TimeUtil.getTimestampFromSeconds(timestampDeparture);

		this.availableTimeslots = new boolean[24 * 4];
		int startAvailable = TimeUtil.getTimeslotFromSeconds(timestampArrival); 
		int endAvailable = TimeUtil.getTimeslotFromSeconds(timestampDeparture); 
		for (int timeslot = 0; timeslot < 24*4; timeslot++) {
			availableTimeslots[timeslot] = (timeslot >= startAvailable && timeslot <= endAvailable) ? true : false;
		}
		
		this.minCurrent = minCurrent;
		this.minCurrentPerPhase = minCurrentPerPhase;
		this.maxCurrent = maxCurrent;
		this.maxCurrentPerPhase = maxCurrentPerPhase;
		
		this.suspendable = (suspendable != null) ? suspendable : true; // Default value: car is suspendable
		this.canUseVariablePower = (canUseVariablePower != null) ? canUseVariablePower : true; // Default value: Car can use variable power
		this.immediateStart = (immediateStart != null) ? immediateStart : false; // Default value: car does not need immediate start
		this.minLoadingState = minLoadingState;
		this.canLoadPhase1 = canLoadPhase1;
		this.canLoadPhase2 = canLoadPhase2;
		this.canLoadPhase3 = canLoadPhase3;
		this.nrOfUsedPhases = (int) (Math.ceil(canLoadPhase1) + Math.ceil(canLoadPhase2) + Math.ceil(canLoadPhase3));
		this.sumUsedPhases = canLoadPhase1 + canLoadPhase2 + canLoadPhase3;

		this.carBattery = new CarBattery(this, startCapacity, maxCapacity, nonlinearCharging, batteryData);
		
		this.setCurrentPlan(currentPlan);
		this.sanityCheckPhases(); 
	}
	
	private void sanityCheckPhases() {
		// Sum of used phases * minCurrentPerPhase should equal minCurrent
		if (sumUsedPhases * minCurrentPerPhase != minCurrent) {
			throw new IllegalArgumentException("SumUsedPhases (" + sumUsedPhases + ") * minCurrentPerPhase (" + minCurrentPerPhase + ") should equal minCurrent ( " + minCurrent + ")"); 
		}
		// Sum of used phases * maxCurrentPerPhase should equal maxCurrent
		if (sumUsedPhases * maxCurrentPerPhase != maxCurrent) {
			throw new IllegalArgumentException("SumUsedPhases (" + sumUsedPhases + ") * maxCurrentPerPhase (" + maxCurrentPerPhase + ") should equal maxCurrent ( " + maxCurrent + ")"); 
		}
	}
	

	public Car(int id, String name, String modelName, CarType carType, double curCapacity, boolean[] availableTimeslots,
			LocalTime timestampArrival, LocalTime timestampDeparture, double maxCapacity, double minCurrent,
			double minCurrentPerPhase, double maxCurrent, double maxCurrentPerPhase, boolean suspendable,
			boolean canUseVariablePower, boolean immediateStart, double minLoadingState, double canLoadPhase1,
			double canLoadPhase2, double canLoadPhase3, boolean nonlinearCharging, BatteryData batteryData) {
		this.id = id;
		this.name = name;
		this.modelName = modelName;
		this.carType = carType;
		this.availableTimeslots = availableTimeslots;
		this.timestampArrival = timestampArrival;
		this.timestampDeparture = timestampDeparture;
		this.minCurrent = minCurrent;
		this.minCurrentPerPhase = minCurrentPerPhase;
		this.maxCurrent = maxCurrent;
		this.maxCurrentPerPhase = maxCurrentPerPhase;
		this.suspendable = suspendable;
		this.canUseVariablePower = canUseVariablePower;
		this.immediateStart = immediateStart;
		this.minLoadingState = minLoadingState;
		this.canLoadPhase1 = canLoadPhase1;
		this.canLoadPhase2 = canLoadPhase2;
		this.canLoadPhase3 = canLoadPhase3;
		this.nrOfUsedPhases = (int) (Math.ceil(canLoadPhase1) + Math.ceil(canLoadPhase2) + Math.ceil(canLoadPhase3));
		this.sumUsedPhases = canLoadPhase1 + canLoadPhase2 + canLoadPhase3;

		this.carBattery = new CarBattery(this, curCapacity, maxCapacity, nonlinearCharging, batteryData);
		this.sanityCheckPhases(); 
	}
	
	@JsonGetter("timestampArrival")
	public int getTimestampArrival() {
		return this.timestampArrival.toSecondOfDay(); 
	}
	
	@JsonGetter("timestampDeparture") 
	public int getTimestampDeparture() {
		if (this.timestampDeparture == null) 
			return -1; 
		return this.timestampDeparture.toSecondOfDay(); 
	}

	/**
	 * @param phase
	 * @return 0 <= a_{j,n} <= 1, real number
	 */
	public double canLoadPhase(int phase) {
		switch (phase) {
		case 1:
			return canLoadPhase1;
		case 2:
			return canLoadPhase2;
		case 3:
			return canLoadPhase3;
		default:
			return -1;
		}
	}

	public double canLoadPhase(Phase phase) {
		switch (phase) {
		case PHASE_1:
			return canLoadPhase1;
		case PHASE_2:
			return canLoadPhase2;
		case PHASE_3:
			return canLoadPhase3;
		default:
			return -1;
		}
	}

	/**
	 * @param timeslot k
	 * @return
	 */
	public boolean isAvailable(int timeslot) {
		return availableTimeslots[timeslot];
	}

	public int isAvailableInt(int timeslot) {
		return isAvailable(timeslot) ? 1 : 0;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getModelName() {
		return modelName;
	}

	@JsonIgnore
	public boolean isFullyCharged() {
		return carBattery.isFullyCharged();
	}

	@JsonProperty("startCapacity")
	public double getCurrentCapacity() {
		return carBattery.getCurrentCapacity();
	}

	@JsonIgnore
	public double getMissingCapacity() {
		return carBattery.getMaxCapacity() - (carBattery.getCurrentCapacity() + carBattery.getChargedCapacity());
	}

	/**
	 * Returns the difference to the min loading state. This can be negative!
	 * 
	 * @return
	 */
	@JsonIgnore
	public double getMissingCapacityToMinSoC() {
		return minLoadingState - (carBattery.getCurrentCapacity() + carBattery.getChargedCapacity());
	}

	/**
	 * Returns the sum how much would be loaded with the current plan, taking into
	 * account efficiency. NOTE: Car is assumed to be available for each timeslot
	 * fully. Unit: Ah
	 * 
	 * @param currentPlan
	 * @return
	 */
	/*
	 * public double getSumPlannedChargedCapacity() { return
	 * Arrays.stream(this.currentPlan).sum() / 4 * CONSTANTS.CHARGING_EFFICIENCY *
	 * sumUsedPhases; }
	 */

	/**
	 * Sets the current (starting) capacity, should only be used at START of
	 * simulation not during
	 * 
	 * @param ampereHours
	 */
	public void setCurrentCapacity(double ampereHours) {
		if (ampereHours > carBattery.getMaxCapacity()) {
			throw new RuntimeException("ERROR: Tried setting current (starting) capacity=" + ampereHours
					+ ", which is larger than maxCapacity=" + carBattery.getMaxCapacity());
		}
		if (ampereHours + getChargedCapacity() > carBattery.getMaxCapacity()) {
			throw new RuntimeException("ERROR: Tried setting current (starting) capacity=" + ampereHours
					+ " together with chargedCapacity=" + carBattery.getChargedCapacity()
					+ ", which is larger than maxCapacity=" + carBattery.getMaxCapacity()
					+ " (try calling resetChargedCapacity first)");
		}
		if (ampereHours < 0) {
			throw new RuntimeException("ERROR: Tried setting negative current (starting capacity)=" + ampereHours
					+ ", for car=" + this.toString());
		}
		this.carBattery.setCurrentCapacity(ampereHours);
	}

	/*
	 * public void addChargedCapacity(int deltaSeconds, int currentTimeslot) {
	 * this.carBattery.addChargedCapacity(deltaSeconds, currentTimeslot); }
	 */
	/**
	 * @param deltaSeconds How long (in seconds) was this current applied for?
	 * @param maxCurrentAllowed What was the maximum current allowed? The car may decide to draw less depending on its SoC
	 */
	public void addChargedCapacity(int deltaSeconds, double maxCurrentAllowed) {
		this.carBattery.addChargedCapacity(deltaSeconds, maxCurrentAllowed);
	}

	public double getMaxCapacity() {
		return carBattery.getMaxCapacity();
	}

	/**
	 * Directly add current in ampereHours
	 * 
	 * @param ampereHours
	 */
	/*
	 * @Deprecated public void addChargedCapacity(double ampereHours) {
	 * this.addChargedCapacity(3600, ampereHours); }
	 */

	public void resetChargedCapacity() {
		this.carBattery.resetChargedCapacity();
	}

	public void setChargedCapacity(double chargedCapacity) {
		this.carBattery.setChargedCapacity(chargedCapacity);
	}

	/*
	 * public void addChargedToCurrentCapacity(){ this.curCapacity +=
	 * chargedCapacity; this.chargedCapacity = 0; }
	 */

	@JsonIgnore
	public int getFirstAvailableTimeslot() {
		if (availStartCache == null) {
			for (int k = 0; k < availableTimeslots.length; k++) {
				if (availableTimeslots[k]) {
					availStartCache = k;
					return k;
				}
			}
			availStartCache = -1;
		}
		return availStartCache;
	}

	@JsonIgnore
	public int getLastAvailableTimeslot() {
		if (availEndCache == null) {
			for (int k = availableTimeslots.length - 1; k >= 0; k--) {
				if (availableTimeslots[k]) {
					availEndCache = k;
					return k;
				}
			}
			availEndCache = -1;
		}
		return availEndCache;
	}

	@JsonIgnore
	public boolean isImmediateStartNeeded() {
		return immediateStart;
	}

	public boolean isSuspendable() {
		return suspendable;
	}

	@JsonGetter("canUseVariablePower")
	public boolean canUseVariablePower() {
		return canUseVariablePower;
	}

	/**
	 * Is this a plugin hybrid electric vehicle (PHEV)?
	 * 
	 * @return
	 */
	@JsonIgnore
	public boolean isPHEV() {
		return this.carType == CarType.PHEV;
	}

	/**
	 * Is this a battery electric vehicle (BEV)?
	 * 
	 * @return
	 */
	@JsonIgnore
	public boolean isBEV() {
		return this.carType == CarType.BEV;
	}
	
	public CarType getCarType() {
		return this.carType; 
	}

	/**
	 * An ideal car has no immediateStart (immediateStart=false), is suspendable
	 * (suspendable=true) and can use variable power (canUseVariablePower=true)
	 * 
	 * @param ideal
	 */
	public void setIdealCar(boolean ideal) {
		this.immediateStart = !ideal;
		this.suspendable = ideal;
		this.canUseVariablePower = ideal;
	}

	public boolean isChargingStarted() {
		return chargingStarted;
	}

	public void setChargingStarted(boolean chargingStarted) {
		this.chargingStarted = chargingStarted;
	}

	public double[] getCurrentPlan() {
		return currentPlan;
	}

	/*
	 * public double getCurrentPerPhase(int timeslot) { // How much current will the
	 * car use at k? If nonlinear charging is active, // car may decide based on
	 * exponential function return carBattery.getCurrentPerPhase(timeslot); }
	 */

	public void setCurrentPlan(double[] currentPlan) {
		if (currentPlan != null && currentPlan.length != availableTimeslots.length) {
			log(1, "Car::setCurrentPlan WARNING:" + " currentPlan length (" + currentPlan.length + ") !="
					+ " availableTimeslots (" + availableTimeslots.length + ")");
		}
		this.currentPlan = currentPlan;
	}

	public double getChargedCapacity() {
		return this.carBattery.getChargedCapacity();
	}

	public void resetCaches() {
		availStartCache = null;
		availEndCache = null;
	}

	@Override
	public String toString() {
		return "Car [ID=" + id + ", name=" + getName() + ", min(k)=" + getFirstAvailableTimeslot() + ", max(k)="
				+ getLastAvailableTimeslot() + ", timestampArrival=" + timestampArrival.toSecondOfDay() + "s"
				+ ", timestampDeparture=" + timestampDeparture.toSecondOfDay() + "s" + ", modelName=" + modelName
				+ ", carType=" + carType.name() + ", curCapacity=" + Util.formatDouble(carBattery.getCurrentCapacity())
				+ "Ah" + ", chargedCapacity=" + Util.formatDouble(getChargedCapacity()) + "Ah" + ", maxCapacity="
				+ Util.formatDouble(carBattery.getMaxCapacity()) + "Ah" + ", minLoadingState="
				+ Util.formatDouble(minLoadingState) + "Ah" + ", canLoad=[" + canLoadPhase1 + "," + canLoadPhase2 + ","
				+ canLoadPhase3 + "]" + ", minCurrent=" + Util.formatDouble(minCurrent) + "A" + ", minCurrentPerPhase="
				+ Util.formatDouble(minCurrentPerPhase) + "A" + ", maxCurrent=" + Util.formatDouble(maxCurrent) + "A"
				+ ", maxCurrentPerPhase=" + Util.formatDouble(maxCurrentPerPhase) + "A" + ", suspendable=" + suspendable
				+ ", canUseVariablePower=" + canUseVariablePower + ", chargingStarted=" + chargingStarted
				+ ", immediateStart=" + immediateStart + ", nonlinearCharging=" + carBattery.getNonlinearCharging()
				+ "]";
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJSONObject() {
		JSONObject result = new JSONObject(new LinkedHashMap<>());
		result.put(JSONKeys.JSON_KEY_NAME, getName());
		result.put(JSONKeys.JSON_KEY_MODEL_NAME, modelName);
		result.put(JSONKeys.JSON_KEY_CAR_TYPE, carType.name());
		result.put(JSONKeys.JSON_KEY_INDEX_N, getId());

		result.put(JSONKeys.JSON_KEY_CAR_MAX_CAPACITY, this.carBattery.getMaxCapacity());
		result.put(JSONKeys.JSON_KEY_CAR_CUR_CAPACITY, this.carBattery.getCurrentCapacity());
		result.put(JSONKeys.JSON_KEY_CAR_CHARGED_CAPACITY, this.carBattery.getChargedCapacity());
		result.put(JSONKeys.JSON_KEY_CAR_MIN_LOADING_STATE, this.minLoadingState);

		result.put(JSONKeys.JSON_KEY_CAR_MIN_CURRENT, this.minCurrent);
		result.put(JSONKeys.JSON_KEY_CAR_MIN_CURRENT_PER_PHASE, this.minCurrentPerPhase);
		result.put(JSONKeys.JSON_KEY_CAR_MAX_CURRENT, this.maxCurrent);
		result.put(JSONKeys.JSON_KEY_CAR_MAX_CURRENT_PER_PHASE, this.maxCurrentPerPhase);

		result.put(JSONKeys.JSON_KEY_CAR_CAN_LOAD_PHASE_1, this.canLoadPhase1);
		result.put(JSONKeys.JSON_KEY_CAR_CAN_LOAD_PHASE_2, this.canLoadPhase2);
		result.put(JSONKeys.JSON_KEY_CAR_CAN_LOAD_PHASE_3, this.canLoadPhase3);

		result.put(JSONKeys.JSON_KEY_CAR_FIRST_AVAILABLE_TIMESLOT, this.getFirstAvailableTimeslot());
		result.put(JSONKeys.JSON_KEY_CAR_LAST_AVAILABLE_TIMESLOT, this.getLastAvailableTimeslot());

		if (timestampArrival != null && timestampDeparture != null) {
			result.put(JSONKeys.JSON_KEY_CAR_TIMESTAMP_ARRIVAL, timestampArrival.toSecondOfDay());
			result.put(JSONKeys.JSON_KEY_CAR_TIMESTAMP_DEPARTURE, timestampDeparture.toSecondOfDay());
		}

		result.put(JSONKeys.JSON_KEY_CAR_IMMEDIATE_START, this.immediateStart);
		result.put(JSONKeys.JSON_KEY_CAR_SUSPENDABLE, this.suspendable);
		result.put(JSONKeys.JSON_KEY_CAR_VARIABLE_POWER, this.canUseVariablePower);

		JSONArray jsonCurrentPlan = new JSONArray();
		if (currentPlan != null) {
			for (int k = 0; k < currentPlan.length; k++)
				jsonCurrentPlan.add(currentPlan[k]);
		}

		result.put(JSONKeys.JSON_KEY_CURRENT_PLAN, jsonCurrentPlan);
		return result;
	}

	public static Car fromJSON(JSONObject o, int nTimeslots) {

		int startAvailable = JSONSerializable
				.getJSONAttributeAsInt(o.get(JSONKeys.JSON_KEY_CAR_FIRST_AVAILABLE_TIMESLOT));
		int endAvailable = JSONSerializable.getJSONAttributeAsInt(o.get(JSONKeys.JSON_KEY_CAR_LAST_AVAILABLE_TIMESLOT));

		CarFactory builder = CarFactory.builder().availableTimeslots(startAvailable, endAvailable, nTimeslots);

		if (o.get(JSONKeys.JSON_KEY_CAR_TIMESTAMP_ARRIVAL) != null
				&& o.get(JSONKeys.JSON_KEY_CAR_TIMESTAMP_DEPARTURE) != null) {
			int secondsArrival = JSONSerializable.getJSONAttributeAsInt(o.get(JSONKeys.JSON_KEY_CAR_TIMESTAMP_ARRIVAL));
			int secondsDeparture = JSONSerializable
					.getJSONAttributeAsInt(o.get(JSONKeys.JSON_KEY_CAR_TIMESTAMP_DEPARTURE));
			builder.availableTimestamps(TimeUtil.getTimestampFromSeconds(secondsArrival),
					TimeUtil.getTimestampFromSeconds(secondsDeparture));
		}

		Car result = builder.id(JSONSerializable.getJSONAttributeAsInt(o.get(JSONKeys.JSON_KEY_INDEX_N)))
				.name((String) o.get(JSONKeys.JSON_KEY_NAME)).modelName((String) o.get(JSONKeys.JSON_KEY_MODEL_NAME))
				.phases((double) o.get(JSONKeys.JSON_KEY_CAR_CAN_LOAD_PHASE_1),
						(double) o.get(JSONKeys.JSON_KEY_CAR_CAN_LOAD_PHASE_2),
						(double) o.get(JSONKeys.JSON_KEY_CAR_CAN_LOAD_PHASE_3))
				.carType(CarType.valueOf((String) o.get(JSONKeys.JSON_KEY_CAR_TYPE)))
				.currentCapacity((double) o.get(JSONKeys.JSON_KEY_CAR_CUR_CAPACITY))
				.maxCapacity((double) o.get(JSONKeys.JSON_KEY_CAR_MAX_CAPACITY))
				.minLoadingState((double) o.get(JSONKeys.JSON_KEY_CAR_MIN_LOADING_STATE))
				.minCurrent((double) o.get(JSONKeys.JSON_KEY_CAR_MIN_CURRENT))
				.maxCurrent((double) o.get(JSONKeys.JSON_KEY_CAR_MAX_CURRENT))
				.suspendable((boolean) o.get(JSONKeys.JSON_KEY_CAR_SUSPENDABLE))
				.canUseVariablePower((boolean) o.get(JSONKeys.JSON_KEY_CAR_VARIABLE_POWER))
				.immediateStart((boolean) o.get(JSONKeys.JSON_KEY_CAR_IMMEDIATE_START)).build();


		result.setChargedCapacity((double) o.get(JSONKeys.JSON_KEY_CAR_CHARGED_CAPACITY));
		if (o.get(JSONKeys.JSON_KEY_CURRENT_PLAN) != null) {
			result.currentPlan = JSONSerializable.getJSONAttributeAsDoubleArray(o.get(JSONKeys.JSON_KEY_CURRENT_PLAN));
		}
		return result;
	}

	public void setCarProcessData(CarProcessData carProcessData) {
		this.carProcessData = carProcessData;
	}

	@JsonIgnore
	public CarProcessData getCarProcessData() {
		return this.carProcessData;
	}

	@Override
	public int getVerbosity() {
		return Simulation.verbosity;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Car other = (Car) obj;
		if (!(getFirstAvailableTimeslot()==other.getFirstAvailableTimeslot() && getLastAvailableTimeslot()==other.getLastAvailableTimeslot()))
			return false;
		if (Double.doubleToLongBits(canLoadPhase1) != Double.doubleToLongBits(other.canLoadPhase1))
			return false;
		if (Double.doubleToLongBits(canLoadPhase2) != Double.doubleToLongBits(other.canLoadPhase2))
			return false;
		if (Double.doubleToLongBits(canLoadPhase3) != Double.doubleToLongBits(other.canLoadPhase3))
			return false;
		if (canUseVariablePower != other.canUseVariablePower)
			return false;
		if (carBattery == null) {
			if (other.carBattery != null)
				return false;
		} else if (!carBattery.equals(other.carBattery))
			return false;
		if (carType != other.carType)
			return false;
		if (!Arrays.equals(currentPlan, other.currentPlan))
			return false;
		if (id != other.id)
			return false;
		if (immediateStart != other.immediateStart)
			return false;
		if (Double.doubleToLongBits(maxCurrent) != Double.doubleToLongBits(other.maxCurrent))
			return false;
		if (Double.doubleToLongBits(maxCurrentPerPhase) != Double.doubleToLongBits(other.maxCurrentPerPhase))
			return false;
		if (Double.doubleToLongBits(minCurrent) != Double.doubleToLongBits(other.minCurrent))
			return false;
		if (Double.doubleToLongBits(minCurrentPerPhase) != Double.doubleToLongBits(other.minCurrentPerPhase))
			return false;
		if (Double.doubleToLongBits(minLoadingState) != Double.doubleToLongBits(other.minLoadingState))
			return false;
		if (modelName == null) {
			if (other.modelName != null)
				return false;
		} else if (!modelName.equals(other.modelName))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (nrOfUsedPhases != other.nrOfUsedPhases)
			return false;
		if (Double.doubleToLongBits(sumUsedPhases) != Double.doubleToLongBits(other.sumUsedPhases))
			return false;
		if (suspendable != other.suspendable)
			return false;
		if (timestampArrival == null) {
			if (other.timestampArrival != null)
				return false;
		} else if (!timestampArrival.equals(other.timestampArrival))
			return false;
		if (timestampDeparture == null) {
			if (other.timestampDeparture != null)
				return false;
		} else if (!timestampDeparture.equals(other.timestampDeparture))
			return false;
		return true;
	}

}
