package com.sap.charging.server.api.v1.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.realTime.State;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.model.CarAssignment;
import com.sap.charging.realTime.model.CarAssignmentStore;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureOracle;
import com.sap.charging.server.api.v1.OptimizeChargingProfilesRequest;
import com.sap.charging.server.api.v1.exception.UnknownCarException;
import com.sap.charging.server.api.v1.exception.UnknownChargingStationException;
import com.sap.charging.sim.Simulation;
import com.sap.charging.sim.common.SimulationUnitTest;
import com.sap.charging.util.FileIO;

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
	public void test_ToState() {
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
	
	@Test
	public void test_PassFuseTree_ChargingStationParent_NotNull() throws JsonMappingException, JsonProcessingException {
		
		String jsonStateStore = FileIO.readFile("src/test/resources/testCasesJSON/testFuseTooSmall_4_Cars.json"); 
		
		ObjectMapper objectMapper = new ObjectMapper();
		OptimizeChargingProfilesRequest request = objectMapper.readValue(jsonStateStore, OptimizeChargingProfilesRequest.class);  
		StateStore stateStore = request.state; 
		
		State state = stateStore.toState(); 
		
		assertEquals(2, state.getChargingStationsOccupied().size()); 
		for (ChargingStation station : state.getChargingStationsOccupied()) {
			assertNotNull(station.getParent());
		}
	}
	
	@Test
	public void test_OptimizeState() throws JsonMappingException, JsonProcessingException {
		
		String jsonStateStore = FileIO.readFile("src/test/resources/testCasesJSON/testFuseTooSmall_4_Cars.json"); 
		
		ObjectMapper objectMapper = new ObjectMapper();
		OptimizeChargingProfilesRequest request = objectMapper.readValue(jsonStateStore, OptimizeChargingProfilesRequest.class);  
		StateStore stateStore = request.state; 
		
		
		State state = stateStore.toState(); 
		
		StrategyAlgorithmic strategy = new StrategyAlgorithmic(new CarDepartureOracle());
    	strategy.objectiveEnergyCosts.setWeight(0);
    	strategy.objectiveFairShare.setWeight(1);
    	strategy.setReoptimizeOnStillAvailableAfterExpectedDepartureTimeslot(true);
    	strategy.setRescheduleCarsWith0A(false);
		
    	Simulation.verbosity = 0; 
		
		strategy.reactReoptimize(state);
		
		// All cars should be planned to be fully charged 
		for (CarAssignment carAssignment : state.getCurrentCarAssignments()) {
			Car car = carAssignment.car; 
			ChargingStation chargingStation = carAssignment.chargingStation; 
			assertEquals(car.getMissingCapacity(), strategy.getScheduler().getPlannedCapacity(chargingStation, car, state.currentTimeSeconds), 1e-8); 
		}
		
		// However, cars should not charge at the same time
		Car car1 = state.getCar(0); 
		Car car2 = state.getCar(1); 
		int currentTimeslot = state.currentTimeslot; 
		assertNotEquals(car1.getCurrentPlan()[currentTimeslot], car2.getCurrentPlan()[currentTimeslot]);
		
	}
	
}






