package com.sap.charging.realTime.model.forecasting.departure;

import com.sap.charging.model.Car;
import com.sap.charging.model.CarProcessData;
import com.sap.charging.realTime.State;
import com.sap.charging.realTime.model.forecasting.Forecast;

public abstract class CarDepartureForecast extends Forecast {
	
	
	public abstract int getExpectedDepartureTimeslot(State state, Car car);

	public abstract int getExpectedDepartureTimeSeconds(State state, Car car);
	
	public static CarDepartureForecast getDefaultCarDepartureForecast() {
		return new CarDepartureForecastMedianTimestamp(17*3600 + 15*60); 
	}
	
	public String getRMatrixString(Car car) {
		CarProcessData data = car.getCarProcessData();
		String matrixString = "as.matrix(data.table::" + data.oneHotEncodedRaw + ")";
		return matrixString;
	}
	
	
	
}
