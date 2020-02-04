package com.sap.charging.sim.event;

import java.time.LocalTime;

import org.json.simple.JSONObject;

import com.sap.charging.model.Car;
import com.sap.charging.util.JSONKeys;

public class EventCarArrival extends Event {

	public final Car car;
	
	public EventCarArrival(LocalTime timestamp, Car newCar) {
		super(timestamp);
		this.car = newCar;
	}
	
	public EventCarArrival(JSONObject jsonObject) {
		super(jsonObject);
		this.car = Car.fromJSON((JSONObject) jsonObject.get(JSONKeys.JSON_KEY_CAR), 96);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addChildJSONAttributes(JSONObject object) {
		object.put(JSONKeys.JSON_KEY_EVENT_TYPE, EventType.CarArrival.toString());
		object.put(JSONKeys.JSON_KEY_CAR, car.toJSONObject());
	}
	
	
}
