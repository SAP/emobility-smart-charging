package com.sap.charging.realTime.model;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.util.JSONSerializable;

public abstract class Assignment implements JSONSerializable {
	
	public final Car car;
	public final ChargingStation chargingStation;
	
	
	public Assignment(Car car, ChargingStation chargingStation) {
		this.car = car;
		this.chargingStation = chargingStation;
	}
	
	public abstract double getCurrentByPhase(Phase phase, int timeslot); 
	
	
	public final double[] getCurrentPerGridPhase(int timeslot) {
		
		double[] currentAtGrid = new double[3]; 
		
		// Get actual phase 1 consumption 
		// Which phase is the grid phase 1 at the charging station?
		
		// Example: 
		// EV with 16A~1
		// Station with 2,3,1 matching ==> 1st phase on station matches 2nd phase of grid
		// Result array: [0, 16, 0]
		
		Phase phase1ChargingStation = chargingStation.getPhaseGridToChargingStation(Phase.PHASE_1);
		Phase phase2ChargingStation = chargingStation.getPhaseGridToChargingStation(Phase.PHASE_2);
		Phase phase3ChargingStation = chargingStation.getPhaseGridToChargingStation(Phase.PHASE_3);
		
		currentAtGrid[0] = this.getCurrentByPhase(phase1ChargingStation, timeslot); 
		currentAtGrid[1] = this.getCurrentByPhase(phase2ChargingStation, timeslot); 
		currentAtGrid[2] = this.getCurrentByPhase(phase3ChargingStation, timeslot); 
		
		return currentAtGrid;
		
	}
	
	
}
