package com.sap.charging.dataGeneration;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.Fuse;
import com.sap.charging.model.FuseTree;
import com.sap.charging.model.EnergyUtil.Phase;

public class DataGeneratorFuseTreeTest {
	
	private static final int nChargingStationsPerBottomLevelFuseTree = 4;
	
	
	@Test
	public void testDataGenerationFuseTreeRotating() {
		DataGeneratorRandom dataGenerator = new DataGeneratorRandom(0, true);
		
		int nTimeslots = 1;
		int nCars = 1;
		int nChargingStations = 13;
		boolean doRotatePhases = true;
		
		dataGenerator.generateEnergyPriceHistory(nTimeslots)
			.generateCars(nCars)
			.generateChargingStations(nChargingStations)
			.generateFuseTree(nChargingStationsPerBottomLevelFuseTree, doRotatePhases);
		
		List<ChargingStation> chargingStations = dataGenerator.getChargingStations();
		FuseTree result = dataGenerator.getFuseTree();
		
		Fuse root = (Fuse) result.getRootFuse();
		assertEquals(0, root.getId());
		assertEquals(4000, root.getFusePhase(Phase.PHASE_1), 1e-8);
		assertEquals(4000, root.getFusePhase(Phase.PHASE_2), 1e-8);
		assertEquals(4000, root.getFusePhase(Phase.PHASE_3), 1e-8);
		
		assertEquals("Fuse l0: 3x 4000.0A", root.toString());
		assertTrue(root.hasChildren());
		Phase temp = null;
		assertEquals(-1, root.getFusePhase(temp), 1e-8);
		
		// Under configuration with nChargingStationsPerBottomLevelFuseTree=4
		// This should be false
		assertFalse(root.getChildren().get(2) // 1250A
						.getChildren().get(0) //  800A
						.hasChildren());
		
		// i=0: 1=1, 2=2, 3=3
		assertEquals(Phase.PHASE_1, chargingStations.get(0).getPhaseConsumed(1));
		assertEquals(Phase.PHASE_2, chargingStations.get(0).getPhaseConsumed(2));
		assertEquals(Phase.PHASE_3, chargingStations.get(0).getPhaseConsumed(3));
		assertFalse(chargingStations.get(0).hasChildren());
		
		// i=1: 1=2, 2=3, 3=1
		assertEquals(Phase.PHASE_2, chargingStations.get(1).getPhaseConsumed(1));
		assertEquals(Phase.PHASE_3, chargingStations.get(1).getPhaseConsumed(2));
		assertEquals(Phase.PHASE_1, chargingStations.get(1).getPhaseConsumed(3));
		
		// i=2: 1=3, 2=1, 3=2
		assertEquals(Phase.PHASE_3, chargingStations.get(2).getPhaseConsumed(1));
		assertEquals(Phase.PHASE_1, chargingStations.get(2).getPhaseConsumed(2));
		assertEquals(Phase.PHASE_2, chargingStations.get(2).getPhaseConsumed(3));
				
		// i=3: 1=1, 2=2, 3=3
		assertEquals(Phase.PHASE_1, chargingStations.get(3).getPhaseConsumed(1));
		assertEquals(Phase.PHASE_2, chargingStations.get(3).getPhaseConsumed(2));
		assertEquals(Phase.PHASE_3, chargingStations.get(3).getPhaseConsumed(3));
		
		// i=4: 1=1, 2=2, 3=3 (new sub level)
		assertEquals(Phase.PHASE_1, chargingStations.get(4).getPhaseConsumed(1));
		assertEquals(Phase.PHASE_2, chargingStations.get(4).getPhaseConsumed(2));
		assertEquals(Phase.PHASE_3, chargingStations.get(4).getPhaseConsumed(3));
		
	}
	
	
	@Test
	public void testDataGenerationFuseTreeNonRotating() {
		DataGeneratorRandom dataGenerator = new DataGeneratorRandom(0, true);
		
		int nTimeslots = 1;
		int nCars = 1;
		int nChargingStations = 13;
		boolean doRotatePhases = false;
		
		dataGenerator.generateEnergyPriceHistory(nTimeslots)
			.generateCars(nCars)
			.generateChargingStations(nChargingStations)
			.generateFuseTree(nChargingStationsPerBottomLevelFuseTree, doRotatePhases);
		
		List<ChargingStation> chargingStations = dataGenerator.getChargingStations();
		
		// Phase configurations should always be the same
		// i=0: 1=1, 2=2, 3=3
		for (int i=0;i<chargingStations.size();i++) {
			assertEquals(Phase.PHASE_1, chargingStations.get(i).getPhaseConsumed(1));
			assertEquals(Phase.PHASE_2, chargingStations.get(i).getPhaseConsumed(2));
			assertEquals(Phase.PHASE_3, chargingStations.get(i).getPhaseConsumed(3));
		}
	}
	
	
	
	
	
	
	
	
	
}
