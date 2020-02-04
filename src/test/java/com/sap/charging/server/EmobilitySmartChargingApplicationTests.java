package com.sap.charging.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.StrategyAlgorithmicChargeScheduler;
import com.sap.charging.realTime.model.CarAssignmentStore;
import com.sap.charging.server.api.v1.OptimizeChargingProfilesController;
import com.sap.charging.server.api.v1.OptimizeChargingProfilesRequest;
import com.sap.charging.server.api.v1.OptimizeChargingProfilesResponse;
import com.sap.charging.server.api.v1.store.ChargingStationStore;
import com.sap.charging.server.api.v1.store.EventStore;
import com.sap.charging.server.api.v1.store.StateStore;
import com.sap.charging.sim.Simulation;
import com.sap.charging.sim.common.SimulationUnitTest;
import com.sap.charging.sim.event.EventType;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class EmobilitySmartChargingApplicationTests extends SimulationUnitTest {

	@LocalServerPort
	private int port;

	@Autowired
	private OptimizeChargingProfilesController controller;

	@Autowired
	private TestRestTemplate restTemplate;

	private StrategyAlgorithmicChargeScheduler scheduler; 
	ObjectMapper mapper; 	
	
	@BeforeEach
	public void setupEach() {
		Simulation.verbosity = 0; 
		this.initSimulation(this.dataSim);
		scheduler = ((StrategyAlgorithmic) strategy).getScheduler(); 
		mapper = new ObjectMapper(); 
	}

	@Test
	void testRESTEndpoint_singleCarAssignment() {
		assertThat(controller).isNotNull();
		
		Car car = state.getCar(0); 
		car.setCurrentCapacity(0);
		car.resetChargedCapacity();
		
		int currentTimeSeconds = car.timestampArrival.toSecondOfDay(); 
		ChargingStation chargingStation = state.getChargingStation(0); 
		
		List<ChargingStationStore> chargingStations = dataSim.getChargingStations().stream().map(station -> ChargingStationStore.fromChargingStation(station)).collect(Collectors.toList()); 
		List<CarAssignmentStore> carAssignments = new ArrayList<>(); 
		carAssignments.add(new CarAssignmentStore(car.getId(), chargingStation.getId())); 
		
		EventStore event = new EventStore(-1, -1, null, EventType.Reoptimize); 
		
		StateStore state = new StateStore(currentTimeSeconds, null, chargingStations, 100.0, dataSim.getCars(), dataSim.getEnergyPriceHistory(), carAssignments);
		
		OptimizeChargingProfilesRequest request = new OptimizeChargingProfilesRequest(state, event, 0); 
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Basic dXNlcjE6QkU0a3BUWkhrQ3BNTVZqMzh6cGo=");
		
		HttpEntity<OptimizeChargingProfilesRequest> entity = new HttpEntity<>(request, headers);
		
		ResponseEntity<Object> response = this.restTemplate.postForEntity("http://localhost:" + port + "/api/v1/OptimizeChargingProfiles", 
				entity, 
				Object.class); 
		
		
		OptimizeChargingProfilesResponse result = mapper.convertValue(response.getBody(), OptimizeChargingProfilesResponse.class); 
		
		double plannedCapacity = scheduler.getPlannedCapacity(chargingStation, result.cars.get(0), currentTimeSeconds);
		assertThat(plannedCapacity).isGreaterThan(car.getMaxCapacity()- (1e-8)); 
		assertEquals(car.getMaxCapacity(), plannedCapacity, 1);
		assertEquals(state.cars.size(), result.cars.size()); 
		
	}

}















