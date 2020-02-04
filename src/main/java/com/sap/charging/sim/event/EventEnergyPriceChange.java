package com.sap.charging.sim.event;

import java.time.LocalTime;

import org.json.simple.JSONObject;

import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.util.JSONKeys;

public class EventEnergyPriceChange extends Event {

	public final EnergyPriceHistory history;
	
	public EventEnergyPriceChange(LocalTime timestamp, EnergyPriceHistory history) {
		super(timestamp);
		this.history = history;
	}
	
	public EventEnergyPriceChange(JSONObject jsonObject) {
		super(jsonObject);
		this.history = EnergyPriceHistory.fromJSON((JSONObject) jsonObject.get(JSONKeys.JSON_KEY_ENERGY_PRICE_HISTORY));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addChildJSONAttributes(JSONObject object) {
		object.put(JSONKeys.JSON_KEY_EVENT_TYPE, EventType.EnergyPriceChange.toString());
		object.put(JSONKeys.JSON_KEY_ENERGY_PRICE_HISTORY, history.toJSONObject());
	}
	
	
}
