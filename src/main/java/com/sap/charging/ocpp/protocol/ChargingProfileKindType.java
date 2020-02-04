package com.sap.charging.ocpp.protocol;

public enum ChargingProfileKindType {
	
	/**
	 * Schedule periods are relative to a fixed point in time defined in the schedule.
	 * 
	 * here always used.
	 */
	Absolute,
	
	/**
	 * The schedule restarts periodically at the first schedule period.
	 */
	Recurring,
	
	/**
	 * Schedule periods are relative to a situation-specific start point (such as the start of a Transaction) that is determined by the
		charge point.
	 */
	Relative
	
	
	
}
