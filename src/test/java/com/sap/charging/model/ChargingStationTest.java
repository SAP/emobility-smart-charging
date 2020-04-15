package com.sap.charging.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.charging.model.CarFactory.CarModel;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.sim.common.SimulationUnitTest;

public class ChargingStationTest extends SimulationUnitTest {

	Car onePhaseCar; 
    Fuse parentFuse; 
	
    @BeforeEach
    public void setup() {
    	this.onePhaseCar = CarFactory.builder().set(CarModel.MERCEDES_GLC_350e)
    			.availableTimeslots(0, 95, 96)
    			.build();
    	this.parentFuse = (Fuse) this.dataSim.getChargingStation(0).getParent(); 
    }
    
    @Test
    public void test_IsPhaseConnectedInFuseTree_ChargingStation_NotConnected() {
    	// Charging station connected on first phase
    	ChargingStation chargingStation =  new ChargingStation(0, 32d, 0d, 0d, true, false, false, null, null);
    	parentFuse.addChild(chargingStation);
    	
    	// No phase rotation
    	assertTrue(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_1));
    	assertFalse(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_2));
    	assertFalse(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_3));
    	
    	assertTrue(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_1));
    	assertFalse(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_2));
    	assertFalse(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_3));
    	
    }
    
    @Test
    public void test_IsPhaseConnectedInFuseTree_ChargingStationPhaseRotation_NotConnected() {
    	
    	// Charging station connected on first phase
    	ChargingStation chargingStation =  new ChargingStation(0, 32d, 0d, 0d, true, false, false, null, null);
    
    	// 2,3,1 phase rotation
    	chargingStation.setPhaseMatching(Phase.PHASE_2, Phase.PHASE_3, Phase.PHASE_1);
    	assertFalse(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_1));
    	assertTrue(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_2));
    	assertFalse(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_3));
    	
    	assertTrue(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_1));
    	assertFalse(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_2));
    	assertFalse(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_3));
    	
    	// 3,1,2 phase rotation
    	chargingStation.setPhaseMatching(Phase.PHASE_3, Phase.PHASE_1, Phase.PHASE_2);
    	assertFalse(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_1));
    	assertFalse(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_2));
    	assertTrue(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_3));
    	
    	assertTrue(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_1));
    	assertFalse(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_2));
    	assertFalse(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_3));
    	
    }
    
    @Test
    public void test_IsPhaseConnectedInFuseTree_Fuse_NotConnected() {
    	ArrayList<FuseTreeNode> childrenRoot = new ArrayList<>(); 
    	Fuse root = new Fuse(0, 0, 0, 0, true, true, false, childrenRoot); 

    	ChargingStation chargingStation =  new ChargingStation(0, 32d, 32d, 32d, true, true, true, null, null);
    	root.addChild(chargingStation);
    	
    	// Phase 3 (grid) is not connected due to fuse
    	
    	assertTrue(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_1));
    	assertTrue(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_2));
    	assertFalse(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_3));
    	
    	assertTrue(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_1));
    	assertTrue(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_2));
    	assertFalse(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_3));
    }

    
    @Test
    public void test_IsPhaseConnectedInFuseTree_FusePhaseRotation_NotConnected() {
    	ArrayList<FuseTreeNode> childrenRoot = new ArrayList<>(); 
    	Fuse root = new Fuse(0, 0, 0, 0, true, true, false, childrenRoot); 

    	ChargingStation chargingStation =  new ChargingStation(0, 32d, 32d, 32d, true, true, true, null, null);
    	root.addChild(chargingStation);

    	// 2,3,1 phase rotation
    	chargingStation.setPhaseMatching(Phase.PHASE_2, Phase.PHASE_3, Phase.PHASE_1);
    	assertTrue(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_1));
    	assertTrue(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_2));
    	assertFalse(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_3));
    	
    	assertTrue(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_1));
    	assertFalse(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_2));
    	assertTrue(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_3));
    	
    	// 3,1,2 phase rotation
    	chargingStation.setPhaseMatching(Phase.PHASE_3, Phase.PHASE_1, Phase.PHASE_2);
    	assertTrue(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_1));
    	assertTrue(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_2));
    	assertFalse(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_3));
    	
    	assertFalse(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_1));
    	assertTrue(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_2));
    	assertTrue(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_3));
    	
    }
    
    
    @Test
    public void test_IsPhaseConnectedInFuseTree_FuseTree_NotConnected() {
    	
    	ArrayList<FuseTreeNode> childrenRoot = new ArrayList<>(); 
    	Fuse root = new Fuse(0, 32, 0, 0, true, true, false, childrenRoot); 

    	ArrayList<FuseTreeNode> childrenFuse = new ArrayList<>(); 
    	Fuse childFuse = new Fuse(0, 32, 0, 0, true, false, true, childrenFuse); 
    	
    	ChargingStation chargingStation =  new ChargingStation(0, 0d, 32d, 32d, false, true, true, null, null);
    	
    	root.addChild(childFuse);
    	childFuse.addChild(chargingStation);
    	
    	// 1st phase fails because of charging station
    	// 2nd phase because of childFuse
    	// 3rd phase because of rootFuse
    	
    	assertFalse(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_1));
    	assertFalse(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_2));
    	assertFalse(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_3));
    	
    	assertFalse(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_1));
    	assertFalse(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_2));
    	assertFalse(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_3));
    }
    
    
    @Test
    public void test_IsPhaseConnectedInFuseTree_FuseTreePhaseRotation_NotConnected() {
    	
    	ArrayList<FuseTreeNode> childrenRoot = new ArrayList<>(); 
    	Fuse root = new Fuse(0, 32, 0, 0, true, true, false, childrenRoot); 

    	ArrayList<FuseTreeNode> childrenFuse = new ArrayList<>(); 
    	Fuse childFuse = new Fuse(0, 32, 0, 0, true, false, true, childrenFuse); 
    	
    	ChargingStation chargingStation =  new ChargingStation(0, 0d, 32d, 32d, false, true, true, null, null);
    	
    	root.addChild(childFuse);
    	childFuse.addChild(chargingStation);
    	
    	// 2nd and 3rd grid phases are blocked due to fuses
    	// 1st phase from charging station perspective is blocked
    	
    	// 2,3,1 phase rotation
    	chargingStation.setPhaseMatching(Phase.PHASE_2, Phase.PHASE_3, Phase.PHASE_1);
    	assertTrue(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_1));
    	assertFalse(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_2));
    	assertFalse(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_3));
    	
    	assertFalse(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_1));
    	assertFalse(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_2));
    	assertTrue(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_3));
    	
    	// 3,1,2 phase rotation
    	chargingStation.setPhaseMatching(Phase.PHASE_3, Phase.PHASE_1, Phase.PHASE_2);
    	assertTrue(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_1));
    	assertFalse(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_2));
    	assertFalse(chargingStation.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_3));
    	
    	assertFalse(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_1));
    	assertTrue(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_2));
    	assertFalse(chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_3));
    }
    

}
