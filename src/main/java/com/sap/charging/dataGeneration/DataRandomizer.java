package com.sap.charging.dataGeneration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.sap.charging.model.Car;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.Loggable;
import com.sap.charging.util.random.Distribution;

public class DataRandomizer extends DataGenerator implements Loggable {

	private final DataGenerator dataOriginal;
	
	private final DataGeneratorRandom dataRandom;
	
	/**
	 * Do cars arrive and leave randomly? 
	 */
	private final double randomness;
	/**
	 * Seed for chaos/randomness
	 */
	private final int seedRandomness;
	private final Random random;
	
	public int getVerbosity() {
		return Simulation.verbosity;
	}
	
	/**
	 * Class for randomizing (= changing) original data, e.g. for simulating differences
	 * of day-ahead and real-time simulation. 
	 * 
	 * @param dataOriginal The original data
	 * 
	 * @param randomness Value between 0 and 1, inclusive. 
	 * 0: No changes. 1: No original data remaining. See randomize() for details.
	 * 
	 * @param seedRandomness Seed used for randomizing data
	 */
	public DataRandomizer(DataGenerator dataOriginal, double randomness, int seedRandomness) {
		this.dataOriginal = dataOriginal;
		this.fuseTree = dataOriginal.fuseTree;
		
		this.randomness = randomness;
		this.seedRandomness = seedRandomness;
		this.random = new Random(seedRandomness);
		
		
		
		dataRandom = new DataGeneratorRandom(this.seedRandomness, false);
		dataRandom.setIdealCars(dataOriginal.generateIdealCars);
		dataRandom.setIdealChargingStations(dataOriginal.generateIdealChargingStations);
		
		if (dataOriginal instanceof DataGeneratorRandom) {
			Distribution newDistribution = ((DataGeneratorRandom) dataOriginal).getCurCapacityDistribution().clone(dataRandom.getRandom());
			dataRandom.setCurCapacityDistribution(newDistribution);
		}
		
		dataRandom.generateEnergyPriceHistory(dataOriginal.getEnergyPriceHistory().getNTimeslots());
	}

	
	public DataGenerator generateAll() {
		return this.generateEnergyPriceHistory(-1)
				   .generateChargingStations(-1)
				   .generateCars(-1);
	}
	
	
	/**
	 * Proportional to randomness, cars from original dataset are removed.
	 * Example: 20 cars, randomness=0.25 ==> 5 cars are removed (nCars*randomness).  
	 * Afterwards, between 3 and 7 (center 5), where range is 
	 * [nCarsRemove-nCarsRemove/2, nCarsRemove+nCarsRemove/2],
	 * random new cars are added (center=nCarsRemoved). 
	 * 
	 * @param nCars Ignored
	 * @return
	 */
	@Override
	public DataGenerator generateCars(int nCars) {
		// Get original cars and shuffle (from DB these may be in order of arrival)
		List<Car> originalCars = new ArrayList<>();
		originalCars.addAll(dataOriginal.getCars());
		Collections.shuffle(originalCars, this.random);
		
		int nCarsOriginal = originalCars.size();
		int nCarsToRemove = (int) (nCarsOriginal*randomness);
		int nCarsFromOriginal = nCarsOriginal-nCarsToRemove;
		
		// Add original cars
		this.cars = new ArrayList<>();
		for (int n=0;n<nCarsFromOriginal;n++) {
			this.cars.add(originalCars.get(n));
		}
		
		// Add random cars
		int nCarsRandom = (int) randomizeValue(nCarsToRemove, nCarsToRemove/2);
		dataRandom.setMinCarId(nCarsOriginal);
		dataRandom.generateCars(nCarsRandom);
		this.cars.addAll(dataRandom.getCars());
		
		log(1, "nCarsOriginal=" + nCarsOriginal + ", nCarsToRemove=" + 
				nCarsToRemove + ", nCarsRandom=" + nCarsRandom + ", result.size()=" + 
				this.getCars().size());
		
		// Sort cars by ID
		Collections.sort(this.getCars(), new Comparator<Car>() {
			public int compare(Car o1, Car o2) {
				return o1.getId() - o2.getId();
			}
		});
		
		
		return this;
	}
	
	
	/**
	 * Proportional to randomness, charging stations are removed (never added). 
	 * Example: 20 charging stations, randomness=0.25 ==> between 0 and 5 are removed 
	 * (maxRemoved=nChargingStations*randomness), rounded down
	 * 
	 * @param nChargingStations Ignored
	 * @return
	 */
	@Override
	public DataGenerator generateChargingStations(int nChargingStations) {
		int maxChargingStationsRemoved = (int) Math.round(dataOriginal.getChargingStations().size()*randomness*0.1);
		int nChargingStationsRemoved = (int) Math.round(getNextRandom(0, maxChargingStationsRemoved));
		nChargingStations = dataOriginal.getChargingStations().size()-nChargingStationsRemoved;
		
		this.chargingStations = new ArrayList<>();
		for (int i=0;i<nChargingStations;i++) {
			this.chargingStations.add(dataOriginal.getChargingStations().get(i));
		}
		
		log(1, "nChargingStationsOriginal=" + dataOriginal.getChargingStations().size() + 
				", maxChargingStationsRemoved=" + maxChargingStationsRemoved +
				", nChargingStationsRemoved=" + nChargingStationsRemoved + ", result.size()=" + 
				this.chargingStations.size());	
		return this;
	}


	/**
	 * Proportional to randomness, individual values can go up or down. 
	 * maxChange=40 (constant, decided on average historical energy price)
	 * Example: [0, 5, 15, 10, 5]; randomness=0.25 ==> each value is changed 
	 * by maxAmount=maxChange*randomness=10, e.g. each value is randomized
	 * between [center-maxAmount, 
	 * 
	 * @param lengthK Ignored
	 * @return
	 */
	@Override
	public DataGenerator generateEnergyPriceHistory(int lengthK) {
		double maxChange = 40;
		double maxAmount = maxChange * randomness;
		
		EnergyPriceHistory originalHistory = dataOriginal.getEnergyPriceHistory();
		double[] randomizedPrices = new double[originalHistory.getNTimeslots()];
		
		for (int k=0;k<originalHistory.getNTimeslots();k++) {
			double randomizedValue = randomizeValue(originalHistory.getPrice(k), maxAmount);
			// Get a number with 2 decimal places
			BigDecimal bd = new BigDecimal(randomizedValue).setScale(2, RoundingMode.HALF_EVEN);
			randomizedPrices[k] = bd.doubleValue();
		}
		this.energyPriceHistory = new EnergyPriceHistory(randomizedPrices, originalHistory.getDate());
		return this;		
	}
	
	/**
	 * @param center
	 * @param maxAmount
	 * @return Returns a random value between [center-maxAmount, center+maxAmount]
	 */
	private double randomizeValue(double center, double maxAmount) {
		return getNextRandom(center-maxAmount, center+maxAmount);
	}
	
	private double getNextRandom(double min, double max) {
		return getNextRandom() * (max-min) + min;
	}

	private double getNextRandom() {
		return this.random.nextDouble();
	}

	
	@Override
	public DataGenerator clone() {
		throw new RuntimeException("ERROR: Not implemented yet");
	}
	
	
}
