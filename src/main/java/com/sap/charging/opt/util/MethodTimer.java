package com.sap.charging.opt.util;

public class MethodTimer implements AutoCloseable {
	
	private MethodTimerState state;
	
	private long startTime;
	private long endTime;
	
	public MethodTimer(MethodTimerState state) {
		this.state = state;
		this.startTime = System.nanoTime();
	}
	
	private double getDuration() {
		return (this.endTime - this.startTime) / (1000.0*1000.0*1000.0);
	}
	
	@Override
	public void close() {
		this.endTime = System.nanoTime();
		state.addTime(getDuration());
	}
	
}
