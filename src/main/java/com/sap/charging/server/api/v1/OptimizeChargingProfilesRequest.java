package com.sap.charging.server.api.v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.charging.server.api.v1.store.EventStore;
import com.sap.charging.server.api.v1.store.OptimizerSettings;
import com.sap.charging.server.api.v1.store.StateStore;

public class OptimizeChargingProfilesRequest {

	public StateStore state; 
	public EventStore event; 
	public Integer verbosity;
	public OptimizerSettings optimizerSettings; 
	
	@JsonCreator
	public OptimizeChargingProfilesRequest(
			@JsonProperty(value="state", required=true) StateStore state,
			@JsonProperty(value="event", required=true) EventStore event,
			@JsonProperty(value="optimizerSettings") OptimizerSettings optimizerSettings,
			@JsonProperty(value="verbosity") Integer verbosity
			) {
		this.state = state; 
		this.event = event; 
		if (verbosity != null) {
			this.verbosity = verbosity;
		} else {
			this.verbosity = 3;
		}
		if (optimizerSettings != null) {
			this.optimizerSettings = optimizerSettings;
		} else {
			this.optimizerSettings = OptimizerSettings.getDefaultOptimizerSettings();
		}
	}
	
}
