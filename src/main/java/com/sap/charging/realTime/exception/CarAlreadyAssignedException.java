package com.sap.charging.realTime.exception;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;

public class CarAlreadyAssignedException extends RuntimeException {

	private static final long serialVersionUID = -6489094963833396164L;
	
	public CarAlreadyAssignedException(Car car) {
		super("Car n=" + car.getId() + " is already assigned to a charging station");
	}
	
	public CarAlreadyAssignedException(Car car, ChargingStation chargingStation) {
		super("Car n=" + car.getId() + " is already assigned to a charging station and "
				+ "can't be assigned to chargingStation i=" + chargingStation.getId());
	}
	
}
