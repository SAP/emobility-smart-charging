package com.sap.charging.dataGeneration.carParks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum CarPark {
	
	CUSTOM("CUSTOM", -1, -1, -1, -1, -1, -1, -1, -1),
	
	
	/**
	 * 1x root fuse 4000A
	 * 3x fuse 1250A (Steigschienen)
	 * 6x fuse  800A (Versorgungsschiene)
	 * n charging stations 32A
	 */
	Example1("Example 1", 50, 3, 4000, 
			1250, 3, 
			800, 6,
			10); 
	
	public final String name;
	public final int defaultNumberChargingStations;
	public final int defaultDepth;
	public final int defaultFuseLevel0;
	public final int defaultFuseLevel1;
	public final int defaultNumberFusesLevel1;
	public final int defaultFuseLevel2;
	public final int defaultNumberFusesLevel2;
	public final int defaultNumberChargingStationsLowest;
	
	CarPark(@JsonProperty("name") String name, 
			@JsonProperty("defaultNumberChargingStations") int defaultNumberChargingStations, 
			@JsonProperty("defaultDepth") int defaultDepth, 
			@JsonProperty("defaultFuseLevel0") int defaultFuseLevel0, 
			@JsonProperty("defaultFuseLevel1") int defaultFuseLevel1, 
			@JsonProperty("defaultNumberFusesLevel1") int defaultNumberFusesLevel1,
			@JsonProperty("defaultFuseLevel2") int defaultFuseLevel2, 
			@JsonProperty("defaultNumberFusesLevel2") int defaultNumberFusesLevel2,
			@JsonProperty("defaultNumberChargingStationsLowest") int defaultNumberChargingStationsLowest) {
		this.name = name;
		this.defaultNumberChargingStations = defaultNumberChargingStations;
		this.defaultDepth = defaultDepth;
		this.defaultFuseLevel0 = defaultFuseLevel0;
		this.defaultFuseLevel1 = defaultFuseLevel1;
		this.defaultNumberFusesLevel1 = defaultNumberFusesLevel1;
		this.defaultFuseLevel2 = defaultFuseLevel2;
		this.defaultNumberFusesLevel2 = defaultNumberFusesLevel2;
		this.defaultNumberChargingStationsLowest = defaultNumberChargingStationsLowest;
	}
	
	@JsonCreator
    public static CarPark fromValue(final JsonNode jsonNode) {
		for (CarPark carPark : CarPark.values()) {
			if (carPark.name.equals(jsonNode.get("name").asText())) {
				return carPark;
			}
		}
		return null;
	}
	
	
}
