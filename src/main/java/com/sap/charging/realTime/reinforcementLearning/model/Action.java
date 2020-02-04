package com.sap.charging.realTime.reinforcementLearning.model;

import java.text.DecimalFormat;

public class Action {
	
	static DecimalFormat format = new DecimalFormat("#00.00"); 
	
	public static final double MAX_POWER = 32;
	
	public final double phase1;
	public final double phase2;
	public final double phase3; 
	
	private boolean isValid = true;
	
	/**
	 * Action is P for a timestamp, i.e. has 3 doubles, one per phase
	 */
	public Action(double phase1, double phase2, double phase3) {
		this.phase1 = phase1;
		this.phase2 = phase2;
		this.phase3 = phase3;
	}
	
	public void setInvalid() {
		this.isValid = false;
	}
	
	public boolean isValid() {
		return isValid;
	}
	
	
	@Override
	public String toString() {
		return "Action (p1=" + format.format(phase1) + 
					  ";p2=" + format.format(phase2) + 
					  ";p3=" + format.format(phase3) + ")";
	}
	
}
