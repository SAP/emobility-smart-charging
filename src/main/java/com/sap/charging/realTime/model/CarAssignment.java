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
	
	private double[] getCurrentArray() {
		return new double[3];
	}
	
	private void calculateCurrentArray(double[] currentArray, double currentPlanned, Phase phase1ChargingStation, Phase phase2ChargingStation, Phase phase3ChargingStation) {
		currentArray[0] = car.canLoadPhase(phase1ChargingStation.asInt()) * currentPlanned;
		currentArray[1] = car.canLoadPhase(phase2ChargingStation.asInt()) * currentPlanned;
		currentArray[2] = car.canLoadPhase(phase3ChargingStation.asInt()) * currentPlanned;
	}
	
	public double[] getCurrentPerGridPhase(int timeslot) {
		double[] currentAtGrid = getCurrentArray();
		double currentPlanned = car.getCurrentPlan()[timeslot];
		
		// Get actual phase 1 consumption 
		// Which phase is the grid phase 1 at the charging station?
		Phase phase1ChargingStation = chargingStation.getPhaseGridToChargingStation(Phase.PHASE_1);
		Phase phase2ChargingStation = chargingStation.getPhaseGridToChargingStation(Phase.PHASE_2);
		Phase phase3ChargingStation = chargingStation.getPhaseGridToChargingStation(Phase.PHASE_3);
		
		calculateCurrentArray(currentAtGrid, currentPlanned, phase1ChargingStation, phase2ChargingStation, phase3ChargingStation);
		
		return currentAtGrid;
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










