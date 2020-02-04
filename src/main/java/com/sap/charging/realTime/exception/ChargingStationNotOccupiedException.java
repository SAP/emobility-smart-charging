package com.sap.charging.realTime.exception;

import com.sap.charging.model.ChargingStation;

public class ChargingStationNotOccupiedException extends RuntimeException {

	private static final long serialVersionUID = -7705511373441646605L;

	public ChargingStationNotOccupiedException(ChargingStation chargingStation) {
		super("ChargingStation i=" + chargingStation.getId() + " is not occupied.");
	}
	
}
