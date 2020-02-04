package com.sap.charging.dataGeneration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.carDistributions.CarArrivalDepartureDistribution;
import com.sap.charging.dataGeneration.common.DefaultDataGenerator;
import com.sap.charging.model.Car;
import com.sap.charging.model.CarFactory.CarModel;
import com.sap.charging.sim.Simulation;

public class DataGeneratorRandomTest {
	
	@BeforeEach
	public void setup() {
		Simulation.verbosity = 0; 
	}
	
	@Test
	public void dataGeneration_UniformCarAvailabilityDistribution() {
		DataGeneratorRandom dataGenerator = new DataGeneratorRandom(0, true);
		
		int nTimeslots = 72;
		int nCars = 20;
		int nChargingStations = 10;
		boolean doRotatePhases = true;
		
		dataGenerator.generateEnergyPriceHistory(nTimeslots)
			.generateCars(nCars)
			.generateChargingStations(nChargingStations)
			.generateFuseTree(4, doRotatePhases);
		
		assertEquals(nCars, dataGenerator.getCars().size());
	}
	
	@Test 
	public void testDataGenerationWrongOrder() {
		DataGeneratorRandom dataGenerator = new DataGeneratorRandom(0, true);
		
		int nCars = 20;
		try {
			// generateEnergyPriceHistory should be called first
			dataGenerator.generateCars(nCars);
			fail("generateEnergyPriceHistory should be called first"); 
		}
		catch (Exception e) {
			
		}
	}
	
	@Test 
	public void testDataGenerationInvalidFuseTree() {
		DataGeneratorRandom dataGenerator = new DataGeneratorRandom(0, true);
		
		int nTimeslots = 1;
		int nCars = 1;
		int nChargingStations = 7;
		boolean doRotatePhases = true;
		
		// Generate 1 more charging station then fusetree can support
		dataGenerator.generateEnergyPriceHistory(nTimeslots)
			.generateCars(nCars)
			.generateChargingStations(nChargingStations);
		
		
		try {
			dataGenerator.generateFuseTree(1, doRotatePhases);
			fail("Fuse tree should not support this many fuses in given configuration. An exception should be thrown.");
		}
		catch (RuntimeException e) {
			assertEquals("Number of charging stations is greater than fits in fuse tree: 6<7",
						 e.getMessage());
		}
	}

	@Test
	public void testDataGenerationInvalidTimeslot() {
		
		try {
			CarArrivalDepartureDistribution.getTimeslotsFromDistribution(1.1, 96);
			fail("Should have thrown a null pointer exception");
		}
		catch (NullPointerException e) {
			
		}
	}
	
	@Test
	public void testDataGenerationSeed() {
		int seed1 = 1;
		//int seed2 = 2;
		
		DataGeneratorRandom dataGenerator1 = new DataGeneratorRandom(seed1, false);
		
		Random random1 = new Random();
		random1.setSeed(seed1);
		DataGeneratorRandom dataGenerator2 = new DataGeneratorRandom(random1, false);
		
		assertEquals(dataGenerator1.getRandom().nextInt(), dataGenerator2.getRandom().nextInt());
		
		//dataGenerator1.setSeed(seed2);
		//dataGenerator2.setSeed(seed2);
		//assertEquals(dataGenerator1.getRandom().nextInt(), dataGenerator2.getRandom().nextInt());
		
	}
	
	@Test
	public void testDataGeneratorDefaultEnergyHistory() {
		int seed = 0;
		int nTimeslots = -1;
		DataGenerator dataGenerator = new DataGeneratorRandom(seed, false);
		dataGenerator.generateEnergyPriceHistory(nTimeslots);
		
		int nExpectedTimeslots = DataGeneratorRandom.defaultPrices.length;
		
		assertEquals(nExpectedTimeslots, dataGenerator.getEnergyPriceHistory().getNTimeslots());
	}
	
	@Test
	public void testDataGenerator() {
		int seed = 0;
		DataGenerator dataGenerator = new DataGeneratorRandom(seed, false);
		
		int nTimeslots = 72;
		int nCars = 20;
		int nChargingStations = 10;
		boolean doRotatePhases = true;
		
		dataGenerator.generateEnergyPriceHistory(nTimeslots)
			.generateCars(nCars)
			.generateChargingStations(nChargingStations)
			.generateFuseTree(4, doRotatePhases);
		
		assertEquals(nTimeslots, dataGenerator.getEnergyPriceHistory().getNTimeslots());
		assertEquals(nCars, dataGenerator.getCars().size());
		assertEquals(nChargingStations, dataGenerator.getChargingStations().size());
	}
	
	@Test
	public void testDefaultDataGenerator() {
		DataGenerator data = DefaultDataGenerator.getDefaultDataGenerator();
		assertEquals(DefaultDataGenerator.nTimeslots, data.getEnergyPriceHistory().getNTimeslots());
		assertEquals(DefaultDataGenerator.nCars, data.getCars().size());
		assertEquals(DefaultDataGenerator.nChargingStations, data.getChargingStations().size());
	}
	
	@Test
	public void testSeed() {
		int seed = 0;
		DataGeneratorRandom data1 = new DataGeneratorRandom(seed, false);
		data1.generateEnergyPriceHistory(96).generateCars(100);
		DataGeneratorRandom data2 = new DataGeneratorRandom(seed, false);
		data2.generateEnergyPriceHistory(96).generateCars(100);
		
		for (int n=0;n<data1.getCars().size();n++) {
			Car car1 = data1.getCars().get(n);
			Car car2 = data2.getCars().get(n);
			assertEquals(car1.getCurrentCapacity(), car2.getCurrentCapacity(), 1e-9);
		}
	}
	

	@Test
	public void testSpecificCarModelGeneration() {
		DataGeneratorRandom data = new DataGeneratorRandom(0, false);
		
		CarModel[] carModels = new CarModel[]{CarModel.BMW_I3_2017};
		data.setCarModels(carModels);
		
		data.generateEnergyPriceHistory(96)
			.generateCars(1000);
		
		for (Car car : data.getCars()) {
			assertEquals(car.getModelName(), CarModel.BMW_I3_2017.modelName);
		}
		
	}
	
	
	
	
	
	
}
