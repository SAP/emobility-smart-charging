package com.sap.charging.realTime.model.forecasting.departure;

import com.sap.charging.model.Car;
import com.sap.charging.realTime.State;
import com.sap.charging.util.TimeUtil;

public class CarDepartureForecastLinearModel extends CarDepartureForecast {

	private final double intercept;
	private final double timeOfDayArrivalCoefficient;
	
	public CarDepartureForecastLinearModel(double intercept, double timeOfDayArrivalCoefficient) {
		this.intercept = intercept;
		this.timeOfDayArrivalCoefficient = timeOfDayArrivalCoefficient;
	}
	
	@Override
	public int getExpectedDepartureTimeslot(State state, Car car) {
		double resultTimestamp = intercept + timeOfDayArrivalCoefficient * state.currentTimeSeconds;
		int resultTimeslot = TimeUtil.getTimeslotFromSeconds((int) resultTimestamp);
		return Math.min(resultTimeslot+1, 95);
	}
	
	@Override
	public int getExpectedDepartureTimeSeconds(State state, Car car) {
		double resultTimestamp = intercept + timeOfDayArrivalCoefficient * state.currentTimeSeconds;
		return (int) resultTimestamp;
	}

}
