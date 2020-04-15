package com.sap.charging.realTime.model;

import org.json.simple.JSONObject;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.util.JSONKeys;

public class PowerAssignment extends Assignment {
	
	private double phase1;
	private double phase2;
	private double phase3;
	
	/**
	 * A power assignment is always kept up to date with the actual power being drawn on each phase at the charging station.
	 * Over time, as the charging plan changes, the power assignment is also changed. 
	 * 
	 * Examples: 
	 * 3-phase EV on 3-phase charging station
	 * --> The power assignment is 32A, 32A, 32A
	 * 
	 * 1-phase EV on 3-phase charging station
	 * --> The power assignment is 32A, 0A, 0A
	 * 
	 * 3-phase EV on 1-phase charging station
	 * --> The power assignment is 32A, 0A, 0A
	 * 
	 * 1-phase EV on 1-phase charging station
	 * --> The power assignment is 32A, 0A, 0A
	 * 
	 * 
	 * @param chargingStation
	 * @param car
	 * @param phase1
	 * @param phase2
	 * @param phase3
	 */
	public PowerAssignment(ChargingStation chargingStation, Car car,
			double phase1, double phase2, double phase3) {
		super(car, chargingStation);
		this.setPhase1(phase1);
		this.setPhase2(phase2);
		this.setPhase3(phase3);
	}
	
	public double getPhase1() {
		return phase1;
	}

	public void setPhase1(double phase1) {
		this.phase1 = phase1;
	}

	public double getPhase2() {
		return phase2;
	}

	public void setPhase2(double phase2) {
		this.phase2 = phase2;
	}

	public double getPhase3() {
		return phase3;
	}

	public void setPhase3(double phase3) {
		this.phase3 = phase3;
	}

	public double getPhaseByInt(int j) {
		switch (j) {
		case 1: return getPhase1();
		case 2: return getPhase2();
		case 3: return getPhase3();
		default: 
			throw new IllegalArgumentException("Phase " + j + " does not exist!");
		}
	}
	
	@Override
	public double getCarCurrentByPhaseAtStation(Phase phaseAtStation, int timeslot) {
		return getPhaseByInt(phaseAtStation.asInt()); 
	}
	
	
	@Override
	public String toString() {
		return "ChargingStation=" + chargingStation.toString() + ", phase1=" + getPhase1() + "A, phase2=" + getPhase2() + "A, phase3=" + getPhase3() + "A";
	}

	
	
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJSONObject() {
		JSONObject result = new JSONObject();
		result.put(JSONKeys.JSON_KEY_INDEX_I, chargingStation.getId());
		result.put(JSONKeys.JSON_KEY_INDEX_N, car.getId());
		result.put(JSONKeys.JSON_KEY_PHASE_1, getPhase1());
		result.put(JSONKeys.JSON_KEY_PHASE_2, getPhase2());
		result.put(JSONKeys.JSON_KEY_PHASE_3, getPhase3());
		return result;
	}
	
}
