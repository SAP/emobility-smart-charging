package com.sap.charging.dataGeneration.common;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorRandom;

public class DefaultDataGenerator {
	
	public static final int seed = 0;
	public static final int nTimeslots = 96;
	public static final int nCars = 20;
	public static final int nChargingStations = 10;
	public static final int nBottomLevelChargingStations = 4;
	public static final boolean doRotatePhases = true;
	
	public static final int nTimeslotsToy = 10;
	public static final int nCarsToy = 2;
	public static final int nChargingStationsToy = 2;
	
	
	public static DataGenerator getDefaultDataGenerator() {
		
		DataGenerator dataGenerator = new DataGeneratorRandom(seed, false);
		
		dataGenerator.generateEnergyPriceHistory(nTimeslots)
			.generateCars(nCars)
			.generateChargingStations(nChargingStations)
			.generateFuseTree(nBottomLevelChargingStations, doRotatePhases);
		return dataGenerator;
	}
	
	
	public static DataGenerator getToyDataGenerator() {
		DataGenerator dataGenerator = new DataGeneratorRandom(seed, false);
		dataGenerator.setIdealCars(true);
		dataGenerator.setIdealChargingStations(true);
		
		dataGenerator.generateEnergyPriceHistory(nTimeslotsToy)
			.generateCars(nCarsToy)
			.generateChargingStations(nChargingStationsToy)
			.generateFuseTree(nBottomLevelChargingStations, doRotatePhases);
		return dataGenerator;
	}
	
	public static DataGenerator getDataGenerator(int nTimeslots, 
			int nCars, int nChargingStations, boolean idealCars, boolean idealChargingStations) {
		DataGenerator dataGenerator = new DataGeneratorRandom(seed, false);
		dataGenerator.setIdealCars(idealCars);
		dataGenerator.setIdealChargingStations(true);
		
		dataGenerator.generateEnergyPriceHistory(nTimeslots)
			.generateCars(nCars)
			.generateChargingStations(nChargingStations)
			.generateFuseTree(nBottomLevelChargingStations, doRotatePhases);
		return dataGenerator;
	}
}
