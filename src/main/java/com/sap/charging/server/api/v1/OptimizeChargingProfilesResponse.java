package com.sap.charging.server.api.v1;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.charging.model.Car;

public class OptimizeChargingProfilesResponse {
	
	public final List<Car> cars; 
	
	@JsonCreator
	public OptimizeChargingProfilesResponse(@JsonProperty("cars") List<Car> cars) {
		this.cars = cars; 
	}
	
}
