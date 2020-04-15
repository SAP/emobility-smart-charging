package com.sap.charging.realTime.model;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.charging.model.Car;
import com.sap.charging.model.CarFactory;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.CarFactory.CarModel;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.sim.common.SimulationUnitTest;

public class PowerAssignmentTest  extends SimulationUnitTest {

	Car onePhaseCar; 
    
    @BeforeEach
    public void setup() {
    	this.onePhaseCar = CarFactory.builder().set(CarModel.MERCEDES_GLC_350e)
    			.availableTimeslots(0, 95, 96)
    			.build();
    }
    
	@Test
	public void testCurrentCalculationNoPhaseRotation() {
		ChargingStation chargingStation = dataSim.getChargingStation(0);

		PowerAssignment powerAssignment = new PowerAssignment(chargingStation, onePhaseCar, 
				onePhaseCar.maxCurrent, 0d, 0d);

		double[] currentPerGridPhase0 = powerAssignment.getCurrentPerGridPhase(-1);
		assertEquals(onePhaseCar.maxCurrent, currentPerGridPhase0[0], 1e-8);
		assertEquals(0, currentPerGridPhase0[1], 1e-8);
		assertEquals(0, currentPerGridPhase0[2], 1e-8);

	}

	@Test
	public void testCurrentCalculationWithPhaseRotation() {
		
		ChargingStation chargingStation = dataSim.getChargingStation(0);

		PowerAssignment powerAssignment = new PowerAssignment(chargingStation, onePhaseCar, 
				onePhaseCar.maxCurrent, 0d, 0d);

		// Matching is 1->2, 2->3, 3->1 (1st phase of charging station is connected to
		// 2nd phase of grid)
		chargingStation.setPhaseMatching(Phase.PHASE_2, Phase.PHASE_3, Phase.PHASE_1);

		double[] currentPerGridPhase = powerAssignment.getCurrentPerGridPhase(1);
		assertEquals(0, currentPerGridPhase[0], 1e-8);
		assertEquals(onePhaseCar.maxCurrent, currentPerGridPhase[1], 1e-8);
		assertEquals(0, currentPerGridPhase[2], 1e-8);

		// Matching is 1->2, 2->3, 3->1 (1st phase of charging station is connected to
		// 3rd phase of grid)
		chargingStation.setPhaseMatching(Phase.PHASE_3, Phase.PHASE_1, Phase.PHASE_2);
		currentPerGridPhase = powerAssignment.getCurrentPerGridPhase(1);
		assertEquals(0, currentPerGridPhase[0], 1e-8);
		assertEquals(0, currentPerGridPhase[1], 1e-8);
		assertEquals(onePhaseCar.maxCurrent, currentPerGridPhase[2], 1e-8);

	}

}
