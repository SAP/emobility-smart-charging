package com.sap.charging.realTime.model.forecasting.departure;

import com.sap.charging.model.Car;
import com.sap.charging.realTime.State;
import com.sap.charging.util.TimeUtil;

public class CarDepartureForecastMedianTimestamp extends CarDepartureForecast {

	private final int medianTimestamp;
	
	public CarDepartureForecastMedianTimestamp(int medianTimestamp) {
		this.medianTimestamp = medianTimestamp;
	}
	
	@Override
	public int getExpectedDepartureTimeslot(State state, Car car) {
		return TimeUtil.getTimeslotFromSeconds(medianTimestamp);
	}

	@Override
	public int getExpectedDepartureTimeSeconds(State state, Car car) {
		return medianTimestamp;
	}

}
