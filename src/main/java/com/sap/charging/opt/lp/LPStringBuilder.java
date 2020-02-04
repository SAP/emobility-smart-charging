package com.sap.charging.opt.lp;

public class LPStringBuilder {
	
	private StringBuilder currentString;
	
	public LPStringBuilder() {
		currentString = new StringBuilder();
	}
	public static LPStringBuilder builder() {
		return new LPStringBuilder();
	}
	
	public LPStringBuilder appendString(String input) {
		currentString.append(input);
		return this;
	}
	public LPStringBuilder appendDouble(double input) {
		currentString.append(input);
		return this;
	}
	
	public LPStringBuilder appendSpaces(int nSpaces) {
		for (int i=0;i<nSpaces;i++) 
			currentString.append(" ");
		return this;
	}
	
	public LPStringBuilder appendLineEnding() {
		currentString.append("\r\n");
		return this;
	}
	
	public String build() {
		return currentString.toString();
	}
	
}