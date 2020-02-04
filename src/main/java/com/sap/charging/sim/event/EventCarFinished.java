package com.sap.charging.sim.event;

import java.time.LocalTime;

import org.json.simple.JSONObject;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.util.JSONKeys;

public class EventCarFinished extends Event {
	
	public final Car car;
	public final ChargingStation chargingStation;
	
	public EventCarFinished(LocalTime timestamp, Car carFinished, ChargingStation chargingStation) {
		super(timestamp);
		this.car = carFinished;
		this.chargingStation = chargingStation;
	}
	
	public EventCarFinished(JSONObject jsonObject) {
		super(jsonObject);
		this.car = Car.fromJSON((JSONObject) jsonObject.get(JSONKeys.JSON_KEY_CAR), 96);
		this.chargingStation = ChargingStation.fromJSON((JSONObject) jsonObject.get(JSONKeys.JSON_KEY_CHARGING_STATION));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void addChildJSONAttributes(JSONObject object) {
		object.put(JSONKeys.JSON_KEY_EVENT_TYPE, EventType.CarFinished.toString());
		object.put(JSONKeys.JSON_KEY_CAR, car.toJSONObject());
		object.put(JSONKeys.JSON_KEY_CHARGING_STATION, chargingStation.toJSONObject());
	}
	

}
