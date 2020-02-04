package com.sap.charging.ocpp.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChargingSchedulePeriod {
	
	/**
	 * Required. Start of the period, in seconds from the start of schedule. The value of
		StartPeriod also defines the stop time of the previous period.
		
		Here: Always 15*60=900, since each period (=timeslot) is 15 minutes
	 */
	public final int startPeriod;
	
	/**
	 * Required. Charging rate limit during the schedule period, in the applicable
		chargingRateUnit, for example in Amperes or Watts. Accepts at most one digit
		fraction (e.g. 8.1).
		
		Here: Always in Amperes (per phase)
	 */
	public final double limit;
	
	/**
	 * Optional. The number of phases that can be used for charging. If a number of
		phases is needed, numberPhases=3 will be assumed unless another number is
		given.
		
		Here: car.sumUsedPhases
	 */
	public final int numberPhases;
	
	@JsonCreator
	public ChargingSchedulePeriod(
			@JsonProperty("startPeriod") int startPeriod,
			@JsonProperty("limit") double limit,
			@JsonProperty("numberPhases") int numberPhases
			) {
		this.startPeriod = startPeriod;
		this.limit = limit;
		this.numberPhases = numberPhases;
	}
	
	
	
	
}
