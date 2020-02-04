package com.sap.charging.realTime.model.forecasting.soc;

import com.sap.charging.model.Car;

public class CarSoCForecastMedian extends CarSoCForecast {

	@Override
	public double getExpectedSoC(Car carPreviousDay, Car car, int lastDay, int currentDay) {
		// TODO Auto-generated method stub
		return 0.3253;
	}


}
