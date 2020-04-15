package com.sap.charging.realTime.model;

import org.json.simple.JSONObject;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.opt.lp.Variable;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureForecast;
import com.sap.charging.util.JSONKeys;
import com.sap.charging.util.TimeUtil;

public class CarAssignment extends Assignment {

	private int expectedDepartureTimeSeconds = -1;
	
	public CarAssignment(Car car, ChargingStation chargingStation) {
		super(car, chargingStation);
		this.expectedDepartureTimeSeconds = CarDepartureForecast.getDefaultCarDepartureForecast().getExpectedDepartureTimeSeconds(null, null); 
	}
	
	public Variable toVariableX() {
		Variable variableX = new Variable("X", true);
		variableX.setIndex("i", chargingStation.getId());
		variableX.setIndex("n", car.getId());
		variableX.setValue(1);
		return variableX;
	}
	
	public static CarAssignment fromVariableX(Variable variableX) {
		return null;
	}
	
	@Override
	public String toString() {
		return "CarAssignment carId=" + this.car.getId() + ", chargingStationId=" + this.chargingStation.getId(); 
	}
	
	public void setExpectedDepartureTimeSeconds(int expectedDepartureTimeSeconds) {
		this.expectedDepartureTimeSeconds = expectedDepartureTimeSeconds;
	}

	public int getExpectedDepartureTimeSeconds() {
		return this.expectedDepartureTimeSeconds;
	}

	public int getExpectedDepartureTimeslot() {
		return TimeUtil.getTimeslotFromSeconds(getExpectedDepartureTimeSeconds());
	}
	
	@Override
	public double getCarCurrentByPhaseAtStation(Phase phaseAtStation, int timeslot) {
		double currentPlanned = car.getCurrentPlan()[timeslot];
		
		// Example: Car with 16A~1
		// Station with 2,3,1 matching 
		// Phase: Which phase is the grid phase 1 at the charging station?
		// --> Phase is 2 here
		
		double currentOnPhase = car.canLoadPhase(phaseAtStation) * currentPlanned;
		return currentOnPhase; 
	}
	
	
	

	
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJSONObject() {
		JSONObject result = new JSONObject();
		result.put(JSONKeys.JSON_KEY_INDEX_I, chargingStation.getId());
		result.put(JSONKeys.JSON_KEY_INDEX_N, car.getId());
		return result;
	}
}










