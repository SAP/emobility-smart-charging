package com.sap.charging.ocpp;

import org.json.simple.JSONObject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.charging.util.JSONSerializable;

public class OCPPData implements JSONSerializable {
	
	
	public final String ocppVersion = "1.6";
	public final ChargingProfileAssignment[] chargingProfileAssignments;
	
	@JsonCreator
	public OCPPData(
			@JsonProperty("chargingProfileAssignments") ChargingProfileAssignment[] chargingProfileAssignments
			) {
		this.chargingProfileAssignments = chargingProfileAssignments;
	}
	
	
	@Override
	public JSONObject toJSONObject() {
		return JSONConverter.toJSONObject(this);
	}
	
}
