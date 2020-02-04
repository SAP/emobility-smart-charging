package com.sap.charging.realTime.model.forecasting;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.DataGeneratorRandomProcesses;
import com.sap.charging.model.Car;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureForecastXGBoost_CarSample;
import com.sap.charging.sim.Simulation;

public class CarDepartureForecastXGBoost_CarSampleTest {

	//  No XGBoost models are included with this library
	@Disabled
	@Test
	public static void main(String[] args) {
		Simulation.verbosity = 1;
		
		int nCars = 10;
		
		DataGeneratorRandomProcesses data = new DataGeneratorRandomProcesses(2, nCars);
		data.generateEnergyPriceHistory(96)
			.generateCars(nCars)
			.generateChargingStations(1)
			.generateFuseTree(1, true);
		
		CarDepartureForecastXGBoost_CarSample forecast = new CarDepartureForecastXGBoost_CarSample("gen/models/departureForecastXGBoost.bin");
		
		int sum = 0;
		for (Car car : data.getCars()) {
			int resultTimestamp = forecast.getExpectedDepartureTimeSeconds(null, car);
			System.out.println(forecast.getRMatrixString(car));
			sum += resultTimestamp;
			System.out.println("Prediction for n=" + car.getId() + "=" + resultTimestamp + ", actual=" + car.timestampDeparture.toSecondOfDay());
		}
		
		System.out.println("Mean: " + sum / data.getCars().size());
	}
	
	
}
