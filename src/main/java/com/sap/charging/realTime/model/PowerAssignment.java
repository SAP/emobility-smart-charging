package com.sap.charging.realTime.model;

import org.json.simple.JSONObject;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.util.JSONKeys;

public class PowerAssignment extends Assignment {
	
	public double phase1;
	public double phase2;
	public double phase3;
	
	public PowerAssignment(ChargingStation chargingStation, Car car,
			double phase1, double phase2, double phase3) {
		super(car, chargingStation);
		this.phase1 = phase1;
		this.phase2 = phase2;
		this.phase3 = phase3;
	}
	
	public double getPhaseByInt(int j) {
		switch (j) {
		case 1: return phase1;
		case 2: return phase2;
		case 3: return phase3;
		default: 
			throw new IllegalArgumentException("Phase " + j + " does not exist!");
		}
	}
	
	@Override
	public double getCurrentByPhaseAtGrid(Phase phaseAtGrid, int timeslot) {
		return getPhaseByInt(phaseAtGrid.asInt()); 
	}
	
	
	@Override
	public String toString() {
		return "ChargingStation=" + chargingStation.toString() + ", phase1=" + phase1 + "A, phase2=" + phase2 + "A, phase3=" + phase3 + "A";
	}

	
	
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJSONObject() {
		JSONObject result = new JSONObject();
		result.put(JSONKeys.JSON_KEY_INDEX_I, chargingStation.getId());
		result.put(JSONKeys.JSON_KEY_INDEX_N, car.getId());
		result.put(JSONKeys.JSON_KEY_PHASE_1, phase1);
		result.put(JSONKeys.JSON_KEY_PHASE_2, phase2);
		result.put(JSONKeys.JSON_KEY_PHASE_3, phase3);
		return result;
	}
	
}
