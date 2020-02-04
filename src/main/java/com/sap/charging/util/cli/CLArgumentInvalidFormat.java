package com.sap.charging.util.cli;

public class CLArgumentInvalidFormat extends Exception {

	private static final long serialVersionUID = 1798849042249121220L;
	
	public CLArgumentInvalidFormat(String rawArg) {
		System.out.println("Invalid argument format for arg=" + rawArg + ": excepting arg1=value1 arg2=value2");
	}
	
}
