package com.sap.charging.sim.event;

import java.time.LocalTime;

import org.json.simple.JSONObject;

import com.sap.charging.util.JSONKeys;
import com.sap.charging.util.JSONSerializable;
import com.sap.charging.util.TimeUtil;

public abstract class Event implements JSONSerializable {

	public final LocalTime timestamp;
	
	public Event(LocalTime timestamp) {
		this.timestamp = timestamp;
	};
	
	public Event(JSONObject jsonObject) {
		int timestampSeconds = JSONSerializable.getJSONAttributeAsInt(jsonObject.get(JSONKeys.JSON_KEY_TIMESTAMP_SECONDS));
		this.timestamp = TimeUtil.getTimestampFromSeconds(timestampSeconds);	
	}
	
	/**
	 * Returns the difference in ms between two events
	 * @param otherEvent
	 * @return
	 */
	public long getDelta(Event otherEvent) {
		return TimeUtil.getDifferenceInMS(this.timestamp, otherEvent.timestamp);
	}
	
	/**
	 * Extracts the time as seconds of day, from 0 to 24 * 60 * 60 - 1.
	 * @return
	 */
	public int getSecondsOfDay() {
		return (int) this.timestamp.toSecondOfDay();
	}
	
	public static Event fromJSON(JSONObject jsonObject) {
		return null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public final JSONObject toJSONObject() {
		JSONObject result = new JSONObject();
		result.put(JSONKeys.JSON_KEY_TIMESTAMP_SECONDS, timestamp.toSecondOfDay());
		
		addChildJSONAttributes(result);
		
		return result;
	}
	
	public abstract void addChildJSONAttributes(JSONObject object);
	
	
	
}
