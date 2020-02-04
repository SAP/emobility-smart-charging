package com.sap.charging.sim.common;

import com.sap.charging.model.Car;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.realTime.State;
import com.sap.charging.realTime.Strategy;

public class StrategyMock extends Strategy {

	public int counterCarArrival = 0;
	public int counterCarFinished = 0;
	public int counterCarDeparture = 0;
	public int counterEnergyPriceChange = 0;
	
	@Override
	public void reactCarArrival(State currentState, Car newCar) {
		counterCarArrival++;
	}
	
	@Override
	public void reactCarFinished(State currentState, Car carFinished) {
		counterCarFinished++;
	}
	
	@Override
	public void reactCarDeparture(State currentState, Car carLeaving) {
		counterCarDeparture++;
	}

	@Override
	public void reactEnergyPriceChange(State currentState, EnergyPriceHistory newEnergyPriceHistory) {
		counterEnergyPriceChange++;
	}

	@Override
	public String getMethod() {
		return "realTimeMock";
	}

}
