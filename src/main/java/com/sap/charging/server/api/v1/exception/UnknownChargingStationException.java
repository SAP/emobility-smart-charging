package com.sap.charging.server.api.v1.exception;

public class UnknownChargingStationException extends RuntimeException {

	private static final long serialVersionUID = 1327171431732040517L;

	public UnknownChargingStationException(int chargingStationID) {
		super("ChargingStation id=" + chargingStationID + " not found in state!"); 
	}
	
}


