package com.sap.charging.server;

import static org.junit.Assert.assertEquals;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.Fuse;
import com.sap.charging.model.FuseTree;
import com.sap.charging.sim.Simulation;
import com.sap.charging.sim.common.SimulationUnitTest;

public class SerializationTest extends SimulationUnitTest {

	ObjectMapper mapper;

	@BeforeEach
	public void setup() {
		Simulation.verbosity = 0;
		this.initSimulation(this.dataSimToy);
		this.mapper = new ObjectMapper();
	}

	@Test
	public void testCar_serialize_deserialize() throws JsonProcessingException, JSONException {
		Car car = this.dataSimToy.getCar(0);
		
		int timestampArrival = car.timestampArrival.toSecondOfDay(); 
		int timestampDeparture = car.timestampDeparture.toSecondOfDay(); 
		
		String carJSON = mapper.writeValueAsString(car);
		
		JSONObject obj = new JSONObject(carJSON);
		assertEquals(timestampArrival, obj.get("timestampArrival")); 
		assertEquals(timestampDeparture, obj.get("timestampDeparture")); 
		
		//System.out.println(carJSON);
		
		Car carClone = mapper.readValue(carJSON, Car.class);
		//System.out.println(Arrays.toString(car.availableTimeslots));
		//System.out.println(Arrays.toString(carClone.availableTimeslots));
		assertEquals(car, carClone); 		
	}

	@Test
	public void testEnergyPriceHistory_serialize_deserialize() throws JsonProcessingException, JSONException {
		EnergyPriceHistory history = this.dataSimToy.getEnergyPriceHistory(); 
		int nTimeslots = history.getNTimeslots(); 
		
		String historyJSON = mapper.writeValueAsString(history);

		JSONObject obj = new JSONObject(historyJSON);
		assertEquals(nTimeslots, obj.getJSONArray("energyPrices").length()); 
		
		//System.out.println(historyJSON);
		
		//System.out.println(carJSON);
		EnergyPriceHistory historyClone = mapper.readValue(historyJSON, EnergyPriceHistory.class);
		//System.out.println(car);
		//System.out.println(carClone);
		assertEquals(history, historyClone); 	
	}
	
	@Test
	public void testChargingStation_serialize_deserialize() throws JsonProcessingException, JSONException {
		ChargingStation station = this.dataSimToy.getChargingStation(0); 
		
		String stationJSON = mapper.writeValueAsString(station);
		
		JSONObject obj = new JSONObject(stationJSON);
		assertEquals(station.fusePhase1, obj.get("fusePhase1")); 
		
		ChargingStation stationClone = mapper.readValue(stationJSON, ChargingStation.class);
		
		assertEquals(station, stationClone); 	
	}
	
	@Test
	public void testFuse_serialize_deserialize() throws JsonProcessingException, JSONException {
		Fuse fuse = this.dataSimToy.getFuseTree().getRootFuse(); 
		
		String fuseJSON = mapper.writeValueAsString(fuse);
		//System.out.println(fuseJSON);
		
		JSONObject obj = new JSONObject(fuseJSON);
		assertEquals(fuse.fusePhase1, obj.get("fusePhase1")); 
		
		Fuse fuseClone = mapper.readValue(fuseJSON, Fuse.class);
		
		assertEquals(fuse, fuseClone); 	
	}
	
	@Test
	public void testFuseTree_serialize_deserialize() throws JsonProcessingException, JSONException {
		FuseTree fuseTree = this.dataSimToy.getFuseTree(); 
		
		String fuseTreeJSON = mapper.writeValueAsString(fuseTree);
		
		FuseTree fuseTreeClone = mapper.readValue(fuseTreeJSON, FuseTree.class);
		assertEquals(fuseTree, fuseTreeClone); 	
	} 
	
}


