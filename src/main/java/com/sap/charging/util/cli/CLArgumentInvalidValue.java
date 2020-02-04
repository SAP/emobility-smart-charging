package com.sap.charging.util.cli;

public class CLArgumentInvalidValue extends Exception {

	private static final long serialVersionUID = 3421423031197856146L;

	public CLArgumentInvalidValue(String argName, Object value) {
		String message = "Argument " + argName + " has invalid value: " + value;
		System.out.println(message);
	}
	
}
