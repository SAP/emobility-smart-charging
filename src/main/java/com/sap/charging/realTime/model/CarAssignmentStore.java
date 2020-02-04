package com.sap.charging.realTime.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CarAssignmentStore {

	public final int carID; 
	public final int chargingStationID; 
	
	@JsonCreator
	public CarAssignmentStore(
			@JsonProperty(value="carID", required=true) int carID,
			@JsonProperty(value="chargingStationID", required=true) int chargingStationID) {
		this.carID = carID;
		this.chargingStationID = chargingStationID; 
	}
	
}
