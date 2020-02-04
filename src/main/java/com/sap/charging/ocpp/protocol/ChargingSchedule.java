package com.sap.charging.ocpp.protocol;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChargingSchedule {
	
	/**
	 * Optional. Duration of the charging schedule in seconds. If the
		duration is left empty, the last period will continue indefinitely or
		until end of the transaction in case startSchedule is absent.
	 */
	//private final int duration;
	
	/**
	 * Optional. Starting point of an absolute schedule. If absent the
		schedule will be relative to start of charging.
		
		Here: Used since schedules are always absolute. Will always be midnight of the day
	 */
	public final LocalDateTime startSchedule; 
	
	/**
	 * Required. The unit of measure Limit is expressed in.
	 * 
	 * Here: Always A (amperes) per phase
	 */
	public final ChargingRateUnitType chargingRateUnit = ChargingRateUnitType.A; 
	
	/**
	 * Required. List of ChargingSchedulePeriod elements defining
		maximum power or current usage over time. The startSchedule of
		the first ChargingSchedulePeriod SHALL always be 0.
	 */
	public final ChargingSchedulePeriod[] chargingSchedulePeriod;
	
	/**
	 * Optional. Minimum charging rate supported by the electric
		vehicle. The unit of measure is defined by the chargingRateUnit.
		This parameter is intended to be used by a local smart charging
		algorithm to optimize the power allocation for in the case a
		charging process is inefficient at lower charging rates. Accepts at
		most one digit fraction (e.g. 8.1)
	 */
	//public final double minChargingRate;
	
	
	@JsonCreator
	public ChargingSchedule(
			@JsonProperty("startSchedule") LocalDateTime startSchedule,
			//@JsonProperty("chargingRateUnit") ChargingRateUnitType chargingRateUnit,
			@JsonProperty("chargingSchedulePeriod") ChargingSchedulePeriod[] chargingSchedulePeriod
			//, @JsonProperty("minChargingRate") double minChargingRate
			) {
		this.startSchedule = startSchedule;
		//this.chargingRateUnit = chargingRateUnit;
		this.chargingSchedulePeriod = chargingSchedulePeriod;
		//this.minChargingRate = minChargingRate;
	}
	
}
