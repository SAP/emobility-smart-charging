package com.sap.charging.sim.event;

import java.time.LocalTime;

import org.json.simple.JSONObject;

import com.sap.charging.model.Car;
import com.sap.charging.util.JSONKeys;

public class EventCarDeparture extends Event {

	public final Car car;
	
	public EventCarDeparture(LocalTime timestamp, Car carLeaving) {
		super(timestamp);
		this.car = carLeaving;		
	}

	public EventCarDeparture(JSONObject jsonObject) {
		super(jsonObject);
		this.car = Car.fromJSON((JSONObject) jsonObject.get(JSONKeys.JSON_KEY_CAR), 96);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void addChildJSONAttributes(JSONObject object) {
		object.put(JSONKeys.JSON_KEY_EVENT_TYPE, EventType.CarDeparture.toString());
		object.put(JSONKeys.JSON_KEY_CAR, car.toJSONObject());
	}
	
	
}
