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
	
	public abstract double getCarCurrentByPhaseAtStation(Phase phaseAtStation, int timeslot); 
	
	
	public final double[] getCurrentPerGridPhase(int timeslot) {
		
		double[] currentAtGrid = new double[3]; 
		
		// Get actual phase 1 consumption 
		// Which phase is the grid phase 1 at the charging station?
		
		// Example: 
		// EV with 16A~1
		// Station with 2,3,1 matching ==> 1st phase on station matches 2nd phase of grid
		// Result array: [0, 16, 0]
		
		// How are phases mapped at charging station?
		Phase phase1ChargingStation = chargingStation.getPhaseGridToChargingStation(Phase.PHASE_1);
		Phase phase2ChargingStation = chargingStation.getPhaseGridToChargingStation(Phase.PHASE_2);
		Phase phase3ChargingStation = chargingStation.getPhaseGridToChargingStation(Phase.PHASE_3);
		
		// Check whether each phase is connected throughout the fuse tree
		boolean phase1ConnectedInFuseTree = chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_1); 
		boolean phase2ConnectedInFuseTree = chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_2); 
		boolean phase3ConnectedInFuseTree = chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_3); 

		currentAtGrid[0] = phase1ConnectedInFuseTree ? this.getCarCurrentByPhaseAtStation(phase1ChargingStation, timeslot) : 0;
		currentAtGrid[1] = phase2ConnectedInFuseTree ? this.getCarCurrentByPhaseAtStation(phase2ChargingStation, timeslot) : 0; 
		currentAtGrid[2] = phase3ConnectedInFuseTree ? this.getCarCurrentByPhaseAtStation(phase3ChargingStation, timeslot) : 0; 
		
		return currentAtGrid;
		
	}
	
	public final double[] getCurrentPerStationPhase(int timeslot) {
		
		double[] currentAtStation = new double[3]; 
		
		// Check whether each phase is connected throughout the fuse tree
		boolean phase1ConnectedInFuseTree = chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_1); 
		boolean phase2ConnectedInFuseTree = chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_2); 
		boolean phase3ConnectedInFuseTree = chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_3); 

		currentAtStation[0] = phase1ConnectedInFuseTree ? this.getCarCurrentByPhaseAtStation(Phase.PHASE_1, timeslot) : 0;
		currentAtStation[1] = phase2ConnectedInFuseTree ? this.getCarCurrentByPhaseAtStation(Phase.PHASE_2, timeslot) : 0; 
		currentAtStation[2] = phase3ConnectedInFuseTree ? this.getCarCurrentByPhaseAtStation(Phase.PHASE_3, timeslot) : 0; 
		
		return currentAtStation; 
	}
	
	
	
}
