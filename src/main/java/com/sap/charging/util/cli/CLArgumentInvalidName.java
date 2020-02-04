package com.sap.charging.util.cli;

public class CLArgumentInvalidName extends Exception {

	private static final long serialVersionUID = -5936679843484192593L;

	public CLArgumentInvalidName(String argName) {
		System.out.println("Argument " + argName + " is an invalid name. See --help for a list of parameters.");
	}
}
