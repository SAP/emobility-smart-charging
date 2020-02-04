package com.sap.charging.realTime.model.forecasting;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.DataGeneratorRandomProcesses;
import com.sap.charging.model.Car;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureForecastNN_CarSample;
import com.sap.charging.sim.Simulation;

public class CarDepartureForecastNN_CarSampleTest {

	// No NN models are included with this library
	@Disabled
	@Test 
	public static void main(String[] args) {
		Simulation.verbosity = 1;

		int nCars = 10;

		DataGeneratorRandomProcesses data = new DataGeneratorRandomProcesses(2, nCars);
		data.generateEnergyPriceHistory(96).generateCars(nCars).generateChargingStations(1).generateFuseTree(1, true);

		CarDepartureForecastNN_CarSample forecast = new CarDepartureForecastNN_CarSample(
				"gen/models/departureForecastNN_Keras.hdf5");

		int sum = 0;
		for (Car car : data.getCars()) {
			int resultTimestamp = forecast.getExpectedDepartureTimeSeconds(null, car);
			System.out.println(forecast.getRMatrixString(car));
			sum += resultTimestamp;
			System.out.println("Prediction for n=" + car.getId() + "=" + resultTimestamp + ", actual="
					+ car.timestampDeparture.toSecondOfDay());
		}

		System.out.println("Mean: " + sum / data.getCars().size());
	}

}
