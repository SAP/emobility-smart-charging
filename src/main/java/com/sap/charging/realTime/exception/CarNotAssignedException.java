package com.sap.charging.realTime.exception;

import com.sap.charging.model.Car;

public class CarNotAssignedException extends RuntimeException {

	private static final long serialVersionUID = -7255812508189813106L;

	public CarNotAssignedException(Car car) {
		super("Car n=" + car.getId() + " is not assigned.");
	}
	
	
}	

