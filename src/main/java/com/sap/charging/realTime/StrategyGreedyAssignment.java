package com.sap.charging.realTime;

import java.util.List;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.realTime.model.CarAssignment;
import com.sap.charging.realTime.model.PowerAssignment;
import com.sap.charging.sim.eval.Validation;

public class StrategyGreedyAssignment extends Strategy {

	private final List<CarAssignment> plannedCarAssignments;
	
	
	public StrategyGreedyAssignment(List<CarAssignment> plannedCarAssignments) {
		this.plannedCarAssignments = plannedCarAssignments;
	}
	
	
	@Override
	public void reactCarArrival(State state, Car newCar) {
		// Assign car according to planned assignment
		CarAssignment plannedCarAssignment = null;
		
		for (CarAssignment carAssignment : plannedCarAssignments) {
			if (carAssignment.car.getId() == newCar.getId()) {
				log(2, "Car n=" + newCar.getId() + " assigned to i=" + carAssignment.chargingStation.getId());
				plannedCarAssignment = carAssignment;
			}
		}
		
		if (plannedCarAssignment == null) {
			// Car was not assigned 
			log(2, "Car n=" + newCar.getId() + " was not assigned.");
			return;
		}
		
		ChargingStation chargingStation = plannedCarAssignment.chargingStation;
		
		// Assign car if possible (= free spot)
		log(2, "Adding car n=" + newCar.getId() 
				+ " to i=" + chargingStation.getId()
				+ " at t=" + state.currentTimeSeconds + " (k=" + state.currentTimeslot + ")");
		
		state.addCarAssignment(newCar, chargingStation);
		
		
		// Assign power consumption
		PowerAssignment powerAssignment = state.addPowerAssignment(newCar, chargingStation, 
				Math.min(chargingStation.fusePhase1, 
						 newCar.canLoadPhase1 * newCar.maxCurrentPerPhase), 
				Math.min(chargingStation.fusePhase2, 
						 newCar.canLoadPhase2 * newCar.maxCurrentPerPhase), 
				Math.min(chargingStation.fusePhase3,
						 newCar.canLoadPhase3 * newCar.maxCurrentPerPhase) 
				);
		
		if (Validation.isFuseTreeValid(state) == false) {
			powerAssignment.phase1 = 0;
			powerAssignment.phase2 = 0;
			powerAssignment.phase3 = 0;
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
		return "realTimeGreedyAssignment";
	}
	
}
