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
	
	public abstract double getCurrentByPhaseAtGrid(Phase phaseAtGrid, int timeslot); 
	
	
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
		
		currentAtGrid[0] = this.getCurrentByPhaseAtGrid(phase1ChargingStation, timeslot); 
		currentAtGrid[1] = this.getCurrentByPhaseAtGrid(phase2ChargingStation, timeslot); 
		currentAtGrid[2] = this.getCurrentByPhaseAtGrid(phase3ChargingStation, timeslot); 
		
		return currentAtGrid;
		
	}
	
	/*
	public boolean canLoadOnPhase(Phase phaseAtChargingStation, Phase phaseAtGrid) {
		// Example: 
		// Station with 2,3,1 matching
		// phaseAtChargingStation 
		// ==> PHASE_1 or PHASE_2 or PHASE3
		// phaseAtGrid 
		// ==> PHASE_2 or PHASE_3 or PHASE_1
		
		// Problem: What does 0A on fuses mean? 
		// Does this mean no connection (car could charge then) or actual 0A fuse (no current allowed, car could not charge then)

		// Idea: boolean per FuseTreeNode
		// by default "phase1Connected=true"
		// 
	}*/
	
	
}
