package com.sap.charging.server.api.v1.exception;

public class MissingParameterException extends RuntimeException {

	private static final long serialVersionUID = 6533920595937545649L;
	
	public MissingParameterException(String parameterName) {
		super("Parameter '" + parameterName + "' is required."); 
	}
	
	public MissingParameterException(String p1, String p2) {
		super("Parameter '" + p1 + "' or '" + p2 + "' is required."); 
	}
	
}
