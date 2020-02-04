package com.sap.charging.server.api.v1.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.charging.model.Car;
import com.sap.charging.realTime.State;
import com.sap.charging.realTime.model.CarAssignmentStore;
import com.sap.charging.server.api.v1.exception.UnknownCarException;
import com.sap.charging.server.api.v1.exception.UnknownChargingStationException;
import com.sap.charging.sim.common.SimulationUnitTest;

public class StateStoreTest extends SimulationUnitTest {
	
	List<ChargingStationStore> chargingStations; 
	List<Car> cars; 
	final double maximumSiteLimitKW = 100; 
	List<CarAssignmentStore> carAssignments; 
	
	@BeforeEach
	public void setup() {
		chargingStations = dataSim.getChargingStations()
				.stream()
				.map(chargingStation -> ChargingStationStore.fromChargingStation(chargingStation))
				.collect(Collectors.toList());
		cars = dataSim.getCars(); 
		carAssignments = new ArrayList<>(); 
	}
	
	@Test
	public void testToState() {
		StateStore stateStore = new StateStore(0, null, chargingStations, maximumSiteLimitKW, cars, null, carAssignments); 
		State state = stateStore.toState(); 
		
		assertEquals(chargingStations.size(), state.nChargingStations); 
		assertEquals(cars.size(), state.cars.size());
		assertEquals(0, state.getCurrentCarAssignments().size()); 
	}
	
	@Test
	public void test_InvalidCarAssignment_invalidCarID() {
		carAssignments.add(new CarAssignmentStore(999, chargingStations.get(0).id)); 
		try {
			new StateStore(0, null, chargingStations, maximumSiteLimitKW, cars, null, carAssignments); 
			fail("Should have failed with unknown car ID"); 
		}
		catch (UnknownCarException e) {}
	}
	
	@Test
	public void test_InvalidCarAssignment_invalidChargingStation() {
		carAssignments.add(new CarAssignmentStore(cars.get(0).getId(), 999)); 
		try {
			new StateStore(0, null, chargingStations, maximumSiteLimitKW, cars, null, carAssignments); 
			fail("Should have failed with unknown chargingStation ID"); 
		}
		catch (UnknownChargingStationException e) {}
	}
	
}






