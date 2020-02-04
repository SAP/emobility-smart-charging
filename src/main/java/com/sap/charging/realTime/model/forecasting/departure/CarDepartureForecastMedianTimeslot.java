package com.sap.charging.realTime.model.forecasting.departure;

import com.sap.charging.model.Car;
import com.sap.charging.realTime.State;
import com.sap.charging.util.TimeUtil;

@Deprecated
public class CarDepartureForecastMedianTimeslot extends CarDepartureForecast {

	/**
	 * Default median is 69 (17:15)
	 */
	private int medianDepartureTimeslot = 69;
	
	/**
	 * If this is used, keep median as 69 (17:15)
	 */
	public CarDepartureForecastMedianTimeslot() {
		
	}
	public CarDepartureForecastMedianTimeslot(int medianDepartureTimeslot) {
		this.medianDepartureTimeslot = medianDepartureTimeslot;
	}
	
	@Override
	public int getExpectedDepartureTimeslot(State state, Car car) {
		return medianDepartureTimeslot;
	}
	
	@Override
	public int getExpectedDepartureTimeSeconds(State state, Car car) {
		return TimeUtil.getTimestampFromTimeslot(medianDepartureTimeslot).toSecondOfDay();
	}

}
