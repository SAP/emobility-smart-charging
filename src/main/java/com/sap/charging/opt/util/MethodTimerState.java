package com.sap.charging.opt.util;

import java.util.ArrayList;

public class MethodTimerState {
	
	private double executionTimeOverall;
	private ArrayList<Double> executionTimes; 
	
	public MethodTimerState() {
		this.executionTimes = new ArrayList<>(); 
	}
	
	/**
	 * Returns time in seconds
	 * @return
	 */
	public double getTime() {
		return executionTimeOverall;
	}

	public void addTime(double time) {
		this.executionTimeOverall += time;
		this.executionTimes.add(time); 
	}	
	
	public ArrayList<Double> getExecutionTimes() {
		return executionTimes; 
	}
	
	
}

