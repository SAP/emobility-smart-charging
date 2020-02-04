package com.sap.charging.ocpp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.charging.ocpp.protocol.ChargingProfile;

public class ChargingProfileAssignment {
	
	public final int chargingStationId;
	public final ChargingProfile chargingProfile;
	
	@JsonCreator
	public ChargingProfileAssignment(
			@JsonProperty("chargingStationId") int chargingStationId,
			@JsonProperty("chargingProfile") ChargingProfile chargingProfile
			) {
		this.chargingStationId = chargingStationId;
		this.chargingProfile = chargingProfile;
	}
	
}	
