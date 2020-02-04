package com.sap.charging.dataGeneration;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.common.DefaultDataGenerator;
import com.sap.charging.model.Car;
import com.sap.charging.sim.Simulation;


public class DataRandomizerTest {

	DataGenerator dataOriginal; 
	
	@BeforeEach
	public void setup() {
		Simulation.verbosity = 0; 
		dataOriginal = DefaultDataGenerator.getDefaultDataGenerator(); 
	}
	
	
	@Test
	public void testSeed() {
		int seed = 0;
		
		//DataGeneratorReal dataOriginal = new DataGeneratorReal("2016-12-01", seed, true, true);
		//dataOriginal.setIdealCars(true);
		//dataOriginal.setIdealChargingStations(true);
		//dataOriginal.generateAll();
		
		DataGenerator dataClone1 = dataOriginal.clone();
		DataGenerator dataClone2 = dataOriginal.clone();
		for (int n=0;n<dataOriginal.getCars().size();n++)  {
			Car car1 = dataOriginal.getCars().get(n);
			Car car2 = dataClone1.getCars().get(n);
			Car car3 = dataClone2.getCars().get(n);
			assertEquals(car1.getCurrentCapacity(), car2.getCurrentCapacity(), 1e-9);
			assertEquals(car1.getCurrentCapacity(), car3.getCurrentCapacity(), 1e-9);
		}
		
		
		DataRandomizer data1 = new DataRandomizer(dataClone1, 0.25, seed);
		data1.generateAll();
		
		DataRandomizer data2 = new DataRandomizer(dataClone2, 0.25, seed);
		data2.generateAll();
		
		for (int n=0;n<data1.getCars().size();n++) {
			Car car1 = data1.getCars().get(n);
			Car car2 = data2.getCars().get(n);
			assertEquals(car1.getCurrentCapacity(), car2.getCurrentCapacity(), 1e-9);
		}
		
		
	}
	
	
}






