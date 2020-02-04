package com.sap.charging.realTime;

import java.util.Arrays;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.realTime.model.PowerAssignment;
import com.sap.charging.sim.eval.Validation;

public class StrategyGreedy extends Strategy {

	@Override
	public void reactCarArrival(State state, Car newCar) {
		// Assign car if possible (= free spot)
		if (state.isAnyChargingStationFree()) {
			ChargingStation chargingStation = state.getFirstFreeChargingStation();
			log(2, "Adding car n=" + newCar.getId() 
					+ " to i=" + chargingStation.getId()
					+ " at t=" + state.currentTimeSeconds + " (k=" + state.currentTimeslot + ")");
			
			state.addCarAssignment(newCar, chargingStation);
			
			
			
			newCar.setCurrentPlan(new double[state.energyPriceHistory.getNTimeslots()]);
			
			// Assign power consumption
			PowerAssignment powerAssignment = state.addPowerAssignment(newCar, chargingStation, 
					Math.min(chargingStation.fusePhase1, 
							 newCar.canLoadPhase1 * newCar.maxCurrentPerPhase), 
					Math.min(chargingStation.fusePhase2, 
							 newCar.canLoadPhase2 * newCar.maxCurrentPerPhase), 
					Math.min(chargingStation.fusePhase3,
							 newCar.canLoadPhase3 * newCar.maxCurrentPerPhase) 
					);
			//System.out.println(Arrays.toString(newCar.availableTimeslots));
			//System.out.println(newCar.timestampArrival.toSecondOfDay());
			for (int k=newCar.getFirstAvailableTimeslot(); k<=newCar.getLastAvailableTimeslot();k++) {
				newCar.getCurrentPlan()[k] = powerAssignment.phase1;
			}
			
			if (Validation.isFuseTreeValid(state) == false) {
				powerAssignment.phase1 = 0;
				powerAssignment.phase2 = 0;
				powerAssignment.phase3 = 0;
				Arrays.fill(newCar.getCurrentPlan(), 0);
			}
		}
		else {
			log(2, "No charging station was free for car n=" + newCar.getId()); 
		}
	}

	@Override
	public void reactCarDeparture(State state, Car car) {
		log(2, "Car n=" + car.getId() + " is leaving at "
			    + "t=" + state.currentTimeSeconds + " (k=" + state.currentTimeslot + ")");
	}

	@Override
	public void reactCarFinished(State state, Car carFinished) {
		
	}
	
	@Override
	public void reactEnergyPriceChange(State state, EnergyPriceHistory newEnergyPriceHistory) {
	}

	@Override
	public String getMethod() {
		return getMethodStatic();
	}

	public static String getMethodStatic() {
		return "realTimeGreedy";
	}
	

}
