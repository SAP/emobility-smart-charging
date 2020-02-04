package com.sap.charging.realTime.exception;

import com.sap.charging.model.ChargingStation;

public class ChargingStationAlreadyOccupiedException extends RuntimeException {

	private static final long serialVersionUID = 4856888426498557201L;
	
	public ChargingStationAlreadyOccupiedException(ChargingStation chargingStation) {
		super("ChargingStation i=" + chargingStation.getId() + " is already occupied.");
	}
	
}
