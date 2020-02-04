package com.sap.charging.sim.event;

import org.json.simple.JSONObject;

import com.sap.charging.util.JSONKeys;

public enum EventType {

	CarArrival,
	CarDeparture,
	CarFinished,
	EnergyPriceChange,
	Reoptimize;
	
	public static Event fromJSON(JSONObject jsonObject) {
		
		if (jsonObject.containsKey(JSONKeys.JSON_KEY_EVENT))
			jsonObject = (JSONObject) jsonObject.get(JSONKeys.JSON_KEY_EVENT);
		
		String eventTypeString = (String) jsonObject.get(JSONKeys.JSON_KEY_EVENT_TYPE);
		EventType eventType = EventType.valueOf(eventTypeString);
		switch(eventType) {
		case CarArrival:
			return new EventCarArrival(jsonObject);
		case CarDeparture:
			return new EventCarDeparture(jsonObject);
		case CarFinished:
			return new EventCarFinished(jsonObject);
		case EnergyPriceChange:
			return new EventEnergyPriceChange(jsonObject);
		case Reoptimize:
			return new EventReoptimize(jsonObject); 
		default:
			return null;
		}
	}
	
}
