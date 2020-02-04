package com.sap.charging.util.cli;

public class CLArgumentInvalidValueType extends Exception {
	
	private static final long serialVersionUID = 5571676315973306837L;

	@SuppressWarnings("rawtypes")
	public CLArgumentInvalidValueType(CLArgument arg, Object value) {
		System.out.println("Invalid value type for " + arg.name + ": Passed in " + value + " but expecting type " + arg.type);
	}
	
}
