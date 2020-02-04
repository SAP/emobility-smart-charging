package com.sap.charging.server.api.v1.exception;

public class UnknownEventTypeException extends IllegalArgumentException {

	private static final long serialVersionUID = 2853451587582770814L;

	public UnknownEventTypeException(String string) {
		super(string); 
	}

	
}
