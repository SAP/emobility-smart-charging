package com.sap.charging.realTime.model.forecasting;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.DataGeneratorRandomProcesses;
import com.sap.charging.model.Car;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureForecastXGBoost_CarDistribution;
import com.sap.charging.sim.Simulation;

public class CarDepartureForecastXGBoostTest {

	//@Test
	/*public void testRConnection() {
		CarDepartureForecastXGBoost forecast = new CarDepartureForecastXGBoost("gen/models/xgboost.bin");
	}*/
	
	
	//  No XGBoost models are included with this library
	@Disabled
	@Test
	public void testForecast() {
		Simulation.verbosity = 1;
		
		int nCars = 1000;
		DataGeneratorRandomProcesses data = new DataGeneratorRandomProcesses(2, nCars);
		data.generateEnergyPriceHistory(96)
			.generateCars(nCars)
			.generateChargingStations(1)
			.generateFuseTree(1, true);
		
		CarDepartureForecastXGBoost_CarDistribution forecast = new CarDepartureForecastXGBoost_CarDistribution("gen/models/departureForecastXGBoost.bin");
		
		int sum = 0;
		for (Car car : data.getCars()) {
			int resultTimestamp = forecast.getExpectedDepartureTimeSeconds(null, car);
			sum += resultTimestamp;
			System.out.println("Prediction for n=" + car.getId() + "=" + resultTimestamp);
		}
		
		System.out.println("Mean: " + sum / data.getCars().size());
		
	}
	
}
