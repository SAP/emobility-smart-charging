package com.sap.charging.sim.eval.exception;

public abstract class ValidationException extends Exception {

	private static final long serialVersionUID = -5013179691348196812L;
	
	public ValidationException() {
		super();
	}
	
	public ValidationException(String message) {
		super(message);
	}
	
	
}
