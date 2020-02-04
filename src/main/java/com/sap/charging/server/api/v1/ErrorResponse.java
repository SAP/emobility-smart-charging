package com.sap.charging.server.api.v1;

public class ErrorResponse {

	public final String exceptionName; 
	public final String exceptionMessage; 
	
	public ErrorResponse(Exception e) {
		this.exceptionName = e.getClass().getSimpleName(); 
		this.exceptionMessage = e.getMessage(); 
	}
	
}
