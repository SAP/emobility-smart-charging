package com.sap.charging.realTime.exception;

import com.sap.charging.model.Car;

public class CarNotAvailableException extends RuntimeException {

	private static final long serialVersionUID = -1492868871745905270L;

	public CarNotAvailableException(Car car, int timestamp) {
		super("Car n=" + car.getId() + " is not available for state t=" + timestamp + ". Earliest timestamp for car is t=" + car.timestampArrival.toSecondOfDay() + ", timestamp departure is t=" + car.timestampDeparture.toSecondOfDay());
	}
	
}
