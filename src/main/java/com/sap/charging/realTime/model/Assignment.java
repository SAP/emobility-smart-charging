package com.sap.charging.realTime.model;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.util.JSONSerializable;

public abstract class Assignment implements JSONSerializable {
	
	public final Car car;
	public final ChargingStation chargingStation;
	
	
	public Assignment(Car car, ChargingStation chargingStation) {
		this.car = car;
		this.chargingStation = chargingStation;
	}
	
	
	public abstract double[] getCurrentPerGridPhase(int timeslot);
	
	
}
