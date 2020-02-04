package com.sap.charging.model;

import static org.junit.Assert.assertEquals;

import java.time.LocalTime;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import com.sap.charging.model.CarFactory.CarModel;
import com.sap.charging.model.CarFactory.CarType;
import com.sap.charging.util.JSONKeys;
import com.sap.charging.util.TimeUtil;

public class CarTest {

	private static final int nTimeslots = 20;
	private static final double curCapacity = 10;
	private static final double maxCapacity = 40;
	
	@Test
	public void testToJSONWithoutTimestamps() {
		Car car = CarFactory.builder()
				.set(CarModel.NISSAN_LEAF_2016)
				.availableTimeslots(5, 11, nTimeslots)
				.immediateStart(false)
				.suspendable(true)
				.canUseVariablePower(true)
				.carType(CarType.BEV)
				.currentCapacity(curCapacity)
				.build();
		JSONObject json = car.toJSONObject();
		assertEquals("BEV", json.get(JSONKeys.JSON_KEY_CAR_TYPE));
		assertEquals(null, json.get(JSONKeys.JSON_KEY_CAR_TIMESTAMP_ARRIVAL));
		assertEquals(null, json.get(JSONKeys.JSON_KEY_CAR_TIMESTAMP_DEPARTURE));
		assertEquals(5, json.get(JSONKeys.JSON_KEY_CAR_FIRST_AVAILABLE_TIMESLOT));
		assertEquals(11, json.get(JSONKeys.JSON_KEY_CAR_LAST_AVAILABLE_TIMESLOT));
	}
	
	@Test
	public void testToJSONWithTimestamps() {
		LocalTime timestampArrival = TimeUtil.getTimestampFromSeconds(1234);
		LocalTime timestampDeparture = TimeUtil.getTimestampFromSeconds(4321);
		
		Car car = CarFactory.builder()
				.set(CarModel.NISSAN_LEAF_2016)
				.availableTimeslots(1, 5, nTimeslots)
				.availableTimestamps(timestampArrival, timestampDeparture)
				.immediateStart(false)
				.suspendable(true)
				.canUseVariablePower(true)
				.carType(CarType.PHEV)
				.currentCapacity(curCapacity)
				.maxCapacity(maxCapacity)
				.build();
		JSONObject json = car.toJSONObject();
		assertEquals("PHEV", json.get(JSONKeys.JSON_KEY_CAR_TYPE));
		assertEquals(1234, json.get(JSONKeys.JSON_KEY_CAR_TIMESTAMP_ARRIVAL));
		assertEquals(4321, json.get(JSONKeys.JSON_KEY_CAR_TIMESTAMP_DEPARTURE));
		assertEquals(1, json.get(JSONKeys.JSON_KEY_CAR_FIRST_AVAILABLE_TIMESLOT));
		assertEquals(5, json.get(JSONKeys.JSON_KEY_CAR_LAST_AVAILABLE_TIMESLOT));
	}
	
	@Test
	public void testFromJSON() throws ParseException {
		String jsonString = "{" +
				"\"canLoadPhase2\":0.0," +
				"\"maxCapacity\":40.0," +
				"\"canLoadPhase1\":1.0," +
				"\"maxCurrentPerPhase\":4.0," +
				"\"canLoadPhase3\":0.0,"+
				"\"firstAvailableTimeslot\":1,"+
				"\"curCapacity\":10.0,"+
				"\"minLoadingState\":20.0," +
				"\"modelName\":\"Nissan Leaf 2016\","+
				"\"minCurrentPerPhase\":2.5,"+
				"\"maxCurrent\":4.0,"+
				"\"suspendable\":true,"+
				"\"carType\":\"PHEV\","+
				"\"indexN\":2,"+
				"\"lastAvailableTimeslot\":5,"+
				"\"timestampArrival\":1234,"+
				"\"immediateStart\":false,"+
				"\"chargedCapacity\":0.0,"+
				"\"name\":\"n2\","+
				"\"minCurrent\":2.5," +
				"\"currentPlan\": [0,0,0,10.0, 5.0]," +
				"\"timestampDeparture\":4321," +
				"\"canUseVariablePower\":true}";
		JSONObject json = (JSONObject) (new JSONParser()).parse(jsonString);
		Car car = Car.fromJSON(json, 20);
		assertEquals(1, car.getFirstAvailableTimeslot());
		assertEquals(5, car.getLastAvailableTimeslot());
		assertEquals(1234, car.timestampArrival.toSecondOfDay());
		assertEquals(4321, car.timestampDeparture.toSecondOfDay());
	}
	
	@Test
	public void testFromJSONWithoutTimestamps() throws ParseException {
		String jsonString = "{" +
				"\"indexN\":2,"+
				"\"name\":\"n2\","+
				"\"modelName\":\"Nissan Leaf 2016\","+
				"\"carType\":\"PHEV\","+
				"\"canLoadPhase1\":1.0," +
				"\"canLoadPhase2\":0.0," +
				"\"canLoadPhase3\":0.0,"+
				"\"maxCapacity\":40.0," +
				"\"curCapacity\":10.0,"+
				"\"minLoadingState\":20.0,"+
				"\"chargedCapacity\":0.0," +
				"\"maxCurrent\":8.0,"+
				"\"minCurrent\":5.0," +
				"\"firstAvailableTimeslot\":1,"+
				"\"lastAvailableTimeslot\":5,"+
				"\"suspendable\":true,"+
				"\"immediateStart\":false,"+
				"\"canUseVariablePower\":true}";
		JSONObject json = (JSONObject) (new JSONParser()).parse(jsonString);
		Car car = Car.fromJSON(json, 20);
		assertEquals(1, car.getFirstAvailableTimeslot());
		assertEquals(5, car.getLastAvailableTimeslot());
		assertEquals(null, car.timestampArrival);
		assertEquals(null, car.timestampDeparture);
		assertEquals(true, car.isPHEV());
		assertEquals(false, car.isBEV());
	}

}
















