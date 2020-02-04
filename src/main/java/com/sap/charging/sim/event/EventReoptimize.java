package com.sap.charging.sim.event;

import java.time.LocalTime;

import org.json.simple.JSONObject;

public class EventReoptimize extends Event {

	public EventReoptimize(LocalTime timestamp) {
		super(timestamp); 
	}
	
	public EventReoptimize(JSONObject jsonObject) {
		super(jsonObject);
	}

	@Override
	public void addChildJSONAttributes(JSONObject object) {
	}

}
