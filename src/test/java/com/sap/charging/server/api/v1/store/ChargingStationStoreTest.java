package com.sap.charging.server.api.v1.store;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.charging.model.Car;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.model.CarAssignmentStore;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureOracle;
import com.sap.charging.sim.common.SimulationUnitTest;

public class ChargingStationStoreTest extends SimulationUnitTest {
	
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
	public void test_validPhaseConnected_AllPhasesConnected() {
		ChargingStationStore store = new ChargingStationStore(0, 32d, 32d, 32d, true, true, true, null, null); 
		store.toChargingStation(); 
	}
	
	@Test
	public void test_validPhaseConnected_NotAllPhasesConnected() {
		ChargingStationStore store1 = new ChargingStationStore(0, 0d, 32d, 32d, false, true, true, null, null); 
		store1.toChargingStation(); 
		
		ChargingStationStore store2 = new ChargingStationStore(0, 32d, 0d, 32d, true, false, true, null, null); 
		store2.toChargingStation(); 
		
		ChargingStationStore store3 = new ChargingStationStore(0, 32d, 0d, 0d, true, true, false, null, null); 
		store3.toChargingStation(); 
	}
	
	
	
	@Test
	public void test_invalidPhaseConnected() {
		try {
			ChargingStationStore store = new ChargingStationStore(0, 32d, 32d, 32d, false, true, true, null, null); 
			store.toChargingStation(); 
			fail("Should have failed with invalid phase connected (phase 1>0 but not connected)"); 
		}
		catch (Exception e) {}
		
		try {
			ChargingStationStore store = new ChargingStationStore(0, 32d, 32d, 32d, true, false, true, null, null); 
			store.toChargingStation(); 
			fail("Should have failed with invalid phase connected (phase 2>0 but not connected)"); 
		}
		catch (Exception e) {}
		
		try {
			ChargingStationStore store = new ChargingStationStore(0, 32d, 32d, 32d, false, true, false, null, null); 
			store.toChargingStation(); 
			fail("Should have failed with invalid phase connected (phase 3>0 but not connected)"); 
		}
		catch (Exception e) {}
	}
	
}






