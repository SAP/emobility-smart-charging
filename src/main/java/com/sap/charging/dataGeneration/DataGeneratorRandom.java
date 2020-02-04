package com.sap.charging.dataGeneration;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Random;

import com.sap.charging.dataGeneration.carDistributions.CarArrivalDepartureDistribution;
import com.sap.charging.dataGeneration.carDistributions.CarArrivalDepartureDistribution.Tuple;
import com.sap.charging.dataGeneration.carParks.CarPark;
import com.sap.charging.model.Car;
import com.sap.charging.model.CarFactory;
import com.sap.charging.model.CarProcessData;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.ChargingStationFactory;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.CarFactory.CarModel;
import com.sap.charging.model.battery.BatteryData;
import com.sap.charging.model.battery.BatteryData_Sample;
import com.sap.charging.util.random.Distribution;
import com.sap.charging.util.random.UniformDistribution;

public class DataGeneratorRandom extends DataGenerator {
	
	
	private int seed;
	private final Random random;
	private final boolean timesUniformDistribution;

	protected CarModel[] carModels;
	private double minSoC = -1;
	
	private Distribution curCapacityDistribution;
	
	private boolean nonlinearCharging = false; // Should batteries use nonlinear model (CCCV/CPCV)?
	private BatteryData batteryData = new BatteryData_Sample();
	
	/**
	 * 
	 * @param seed Start seed (e.g. 0) to get reproducible results
	 * @param timesUniformDistribution Signify whether car arrival and departure times 
	 * 								   should be uniformly distributed or according to https://github.wdf.sap.corp/d053838/ECharger/wiki/Test-Data-generation
	 */
	public DataGeneratorRandom(int seed, boolean timesUniformDistribution) {
		this.seed = seed;
		this.random = new Random();
		this.random.setSeed(seed);
		this.curCapacityDistribution = new UniformDistribution(this.random, 0, 1);
		this.timesUniformDistribution = timesUniformDistribution;
		this.carModels = CarModel.values();
	}
	
	/**
	 * 
	 * @param random Allows to set a own random number generator.
	 * @param timesUniformDistribution Signify whether car arrival and departure times 
	 * 								   should be uniformly distributed or according to https://github.wdf.sap.corp/d053838/ECharger/wiki/Test-Data-generation
	 */
	public DataGeneratorRandom(Random random, boolean timesUniformDistribution) {
		this.random = random;
		this.curCapacityDistribution = new UniformDistribution(this.random, 0, 1);
		this.timesUniformDistribution = timesUniformDistribution;
		this.carModels = CarModel.values();
	}
	
	/**
	 * If distribution gives results < 0, soc is set to 0. If > 1, soc is set to 1.
	 * @param distribution
	 */
	public void setCurCapacityDistribution(Distribution distribution) {
		this.curCapacityDistribution = distribution;
	}
	
	public Distribution getCurCapacityDistribution() {
		return this.curCapacityDistribution;
	}
	
	public void setNonlinearCharging(boolean nonlinearCharging) {
		this.nonlinearCharging = nonlinearCharging;
	}
	
	public boolean getNonlinearCharging() {
		return this.nonlinearCharging;
	}
	
	public void setBatteryData(BatteryData batteryData) {
		this.batteryData = batteryData;
	}

	public void setMinSoC(double minSoC)
	{
		this.minSoC = minSoC;
	}

	public Random getRandom() {
		return this.random;
	}
	
	
	
	/**
	 * Default: Simulate all car models (all models are available)
	 * @param carModels
	 */
	public void setCarModels(CarModel[] carModels) {
		this.carModels = carModels;
	}
	
	public CarModel[] getCarModels() {
		return this.carModels;
	}
	
	/**
	 * Returns a new instance of DataGenerator using the same parameters as for this instance. 
	 * The original instance is NOT changed. Any temporary variables (such as car.chargedCapacity) are reset. 
	 * @return
	 */
	public DataGeneratorRandom clone() {
		DataGeneratorRandom clone = new DataGeneratorRandom(seed, timesUniformDistribution);
		clone.setIdealCars(this.generateIdealCars);
		clone.setIdealChargingStations(this.generateIdealChargingStations);
		
		Distribution newDistribution = this.curCapacityDistribution.clone(clone.getRandom());
		clone.setCurCapacityDistribution(newDistribution);
		clone.setCarModels(this.getCarModels());
		clone.setCarPark(this.carPark);
		clone.setNonlinearCharging(this.nonlinearCharging);
		
		clone.generateEnergyPriceHistory(this.getEnergyPriceHistory().getNTimeslots())
			 .generateCars(this.getCars().size());
		
		if (this.carPark == CarPark.CUSTOM) {
			clone.generateChargingStations(this.getChargingStations().size())
		 	  	 .generateFuseTree(this.getFuseTree().getNumberChargingStationsBottomLevel(), this.doRotateFuseTreePhases);
		}
		else {
			clone.generateChargingStations()
				 .generateFuseTree();
		}
		
		return clone;
	}
	
	/**
	 * Always deterministic, since uses the same model.
	 * @param nChargingStations
	 * @return
	 */
	public DataGenerator generateChargingStations(int nChargingStations) {
		chargingStations = new ArrayList<>();
		for (int i=minChargingStationId;i<nChargingStations+minChargingStationId;i++) {
			ChargingStation chargingStation = ChargingStationFactory.builder()
					.buildFromStandard(ChargingStationFactory.Standard.KeContact_P30)
					.setIndexI(i)
					.build();
			chargingStations.add(chargingStation);
		}
		return this;
	}
	
	
	public int getTimeslotFromDistribution(double[] distribution, double random, int nTimeslots) {
		// Iterate over distributions per timeslot, summing up progressively
		// until we "find" the correct slot
		double distSoFar = 0;
		
		// Calculate ratio in case nTimeslots does not match all availabletimeslots
		double ratio = nTimeslots*1.0 / distribution.length;
		
		for (int i=0;i<distribution.length;i++) {
			distSoFar += distribution[i];
			if (random <= distSoFar) 
				return (int) Math.floor(i*ratio);
		}
		return -1;
	}
	
	protected CarModel[] getCarModelSubset(int n) {
		// In this class, just return all previously set carModels
		return getCarModels();
	}
	
	private final CarModel generateCarModel(CarModel[] carModelSubset) {
		//System.out.println("DataGeneratorRandom::generateCars Using the following models: " + Arrays.toString(getCarModels()));
		return carModelSubset[random.nextInt(carModelSubset.length)];
	}
	
	protected Tuple generateNonUniformArrivalDepartureTimeslots(int n) {
		// In this class, return a random sample of the 2D distribution
		double rArrivalDeparture = random.nextDouble();
		return CarArrivalDepartureDistribution.getTimeslotsFromDistribution(rArrivalDeparture, this.getEnergyPriceHistory().getNTimeslots());
	}
	
	protected LocalTime[] generateArrivalDepartureTimestamp(Tuple tuple, int n) {
		return getRandomTimestamps(tuple, random.nextDouble(), random.nextDouble());
	}
	
	protected CarProcessData getCarProcessData(int n) {
		// No-op
		return null;
	}
	
	
	/**
	 * 
	 * @param nCars
	 * @return
	 */
	@Override
	public DataGenerator generateCars(int nCars) {
		cars = new ArrayList<>();
		if (this.getEnergyPriceHistory() == null) {
			throw new RuntimeException("DataGeneratorRandom::generateCars ERROR: Please call generateEnergyPriceHistory first."); 
		}
		int nTimeslots = this.getEnergyPriceHistory().getNTimeslots();
		
		
		for (int n=minCarId;n<nCars+minCarId;n++) {
		  
			// Choose car model
			CarModel standard = generateCarModel(this.getCarModelSubset(n));
			CarFactory builder = CarFactory.builder()
					.set(standard);
			
			// Generate car availabilities
			int arrivalTimeslot; 
			int departureTimeslot; 
			Tuple tuple;
			if (timesUniformDistribution == true) {
				arrivalTimeslot = random.nextInt(nTimeslots);
				departureTimeslot = random.nextInt(nTimeslots);
				tuple = new Tuple(arrivalTimeslot, departureTimeslot);
			}
			else {
				tuple = generateNonUniformArrivalDepartureTimeslots(n);
				arrivalTimeslot = tuple.arrivalTimeslot;
				departureTimeslot = tuple.departureTimeslot;
			}
			// Generate continuous arrival and departure timestamps based on timeslots
			LocalTime[] arrivalDepartureTimestamps = generateArrivalDepartureTimestamp(tuple, n);
			LocalTime arrivalTimestamp = arrivalDepartureTimestamps[0];
			LocalTime departureTimestamp = arrivalDepartureTimestamps[1];
			
			// Car capacity
			double currentCapacity = generateCurCapacity(builder.getMaxCapacity()); // //random.nextInt((int) maxCapacity);
			
			// Leave at least one slot of charging free 
			// ==> Car never arrives with full charge
			if (standard.minCurrent > builder.getMaxCapacity()-currentCapacity) {
				currentCapacity -= standard.minCurrent;
			}
			
			
			// Finish by building car object
			builder.id(n)
					.currentCapacity(currentCapacity)
					.availableTimeslots(arrivalTimeslot, departureTimeslot, nTimeslots)
					.availableTimestamps(arrivalTimestamp, departureTimestamp)
					.nonlinearCharging(this.getNonlinearCharging())
					.batteryData(batteryData);
			if (this.minSoC >= 0) {
				builder.minLoadingState(builder.getMaxCapacity() * this.minSoC);
			}
			Car car = builder.build();
			
			car.setIdealCar(generateIdealCars);
			car.setCarProcessData(getCarProcessData(n));
			cars.add(car);
		}
		return this;
	}
	
	private double generateCurCapacity(double maxCapacity) {
		// Distribution gives soc, meaning the fraction of capacity filled
		double socFraction = curCapacityDistribution.getNextDouble();
		if (socFraction < 0) // Can happen in normal distribution
			socFraction = 0;
		
		if (socFraction > 1) // Can happen in normal distribution
			socFraction = 1;
		return socFraction * maxCapacity;
	}
	
	/**
	 * Example: timeslot arrival k=0 (05:00)
	 * Timestamp generation: random in 15 mins slot (05:00:00-05:14:59)
	 * @param timeslot
	 * @param random From 0 (inclusive) to 1 (exlusive)
	 * @return
	 */
	public static LocalTime getRandomTimestamp(int timeslot, double random) {
		int secondsDiff = (int)(random * (15*60));
		return getTimestamp(timeslot).plusSeconds(secondsDiff);		
	}
	
	public static LocalTime[] getRandomTimestamps(Tuple tuple, double randomArrival, double randomDeparture) {
		//LocalTime arrivalTimestamp = getRandomTimestamp((tuple.arrivalTimeslot > 0 ) ? tuple.arrivalTimeslot-1 // If arriving after at least at 00:15:00, subtract seconds instead of taking 
		//																			 : tuple.arrivalTimeslot, 
		//												randomArrival);
		LocalTime arrivalTimestamp = getRandomTimestamp(tuple.arrivalTimeslot, randomArrival);
		LocalTime departureTimestamp = getRandomTimestamp(tuple.departureTimeslot, randomDeparture);
		if (arrivalTimestamp.isAfter(departureTimestamp)) {
			// Can happen if arrivalTimeslot = departureTimeslot, switch in this case
			LocalTime temp = arrivalTimestamp;
			arrivalTimestamp = departureTimestamp;
			departureTimestamp = temp;
		}
		return new LocalTime[] {arrivalTimestamp, departureTimestamp};
	}
	
	
	
	/**
	 * Example: data from 18.10.2017 intraday germany, weighted average used
	 * https://www.epexspot.com/en/market-data/intradaycontinuous/intraday-table/-/DE
	 */
	public static final double[] defaultPrices = new double[]{
			27.27, 37.53, 35.55, 28.63, //  0:00- 1:00 
			39.26, 38.13, 35.71, 34.25, //  1:00- 2:00
			37.65, 34.57, 36.41, 39.53, //  2:00- 3:00
			31.54, 31.55, 31.41, 29.72, //  3:00- 4:00
			26.27, 32.84, 35.39, 38.53, //  4:00- 5:00
			31.48, 35.18, 40.22, 44.39, //  5:00- 6:00  0 -  3
			40.33, 38.45, 50.61, 63.06, //  6:00- 7:00  4 -  7
			53.66, 53.91, 63.53, 72.64, //  7:00- 8:00  8 - 11
			67.55, 64.33, 60.96, 53.19, //  8:00- 9:00 12 - 15
			70.43, 59.24, 64.52, 54.94, //  9:00-10:00 16 - 19 
			65.26, 57.29, 48.90, 41.52, // 10:00-11:00 20 - 23
			57.33, 58.90, 51.74, 48.39, // 11:00-12:00 24 - 27
			57.90, 47.67, 48.32, 45.24, // 12:00-13:00 28 - 31
			54.22, 48.42, 45.62, 46.88, // 13:00-14:00 32 - 35
			43.31, 41.76, 38.00, 48.65, // 14:00-15:00 36 - 39
			40.17, 47.82, 51.83, 53.56, // 15:00-16:00 40 - 43
			51.22, 59.17, 59.66, 64.19, // 16:00-17:00 44 - 47
			54.11, 57.56, 66.56, 82.67, // 17:00-18:00 48 - 51
			70.38, 91.69,100.25,133.85, // 18:00-19:00 52 - 55
		   123.98,102.26, 79.37, 68.78, // 19:00-20:00 56 - 59
		    91.31, 54.62, 45.52, 41.26, // 20:00-21:00 60 - 63
		    60.89, 54.07, 40.26, 26.85, // 21:00-22:00 64 - 67
		    55.72, 61.53, 44.56, 34.19, // 22:00-23:00 68 - 71
		    42.62, 46.79, 41.32, 34.85  // 23:00-00:00
		};
	
	/**
	 * @param lengthK Use "-1" to get full length. (5:00-23:00, 72 slots)
	 * @return
	 */
	public DataGenerator generateEnergyPriceHistory(int lengthK) {
		// Prices from https://www.epexspot.com/en/market-data/intradaycontinuous/intraday-table/2017-10-18/DE
		double[] prices = defaultPrices;
		if (lengthK == -1) {
			lengthK = prices.length;
		}
		ArrayList<Double> allPrices = new ArrayList<>();
		int i=0;
		while(allPrices.size()<lengthK){
		  allPrices.add(prices[i++%prices.length]);
		}
		prices = allPrices.stream().mapToDouble(Double::doubleValue).toArray();
		energyPriceHistory = new EnergyPriceHistory(prices);
		return this;
	}
	
	
}
