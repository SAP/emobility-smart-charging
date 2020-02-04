package com.sap.charging.model;

public class FuseTreeChargingStationCounter {

	private final int limitNumberChargingStations;
	private int currentNumberChargingStations = 0;
	
	/**
	 * 
	 * @param max Pass 0 for no limit
	 */
	public FuseTreeChargingStationCounter(int limitNumberChargingStations) {
		this.limitNumberChargingStations = limitNumberChargingStations;
	}
	public void addChargingStation() {
		currentNumberChargingStations++;
	}
	
	public int getCurrentNumberChargingStation() {
		return currentNumberChargingStations;
	}
	
	public boolean isLimitReached() {
		if (limitNumberChargingStations <= 0) {
			return false;
		}
		return currentNumberChargingStations >= limitNumberChargingStations;
	}
	
}
