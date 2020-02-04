package com.sap.charging.dataGeneration;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.sap.charging.model.Car;



public class DataGeneratorDeterministicTest {

	@Test
	public void test() {
		int nTimeslots = -1;
		int nCars = -1;
		int nChargingStations = 10;
		
		DataGeneratorDeterministic data = new DataGeneratorDeterministic();
		data.generateEnergyPriceHistory(nTimeslots)
			.generateCars(nCars)
			.generateChargingStations(nChargingStations)
			.generateFuseTree(4, true);
		
		assertEquals(DataGeneratorDeterministic.defaultEnergyPriceHistory.length,  
					 data.getEnergyPriceHistory().getNTimeslots());
		
		ArrayList<Car> defaultCars = data.generateDefaultCars();
		
		assertEquals(defaultCars.size(), data.getCars().size());
		assertEquals(nChargingStations, data.getChargingStations().size());
		
	}

}
