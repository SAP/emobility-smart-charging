package com.sap.charging.realTime.exception;

import com.sap.charging.model.Car;

public class CarAlreadyAssignedPowerException extends RuntimeException  {

	private static final long serialVersionUID = -8896866028319010991L;
	
	public CarAlreadyAssignedPowerException(Car car) {
		super("Car n=" + car.getId() + " already has a power assignment.");
	}
	
}
