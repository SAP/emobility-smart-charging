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
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.model.Fuse;
import com.sap.charging.model.FuseTree;
import com.sap.charging.realTime.State;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.StrategyAlgorithmicChargeScheduler;
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
	
	StrategyAlgorithmic strategy; 
	
	@BeforeEach
	public void setup() {
		chargingStations = dataSim.getChargingStations()
				.stream()
				.map(chargingStation -> ChargingStationStore.fromChargingStation(chargingStation))
				.collect(Collectors.toList());
		cars = dataSim.getCars(); 
		carAssignments = new ArrayList<>(); 
		
		strategy = new StrategyAlgorithmic(new CarDepartureOracle());
    	strategy.objectiveEnergyCosts.setWeight(0);
    	strategy.objectiveFairShare.setWeight(1);
    	strategy.setReoptimizeOnStillAvailableAfterExpectedDepartureTimeslot(true);
    	strategy.setRescheduleCarsWith0A(false);
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
		
		String jsonStateStore = FileIO.readFile("src/test/resources/testCasesJSON/StateStoreTest_FuseTooSmall_4_Cars.json"); 
		
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
	public void test_OptimizeState_WithUnassignedCars() throws JsonMappingException, JsonProcessingException {
		
		String jsonStateStore = FileIO.readFile("src/test/resources/testCasesJSON/StateStoreTest_FuseTooSmall_4_Cars.json"); 
		
		ObjectMapper objectMapper = new ObjectMapper();
		OptimizeChargingProfilesRequest request = objectMapper.readValue(jsonStateStore, OptimizeChargingProfilesRequest.class);  
		StateStore stateStore = request.state; 
		
		
		State state = stateStore.toState(); 
		
		
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
	
	
	@Test
	public void test_OptimizeState_SinglePhaseCar_StationWithPhaseRotation_SinglePhaseFuse() throws JsonMappingException, JsonProcessingException {
		// See also StrategyAlgorithmicTest.testRescheduleCar_SinglePhaseCar_StationWithPhaseRotation_SinglePhaseFuse
		
		String jsonStateStore = FileIO.readFile("src/test/resources/testCasesJSON/StateStoreTest_SinglePhaseCar_StationWithPhaseRotation_SinglePhaseFuse.json"); 
		
		ObjectMapper objectMapper = new ObjectMapper();
		OptimizeChargingProfilesRequest request = objectMapper.readValue(jsonStateStore, OptimizeChargingProfilesRequest.class);  
		StateStore stateStore = request.state; 
		
		State state = stateStore.toState(); 
		
		Simulation.verbosity = 0; 
		strategy.reactReoptimize(state);
		Car car = state.getCar(0); 
		
		for (int k=0; k<96; k++) {
			assertEquals(0, car.getCurrentPlan()[k], 1e-8); 
		}
	}
	
	
	@Test
	public void test_OptimizeState_ThreePhaseCar_SinglePhaseStation() throws JsonMappingException, JsonProcessingException {
		String jsonStateStore = FileIO.readFile("src/test/resources/testCasesJSON/StateStoreTest_ThreePhaseCar_SinglePhaseStation.json"); 
		
		ObjectMapper objectMapper = new ObjectMapper();
		OptimizeChargingProfilesRequest request = objectMapper.readValue(jsonStateStore, OptimizeChargingProfilesRequest.class);  
		StateStore stateStore = request.state; 
		
		State state = stateStore.toState(); 
		
    	Simulation.verbosity = 0; 
		strategy.reactReoptimize(state);
		Car car = state.getCar(0); 
		ChargingStation station = state.getChargingStation(0); 
		
		assertEquals(3, car.sumUsedPhases, 0); 
		
		StrategyAlgorithmicChargeScheduler scheduler = strategy.getScheduler(); 
		assertEquals(1, scheduler.getSumUsedPhases(station, car), 0); 
		
		// Should be fully planned
		double desiredCapacity = car.getMissingCapacity(); 
		double plannedCapacity = scheduler.getPlannedCapacity(station, car, state.currentTimeSeconds);
		assertEquals(desiredCapacity, plannedCapacity, 1e-8); 
		
	}
	
	@Test
	public void test_OptimizeState_ThreePhaseCar_SinglePhaseStationWithPhaseRotation() throws JsonMappingException, JsonProcessingException {
		String jsonStateStore = FileIO.readFile("src/test/resources/testCasesJSON/StateStoreTest_ThreePhaseCar_SinglePhaseStationWithPhaseRotation.json"); 
		
		ObjectMapper objectMapper = new ObjectMapper();
		OptimizeChargingProfilesRequest request = objectMapper.readValue(jsonStateStore, OptimizeChargingProfilesRequest.class);  
		StateStore stateStore = request.state; 
		
		State state = stateStore.toState(); 
		
    	Simulation.verbosity = 0; 
		strategy.reactReoptimize(state);
		Car car = state.getCar(0); 
		ChargingStation station = state.getChargingStation(0); 
		
		assertEquals(3, car.sumUsedPhases, 0); 
		
		assertEquals(true, station.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_1));
		assertEquals(false, station.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_2));
		assertEquals(false, station.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_3));
		
		assertEquals(false, station.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_1));
		assertEquals(true, station.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_2));
		assertEquals(false, station.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_3));
		
		
		StrategyAlgorithmicChargeScheduler scheduler = strategy.getScheduler(); 
		assertEquals(1, scheduler.getSumUsedPhases(station, car), 0); 
		
		// Should be fully planned
		double desiredCapacity = car.getMissingCapacity(); 
		double plannedCapacity = scheduler.getPlannedCapacity(station, car, state.currentTimeSeconds);
		assertEquals(desiredCapacity, plannedCapacity, 1e-8); 
		
	}
	
	@Test
	public void test_FuseTree_And_ChargingStations_Passed_ShouldError() {
		
		ChargingStation station = new ChargingStation(); 
		station.setID(0);
		station.fusePhase1 = 10.666;
		station.fusePhase2 = 10.666;
		station.fusePhase3 = 10.666;
		
		Fuse childFuse = new Fuse(1, 10.666); 
		childFuse.addChild(station);
		Fuse rootFuse = new Fuse(0, 2.89); 
		rootFuse.addChild(childFuse);
		
		FuseTree fuseTree = new FuseTree(rootFuse, 1); 
		
		chargingStations = new ArrayList<ChargingStationStore>(); 
		chargingStations.add(ChargingStationStore.fromChargingStation(station)); 
		
		Car car = cars.get(0);
		cars = new ArrayList<Car>(); 
		cars.add(car); 
		
		
		carAssignments.add(new CarAssignmentStore(car.getId(), station.getId())); 
		
		int currentTimeSeconds = car.timestampArrival.toSecondOfDay();  
		
		try {
			new StateStore(currentTimeSeconds, fuseTree, chargingStations, null, cars, null, carAssignments); 
			fail("Should have failed when passing fuseTree and a separate chargingStations array"); 
		}
		catch (Exception e) {}
		
		
		
	}
	
	
	
	
	
}






