package com.sap.charging.realTime;

import java.util.Arrays;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.EnergyUtil.Phase;
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
			
			// Check whether each phase is connected throughout the fuse tree
			boolean phase1ConnectedInFuseTree = chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_1); 
			boolean phase2ConnectedInFuseTree = chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_2); 
			boolean phase3ConnectedInFuseTree = chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_3); 
					
			double phase1 = phase1ConnectedInFuseTree ? newCar.canLoadPhase1*Math.min(newCar.maxCurrentPerPhase, chargingStation.fusePhase1): 0; 
			double phase2 = phase2ConnectedInFuseTree ? newCar.canLoadPhase2*Math.min(newCar.maxCurrentPerPhase, chargingStation.fusePhase2): 0; 
			double phase3 = phase3ConnectedInFuseTree ? newCar.canLoadPhase3*Math.min(newCar.maxCurrentPerPhase, chargingStation.fusePhase3): 0; 
			
			
			// Assign power consumption
			PowerAssignment powerAssignment = state.addPowerAssignment(newCar, chargingStation, 
					phase1, phase2, phase3); 

			for (int k=newCar.getFirstAvailableTimeslot(); k<=newCar.getLastAvailableTimeslot();k++) {
				newCar.getCurrentPlan()[k] = powerAssignment.getPhase1();
			}
			
			if (Validation.isFuseTreeValid(state) == false) {
				powerAssignment.setPhase1(0);
				powerAssignment.setPhase2(0);
				powerAssignment.setPhase3(0);
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
