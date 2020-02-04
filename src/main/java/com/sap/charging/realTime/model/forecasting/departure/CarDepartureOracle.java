package com.sap.charging.realTime.model.forecasting.departure;

import com.sap.charging.model.Car;
import com.sap.charging.realTime.State;
import com.sap.charging.util.TimeUtil;

public class CarDepartureOracle extends CarDepartureForecast {

	@Override
	public int getExpectedDepartureTimeslot(State state, Car car) {
		return TimeUtil.getTimeslotFromSeconds(car.timestampDeparture.toSecondOfDay()) + 1;
	}

	@Override
	public int getExpectedDepartureTimeSeconds(State state, Car car) {
		return car.timestampDeparture.toSecondOfDay();
	}

}
