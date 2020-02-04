package com.sap.charging.dataGeneration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import com.sap.charging.model.Car;
import com.sap.charging.model.CarFactory;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyUtil;
import com.sap.charging.model.CarFactory.CarModel;
import com.sap.charging.model.CarFactory.CarType;
import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.util.TimeUtil;

public class DataGeneratorReal extends DataGenerator {
	
	private DataGeneratorRealDB db;
	private final String chosenDate;
	private final String carParkName; 
	private final int seed;
	private final Random random;
	private final boolean limitNumberChargingStations;
	private final boolean allowAllCarTypesAtChargingStation;	
	
	/**
	 * Ratio should be chosen from historical data. For example, the ratio should be set to 0.2 if the dataset has 100 BEVs and 400 PHEVs. 
	 */
	private static final double ratioBEV_PHEV = 100 / (100+400); 
	
	
	
	/**
	 * Deterministic data generator based on real data. 
	 * Data is taken from a cleansed version of 
	 * Chargelog_anonymus_20171214.xlsx.
	 * <br>
	 * Order to call generate functions in:<br>
	 * generateEnergyPriceHistory -> <br>
	 * generateCars (because of nTimeslots) -> <br>
	 * generateFuseTree (because of nCars) -> <br>
	 * generateChargingStations (because charging stations are contained in real fuse tree)<br>
	 * 
	 * @param date The day to be used, example "2017-12-31"
	 * @param seed The seed to be used for RNG, e.g. assigning specific car models
	 * @param limitNumberChargingStations In this dataset, there are never more cars 
	 * then there are charging stations. As such, the number of charging stations can be limited
	 * to max. number of cars. With false, 79 charging stations are returned, else 
	 * getCars().size() charging stations are returned. 
	 *
	 */
	public DataGeneratorReal(String date, int seed, 
			boolean limitNumberChargingStations, boolean allowAllCarTypesAtChargingStation) {
		this.db = new DataGeneratorRealDB();
		this.chosenDate = date;
		this.seed = seed;
		this.random = new Random(this.seed);
		this.limitNumberChargingStations = limitNumberChargingStations;
		this.carParkName = "Insert car park here"; 
		this.allowAllCarTypesAtChargingStation = allowAllCarTypesAtChargingStation;
	}
	
	/**
	 * Simplified constructor for deterministic data generator based on real data:
	 * seed is set to 0,
	 * the number of charging stations is not limited (by default 79 are returned),
	 * chargingStation settings (isBEVAllowed and isPHEVAllowed) are kept as real values.
	 * @param date
	 */
	public DataGeneratorReal(String date) {
		this(date, 0, false, false);
	}
	
	@Override
	public DataGenerator generateFuseTree(int nChargingStationsPerLevel2, boolean doRotatePhases) {
		int nChargingStations = (limitNumberChargingStations) ? getCars().size() : 0;
		this.fuseTree = db.retrieveFuseTree(nChargingStations, this.chargingStations);
		return this;
	}
	
	/**
	 * Convenience method for retrieving all data neccessary for chosen date. 
	 * @return
	 */
	public DataGenerator generateAll() {
		return this.generateEnergyPriceHistory(-1)
				   .generateCars(-1)
				   .generateChargingStations(-1)
				   .generateFuseTree(-1, true);
	}
	
	/**
	 * Returns a new instance of DataGenerator using the same parameters as for this instance. 
	 * The original instance is NOT changed. Any temporary variables (such as car.chargedCapacity) are reset. 
	 * @return
	 */
	public DataGeneratorReal clone() {
		DataGeneratorReal clone = new DataGeneratorReal(this.chosenDate, this.seed, this.limitNumberChargingStations, this.allowAllCarTypesAtChargingStation);
		clone.setIdealCars(this.generateIdealCars);
		clone.setIdealChargingStations(this.generateIdealChargingStations);
		clone.generateAll();
		return clone;
	}
	
	
	
	//private int nChargingStationsAdded = 0;
	
	/**
	 * 
	 * @param nChargingStations
	 */
	@Override
	public DataGenerator generateChargingStations(int nChargingStations) {
		this.chargingStations = new ArrayList<>();
		
		/*this.fuseTree.traverseTree(new Callback<FuseTreeNode>() {
			@Override
			public void callback(FuseTreeNode item) {
				if (limitNumberChargingStations == true && 
					nChargingStationsAdded >= getCars().size()) {
					// If limiting the number of charging stations by max
					// number of cars, don't add any more charging stations
					return;
				}
				if (item instanceof ChargingStation) {
					chargingStations.add((ChargingStation) item);
					((ChargingStation) item).setIndexI(minChargingStationId+nChargingStationsAdded);
					nChargingStationsAdded++;
					
				}
				
			}
		});*/
		if (allowAllCarTypesAtChargingStation == true) {
			for (ChargingStation cs : chargingStations) {
				cs.isBEVAllowed = true;
				cs.isPHEVAllowed = true;
			}
		}
		
		return this;
	}

	
	/**
	 * Generates a CarType given a rawCarType
	 * @param rawCarType Accepted values: "B", "P", "N/A"
	 * @return
	 */
	public CarType generateCarType(String rawCarType) {
		if (rawCarType.equals("B")) 
			return CarType.BEV;
		if (rawCarType.equals("P"))
			return CarType.PHEV;
		if (rawCarType.equals("N/A")) {
			double carTypeRandom = random.nextDouble();
			return carTypeRandom < ratioBEV_PHEV ?
					CarType.BEV : CarType.PHEV;
		}
		throw new NullPointerException("CarType " + rawCarType + " is unknown.");
	}
	
	/**
	 * Get possible car models for this raw car data row
	 * Depends on:
	 * MeterTotal (==> check Model.MaxCapacity)
	 * CarType (==> check Model.CarType)
	 * @param amountCharged (in Ah), actual amount charged, energy into battery (efficiency taken into account!)
	 * @param rawCarType
	 * @param availableTimeslots How many timeslots is the car available for?
	 * @return
	 */
	public CarModel[] generatePossibleCarModels(double amountCharged, String rawCarType, int secondsAvailable) {
		// Filter by amountCharged
		CarModel[] possibleCarModels = Arrays.stream(CarModel.values())
				.filter(m -> m.maxCapacity >= amountCharged) // Capacity of car must be above the amount charged
				// Make sure the car can be fully charged while there (assuming 16A fuses)
				// Add a little buffer (0.01) because in reality cars may charge a little more than 16A without the station switching off
				.filter(m -> m.getSumPhases()*16*0.85*secondsAvailable/3600.0 >= amountCharged-0.1) 
				.toArray(CarModel[]::new);
		
		// Get possible types (can be [BEV] or [BEV, PHEV]) 
		CarType[] possibleTypes = Arrays.stream(possibleCarModels)
				.map(m -> m.carType)
				.distinct()
				.toArray(CarType[]::new);
		
		CarType carType;
		if (possibleTypes.length == 1) { // If only BEV is remaining, chose it
			// Only BEV possible
			carType = possibleTypes[0];
		}
		else { // Else actually generate (pick by random according to ratio of BEVs to PHEVs)
			carType = generateCarType(rawCarType);
		}
		
		possibleCarModels = Arrays.stream(possibleCarModels)
					.filter(m -> m.carType == carType)
					.toArray(CarModel[]::new);
		
		if (possibleCarModels.length == 0) 
			throw new NullPointerException("No possible car model found for: amountCharged=" + 
											amountCharged + ", max amount chargable in time: " + 3*16*0.85*secondsAvailable/3600.0 +
											", rawCarType='" +
											rawCarType + "', secondsAvailable=" + secondsAvailable + 
											", date=" + getEnergyPriceHistory().getDate());
		
		return possibleCarModels;
	}
	
	/**
	 * Generates a random car model (uniform distribution) depending on
	 * possible models, restricted by amountCharged and rawCarType. 
	 * @param amountCharged
	 * @param rawCarType
	 * @return
	 */
	public CarModel generateCarModel(double amountCharged, String rawCarType, int secondsAvailable) {
		CarModel[] possibleCarModels = generatePossibleCarModels(amountCharged, rawCarType, secondsAvailable);
		return possibleCarModels[random.nextInt(possibleCarModels.length)];
	}
	
	
	/**
	 * Retrieves a set of cars from real data.
	 * @param nCars Is ignored
	 * @return
	 */
	@Override
	public DataGenerator generateCars(int nCars) {
		int nTimeslots = getEnergyPriceHistory().getNTimeslots();
		
		try {
			ResultSet carsRaw = db.retrieveCars(chosenDate, this.carParkName);
			this.cars = new ArrayList<>();
			int id = minCarId;
			while (carsRaw.next()) {
				// Amount charged in kW
				double amountCharged = carsRaw.getDouble("amountCharged");
				// Convert to Ah
				amountCharged = EnergyUtil.calculateIFromP(amountCharged, 1);
				// Take into account efficiency: metertotal said this much, but in reality charged less
				amountCharged *= CONSTANTS.CHARGING_EFFICIENCY;
				
				
				int timestampArrival = carsRaw.getInt("timestampArrival");
				int timestampDeparture = carsRaw.getInt("timestampDeparture");
				int firstAvailableTimeslot = TimeUtil.getTimeslotFromSeconds(timestampArrival);
				int lastAvailableTimeslot = TimeUtil.getTimeslotFromSeconds(timestampDeparture);
				
				String rawCarType = carsRaw.getString("carType").trim();
				CarModel carModel = generateCarModel(amountCharged, rawCarType, timestampDeparture-timestampArrival);
				double curCapacity = carModel.maxCapacity - amountCharged;
				
				Car car = CarFactory.builder()
						.id(id++)
						.set(carModel)
						.availableTimeslots(firstAvailableTimeslot, lastAvailableTimeslot, nTimeslots)
						.availableTimestamps(TimeUtil.getTimestampFromSeconds(timestampArrival), 
											 TimeUtil.getTimestampFromSeconds(timestampDeparture))
						.currentCapacity(curCapacity)
						.build();
				car.setIdealCar(generateIdealCars);
				cars.add(car);
			}
			return this;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return this;
	}

	/**
	 * Data is retrieved from EPEX spot for date given in constructor. 
	 * A full day (24*4=96 timeslots) is returned, lengthK is ignored.
	 * @param lengthK Ignored. 
	 */
	@Override
	public DataGenerator generateEnergyPriceHistory(int lengthK) {
		this.energyPriceHistory = db.retrieveEnergyPriceHistory(chosenDate);
		return this;
	}
	
	

}
