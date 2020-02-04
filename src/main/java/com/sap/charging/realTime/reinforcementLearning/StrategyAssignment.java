package com.sap.charging.realTime.reinforcementLearning;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.realTime.State;
import com.sap.charging.realTime.Strategy;

public class StrategyAssignment extends Strategy {

	@Override
	public void reactCarArrival(State state, Car newCar) {
		// Assign car if possible (= free spot)
		if (state.isAnyChargingStationFree()) {
			ChargingStation chargingStation = state.getFirstFreeChargingStation();
			log(1, "StrategyAssignment::reactCarArrival "
					+ "Adding car n=" + newCar.getId() 
					+ " to i=" + chargingStation.getId()
					+ " at t=" + state.currentTimeSeconds + " (k=" + state.currentTimeslot + ")");
			
			state.addCarAssignment(newCar, chargingStation);
		}
	}

	@Override
	public void reactCarFinished(State state, Car carFinished) {
	}

	@Override
	public void reactCarDeparture(State state, Car carLeaving) {
	}

	@Override
	public void reactEnergyPriceChange(State state, EnergyPriceHistory newEnergyPriceHistory) {
	}

	@Override
	public String getMethod() {
		return "realTimeAssignment";
	}

}
