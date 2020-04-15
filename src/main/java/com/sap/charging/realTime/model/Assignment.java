package com.sap.charging.realTime.model;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.model.FuseTreeNode;
import com.sap.charging.util.JSONSerializable;

public abstract class Assignment implements JSONSerializable {
	
	public final Car car;
	public final ChargingStation chargingStation;
	
	
	public Assignment(Car car, ChargingStation chargingStation) {
		this.car = car;
		this.chargingStation = chargingStation;
	}
	
	public abstract double getCarCurrentByPhaseAtGrid(Phase phaseAtGrid, int timeslot); 
	
	
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
		
		// Check whether each phase is connected throughout the fuse tree
		boolean phase1ConnectedInFuseTree = this.isPhaseConnectedInFuseTree(Phase.PHASE_1, phase1ChargingStation); 
		boolean phase2ConnectedInFuseTree = this.isPhaseConnectedInFuseTree(Phase.PHASE_2, phase2ChargingStation); 
		boolean phase3ConnectedInFuseTree = this.isPhaseConnectedInFuseTree(Phase.PHASE_3, phase3ChargingStation); 

		currentAtGrid[0] = phase1ConnectedInFuseTree ? this.getCarCurrentByPhaseAtGrid(phase1ChargingStation, timeslot) : 0;
		currentAtGrid[1] = phase2ConnectedInFuseTree ? this.getCarCurrentByPhaseAtGrid(phase2ChargingStation, timeslot) : 0; 
		currentAtGrid[2] = phase3ConnectedInFuseTree ? this.getCarCurrentByPhaseAtGrid(phase3ChargingStation, timeslot) : 0; 
		
		return currentAtGrid;
		
	}
	
	
	/**
	 * Checks whether a phase connected throughout the complete fuse tree.
	 * If phase is not connected the EV does not charge on this phase.
	 * 
	 * Example: 
	 * Station with 2,3,1 matching
	 * phaseAtChargingStation 
	 * ==> PHASE_1 or PHASE_2 or PHASE3
	 * phaseAtGrid 
	 * ==> PHASE_2 or PHASE_3 or PHASE_1
	 * 
	 * 
	 * 
	 * @param phaseAtChargingStation
	 * @param phaseAtGrid
	 * @return
	 */
	public boolean isPhaseConnectedInFuseTree(Phase phaseAtChargingStation, Phase phaseAtGrid) {
		 
		// Check whether charging station is connected on its local phase
		if (chargingStation.isPhaseConnected(phaseAtChargingStation) == false) {
			return false; 
		}
		
		// Check the chain of parent FuseTreeNodes in the fuse tree if this phase is connected 
		FuseTreeNode parent = chargingStation.getParent(); 
		while (parent != null) {
			if (parent.isPhaseConnected(phaseAtGrid) == false) {
				return false; 
			}
			parent = parent.getParent(); 
		}
		
		return true; 
	}
	
	
}
