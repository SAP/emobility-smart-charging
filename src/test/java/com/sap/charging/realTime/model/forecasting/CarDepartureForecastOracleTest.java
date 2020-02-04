package com.sap.charging.realTime.model.forecasting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.model.Car;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureOracle;

public class CarDepartureForecastOracleTest {

	
	@Test
	public void departurePrediction_oracle_shouldExactlyPredict() {
		
		int nCars = 10;
		
		DataGeneratorRandom data = new DataGeneratorRandom(0, false);
		data.generateEnergyPriceHistory(96)
			.generateCars(nCars)
			.generateChargingStations(nCars)
			.generateFuseTree(100, true);
		
		CarDepartureOracle forecast = new CarDepartureOracle();
		
		for (Car car : data.getCars()) {
			int resultTimestamp = forecast.getExpectedDepartureTimeSeconds(null, car);
			assertEquals(resultTimestamp, car.timestampDeparture.toSecondOfDay());
			//System.out.println("Prediction for n=" + car.getId() + "=" + resultTimestamp + ", actual=" + car.timestampDeparture.toSecondOfDay());
		}
		
	}
	
	
}
