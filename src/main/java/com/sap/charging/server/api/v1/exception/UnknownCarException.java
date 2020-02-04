package com.sap.charging.server.api.v1.exception;

public class UnknownCarException extends RuntimeException {
	
	private static final long serialVersionUID = 7348969993780779108L;

	public UnknownCarException(int carID) {
		super("Car id=" + carID + " not found in state!"); 
	}
	
}
