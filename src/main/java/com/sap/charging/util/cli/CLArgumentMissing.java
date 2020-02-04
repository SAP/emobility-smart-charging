package com.sap.charging.util.cli;

public class CLArgumentMissing extends Exception {
	
	private static final long serialVersionUID = -1124021975788443822L;

	public CLArgumentMissing(String argName) {
		System.out.println("Missing CL argument: " + argName);
	}
	
}
