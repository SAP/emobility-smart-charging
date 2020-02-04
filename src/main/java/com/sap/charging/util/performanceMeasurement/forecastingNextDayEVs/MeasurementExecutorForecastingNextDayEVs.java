package com.sap.charging.util.performanceMeasurement.forecastingNextDayEVs;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorFromFile;
import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.dataGeneration.carDistributions.CarArrivalDepartureDistribution;
import com.sap.charging.dataGeneration.carDistributions.CarArrivalDepartureDistribution.Tuple;
import com.sap.charging.dataGeneration.carDistributions.CarConsumptionDistributionBEV;
import com.sap.charging.dataGeneration.carDistributions.CarConsumptionDistributionPHEV;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyUtil;
import com.sap.charging.opt.heuristics.util.CarAssignmentPriority;
import com.sap.charging.realTime.StrategyGreedyAssignment;
import com.sap.charging.realTime.model.CarAssignment;
import com.sap.charging.realTime.model.forecasting.soc.CarSoCForecast;
import com.sap.charging.realTime.model.forecasting.soc.CarSoCForecastLinearModel;
import com.sap.charging.realTime.model.forecasting.soc.CarSoCForecastXGBoost;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.JSONKeys;
import com.sap.charging.util.performanceMeasurement.MeasurementExecutor;
import com.sap.charging.util.random.ConstantDistribution;
import com.sap.charging.util.random.DiscreteDistribution;
import com.sap.charging.util.random.Distribution;
import com.sap.charging.util.sqlite.SQLiteDB;

public class MeasurementExecutorForecastingNextDayEVs extends MeasurementExecutor<PerformanceMeasurementForecastingNextDayEVs> {

	private final CarSoCForecastLinearModel carSoCForecastLinearModel;
	private final CarSoCForecastLinearModel carSoCForecastLinearModelAbs;
	private final CarSoCForecastXGBoost carSoCForecastXGBoost;
	private final CarSoCForecastXGBoost carSoCForecastXGBoostAbs;
	
	public MeasurementExecutorForecastingNextDayEVs(SQLiteDB db) {
		this(db, false, null, null, null, null);
	}
	public MeasurementExecutorForecastingNextDayEVs(SQLiteDB db, boolean forceMeasurement, 
			CarSoCForecastLinearModel carSoCForecastLinearModel, CarSoCForecastLinearModel carSoCForecastLinearModelAbs,
			CarSoCForecastXGBoost carSoCForecastXGBoost, CarSoCForecastXGBoost carSoCForecastXGBoostAbs) {
		super(db, forceMeasurement);
		this.carSoCForecastLinearModel = carSoCForecastLinearModel;
		this.carSoCForecastLinearModelAbs = carSoCForecastLinearModelAbs;
		this.carSoCForecastXGBoost = carSoCForecastXGBoost;
		this.carSoCForecastXGBoostAbs = carSoCForecastXGBoostAbs;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void accept(PerformanceMeasurementForecastingNextDayEVs measurement) {
		
		/**
		 * Days:
		 * 0 -> monday
		 * 1 -> tuesday
		 * 2 -> wednesday
		 * 3 -> thursday
		 * 4 -> friday 
		 * 5 -> saturday (NO CHARGING + SIMULATION)
		 * 6 -> sunday (NO CHARGING + SIMULATION)
		 */
		
		
		int weekday = measurement.day % 7;
		if (weekday == 5 || weekday == 6) {
			// Don't simulate saturday or sunday
			return;
		}
		
		if (forceMeasurement == true && db.rowExists(measurement) == true) {
			db.deleteRow(measurement);
		}
		if (db.rowExists(measurement) == true) {
			System.out.println(measurement.toString() + " already exists.");
			return;
		}
		
		
		
		Random randomNumberGenerator = new Random(measurement.seed + 1000 * measurement.day);
		DiscreteDistribution socUsageDistributionBEV = new DiscreteDistribution(randomNumberGenerator, 
				CarConsumptionDistributionBEV.binSize, CarConsumptionDistributionBEV.distribution[weekday]);
		
		DiscreteDistribution socUsageDistributionPHEV = new DiscreteDistribution(randomNumberGenerator, 
				CarConsumptionDistributionPHEV.binSize, CarConsumptionDistributionPHEV.distribution[weekday]);
		
		
		
		DataGenerator dataSim = null;
		
		if (measurement.day == 0) { // Start on monday
			
			DataGeneratorRandom dataInitial = new DataGeneratorRandom(measurement.seed, false);
			dataInitial.setIdealCars(true);
			dataInitial.setIdealChargingStations(true);
			
			// Start with 0 curCapacity
			ConstantDistribution startDistribution = new ConstantDistribution(dataInitial.getRandom(), 0);
			dataInitial.setCurCapacityDistribution(startDistribution);
			
			dataInitial.generateEnergyPriceHistory(96)
					   .generateCars(measurement.nCars)
					   .generateChargingStations(measurement.nChargingStations)
					   .generateFuseTree(measurement.nCars, true);
			
			// Initial SoC should be 1 - choose from meterTotalDistribution based on day
			for (int n=0;n<dataInitial.getCars().size();n++) {
				Car car = dataInitial.getCars().get(n);
				
				Distribution socDistribution = car.isPHEV() ? socUsageDistributionPHEV : socUsageDistributionBEV;
				
				double kwhUsed = socDistribution.getNextDouble()/1000.0;
				double ahUsed = EnergyUtil.calculateIFromP(kwhUsed, 1); // Ah
				//System.out.println("energyUsed: " + kwhUsed + "kwh, " + ahUsed + "Ah");
				double initialAbsSoC = Math.max(car.getMaxCapacity() - ahUsed, 0); // Never go below 0 
				car.setCurrentCapacity(initialAbsSoC);
			}
			
			dataSim = dataInitial;
			
		}
		else if (measurement.day >= 1) {
			DataGeneratorFromFile dataPreviousDay = getDataPreviousDay(weekday, measurement);
			
			// Adapt car availability (randomize every day) before generating cars
			JSONArray carsJSON = (JSONArray) dataPreviousDay.getProblemInstance().get(JSONKeys.JSON_KEY_CARS);
			for (int n=0;n<carsJSON.size();n++) {
				JSONObject carJSON = (JSONObject) carsJSON.get(n);
				
				double rArrivalDeparture = randomNumberGenerator.nextDouble();
				Tuple arrivalDepartureTuple = CarArrivalDepartureDistribution.getTimeslotsFromDistribution(rArrivalDeparture, 96);
				
				//System.out.println("Original arrivalTimeslot: " + carJSON.get(JSONKeys.JSON_KEY_CAR_FIRST_AVAILABLE_TIMESLOT));
				carJSON.put(JSONKeys.JSON_KEY_CAR_FIRST_AVAILABLE_TIMESLOT, arrivalDepartureTuple.arrivalTimeslot);
				carJSON.put(JSONKeys.JSON_KEY_CAR_LAST_AVAILABLE_TIMESLOT, arrivalDepartureTuple.departureTimeslot);
				//System.out.println("New arrivalTimeslot: " + carJSON.get(JSONKeys.JSON_KEY_CAR_FIRST_AVAILABLE_TIMESLOT));
				
				
				//System.out.println("Original arrivalTimestamp: " + carJSON.get(JSONKeys.JSON_KEY_CAR_TIMESTAMP_ARRIVAL));
				LocalTime[] arrivalDepartureTimestamps = DataGeneratorRandom.getRandomTimestamps(arrivalDepartureTuple, randomNumberGenerator.nextDouble(), randomNumberGenerator.nextDouble());
				carJSON.put(JSONKeys.JSON_KEY_CAR_TIMESTAMP_ARRIVAL, arrivalDepartureTimestamps[0].toSecondOfDay());
				carJSON.put(JSONKeys.JSON_KEY_CAR_TIMESTAMP_DEPARTURE, arrivalDepartureTimestamps[1].toSecondOfDay());
				
				//System.out.println("New arrivalTimestamp: " + carJSON.get(JSONKeys.JSON_KEY_CAR_TIMESTAMP_ARRIVAL));
				
			}
			
			
			dataPreviousDay.generateAll();
			
			
			// Adapt starting car SoC (curCapacity)
			// New starting SoC: First add current (starting capacity) + charged Capacity [charged capacity was that which was charged previous day]
			// Then subtract number from distribution, that was used before coming to work on this day
			
			for (Car car : dataPreviousDay.getCars()) {
				double ahAtEndOfPreviousDay = car.getCurrentCapacity() + car.getChargedCapacity();
				
				DiscreteDistribution socDistribution = car.isPHEV() ? socUsageDistributionPHEV : socUsageDistributionBEV;
				
				double kwhUsed = socDistribution.getNextDouble()/1000.0;
				double ahUsed = EnergyUtil.calculateIFromP(kwhUsed, 1); // Ah
				
				double initialAbsSoC = Math.max(ahAtEndOfPreviousDay - ahUsed, 0);
				car.resetChargedCapacity();
				car.setCurrentCapacity(initialAbsSoC);
				
				//System.out.println(car);
			}
			
			dataSim = dataPreviousDay;
			
		}
		
		
		
		if (forceMeasurement == true && db.rowExists(measurement) == true) {
			db.deleteRow(measurement);
		}
		if (db.rowExists(measurement) == false) {
			// Add filePath, guid and method
			PerformanceMeasurementForecastingNextDayEVs measurementWithGUID = measurement.cloneWithMethod(StrategyGreedyAssignment.getMethodStatic());
			
			PerformanceMeasurementForecastingNextDayEVs previousMeasurement = null;
			DataGeneratorFromFile dataPreviousDay = null;
			if (measurement.day >= 1) {
				previousMeasurement = getPreviousMeasurement(weekday, measurement);
				dataPreviousDay = getDataPreviousDay(weekday, measurement);
				dataPreviousDay.generateAll();
			}
			
			
			// Set car assignments in advance
			List<CarAssignment> plannedCarAssignments = getPlannedCarAssignmentsByMethod(dataSim, dataPreviousDay, measurement, previousMeasurement);
			
			System.out.println("Simulating " + measurementWithGUID + " with plannedCarAssignments " + plannedCarAssignments);
			
			StrategyGreedyAssignment strategyAssignment = new StrategyGreedyAssignment(plannedCarAssignments);

			Simulation sim = new Simulation(dataSim, strategyAssignment);
			sim.init();
			sim.simulate();
			
			db.insert(measurementWithGUID);	
			FileIO.writeFile(measurementWithGUID.filePath, sim.getSimulationResult().getSolvedProblemInstanceJSON());
		}
		else {
			System.out.println(measurement.toString() + " already exists.");
		}
		
		
	}
	
	private PerformanceMeasurementForecastingNextDayEVs getPreviousMeasurement(int weekday, PerformanceMeasurementForecastingNextDayEVs measurement) {
		// Get measurement from previous day: retrieve file and GUID from db
		int dayToRetrieve = (weekday != 0) ? measurement.day-1 : measurement.day-3; // If it is a monday, skip weekends
		PerformanceMeasurementForecastingNextDayEVs previousMeasurement = measurement.getPreviousDayMeasurement(db, dayToRetrieve); 
		//System.out.println(previousMeasurement);
		return previousMeasurement;
	}
	
	
	private DataGeneratorFromFile getDataPreviousDay(int weekday, PerformanceMeasurementForecastingNextDayEVs measurement) {
		PerformanceMeasurementForecastingNextDayEVs previousMeasurement = getPreviousMeasurement(weekday, measurement);
		
		// Retrieve data from file
		DataGeneratorFromFile dataPreviousDay = new DataGeneratorFromFile(previousMeasurement.filePath);
		return dataPreviousDay;
	}
	
	private List<CarAssignment> getPlannedCarAssignmentsByMethod(DataGenerator data,  DataGenerator dataPreviousDay, PerformanceMeasurementForecastingNextDayEVs measurement, PerformanceMeasurementForecastingNextDayEVs previousMeasurement) {
		int nCars = measurement.nCars;
		int nChargingStations = measurement.nChargingStations;
		int day = measurement.day;
		
		String carAssignmentMethod = measurement.carAssignmentMethod;
		List<CarAssignment> plannedCarAssignments = new ArrayList<CarAssignment>();
		
		if (carAssignmentMethod.equals("firstComeFirstServed")) {
			boolean[][] chargingStationAssigned = new boolean[data.getChargingStations().size()][data.getEnergyPriceHistory().getNTimeslots()];
			
			List<Integer> sortedCarIDs = CarAssignmentPriority.sortCarIdByArrivalTimeslot(data.getCars());
			for (int n : sortedCarIDs) {
				Car car = data.getCars().get(n);
				boolean carIsAssigned = false;
				
				for (int i=0;i<chargingStationAssigned.length && carIsAssigned == false;i++) {
					ChargingStation chargingStation = data.getChargingStations().get(i);
					
					boolean isFreeEntireTime = true; // Free entire time of car stay
					for (int k=car.getFirstAvailableTimeslot();k<=car.getLastAvailableTimeslot();k++) {
						if (chargingStationAssigned[i][k] == true) {
							// If any slots of the charging station are already assigned, 
							isFreeEntireTime = false;
						}
					}
					
					if (isFreeEntireTime == true) {
						carIsAssigned = true;
						
						// If a charging station is free at first available timeslot: take it!
						// Reserve chargingStation for slots in which the car is there
						for (int k=car.getFirstAvailableTimeslot();k<=car.getLastAvailableTimeslot();k++) {
							chargingStationAssigned[i][k] = true;
						}
						
						CarAssignment carAssignment = new CarAssignment(car, chargingStation);
						plannedCarAssignments.add(carAssignment);
					}
				}
			}
		}
		else if (carAssignmentMethod.equals("roundRobin")) {
			// Do a sliding window of which car gets to charge on which day
			int workday = day - (int) (Math.floor(day/7))*2;    // nth workday, take out all weekend days
			
			int carMinID = (workday * nChargingStations) % nCars;
			int carMaxID = (carMinID + nChargingStations -1) % nCars; // Inclusive
			
			System.out.println("day=" + day + ", workday=" + workday + ", carMinID=" + carMinID + ", carMaxID=" + carMaxID);
			
			int currentChargingStationID = 0;
			for (int n=0;n<data.getCars().size();n++) {
				Car car = data.getCars().get(n);
				if ((car.getId() >= carMinID && car.getId() <= carMaxID) || // example: between 0,4 or 5,9
					(carMinID+nChargingStations-1 > nCars && (car.getId() >= carMinID || car.getId() <= carMaxID))) { // example: between 10,2 (cycle)
					CarAssignment carAssignment = new CarAssignment(car, data.getChargingStations().get(currentChargingStationID));
					plannedCarAssignments.add(carAssignment);
					currentChargingStationID++;
				}
			}
			
		}
		else if (carAssignmentMethod.equals("oracle")) {
			// Prioritize cars by minSoCReached (relative)
			List<Integer> sortedCarIDs = CarAssignmentPriority.sortCarIdByRelSoC(data.getCars());
			
			/*for (int n : sortedCarIDs) {
				Car car = data.getCars().get(n);
				System.out.println(car);
			}*/
			
			int currentChargingStationID = 0;
			
			for (int n : sortedCarIDs) {
				Car car = data.getCars().get(n);
				
				CarAssignment carAssignment = new CarAssignment(car, data.getChargingStations().get(currentChargingStationID));
				plannedCarAssignments.add(carAssignment);
				currentChargingStationID++;
				
				if (currentChargingStationID == nChargingStations) {
					break;
				}
			}
			
		}
		else if (carAssignmentMethod.equals("forecastLinear")) {
			// Prioritize cars by forecasted minSoC 
			List<Double> forecastedSoCs = getForecastedSoCs(carSoCForecastLinearModel, data, dataPreviousDay, measurement, previousMeasurement);
			List<Integer> sortedCarIDs = CarAssignmentPriority.sortCarByForecastedSoC(data.getCars(), forecastedSoCs);
			addPlannedCarAssignments(plannedCarAssignments, sortedCarIDs, data);
		}
		else if (carAssignmentMethod.equals("forecastLinearAbs")) {
			// Prioritize cars by forecasted minSoC 
			List<Double> forecastedSoCs = getForecastedSoCs(carSoCForecastLinearModelAbs, data, dataPreviousDay, measurement, previousMeasurement);
			List<Integer> sortedCarIDs = CarAssignmentPriority.sortCarByForecastedSoC(data.getCars(), forecastedSoCs);
			addPlannedCarAssignments(plannedCarAssignments, sortedCarIDs, data);
		}
		else if (carAssignmentMethod.equals("forecastXGBoost")) {
			// Prioritize cars by forecasted minSoC 
			List<Double> forecastedSoCs = getForecastedSoCs(carSoCForecastXGBoost, data, dataPreviousDay, measurement, previousMeasurement);
			List<Integer> sortedCarIDs = CarAssignmentPriority.sortCarByForecastedSoC(data.getCars(), forecastedSoCs);
			addPlannedCarAssignments(plannedCarAssignments, sortedCarIDs, data);
		}
		else if (carAssignmentMethod.equals("forecastXGBoostAbs")) {
			// Prioritize cars by forecasted minSoC 
			List<Double> forecastedSoCs = getForecastedSoCs(carSoCForecastXGBoostAbs, data, dataPreviousDay, measurement, previousMeasurement);
			List<Integer> sortedCarIDs = CarAssignmentPriority.sortCarByForecastedSoC(data.getCars(), forecastedSoCs);
			addPlannedCarAssignments(plannedCarAssignments, sortedCarIDs, data);
		}
		
		
		if (plannedCarAssignments.size() < data.getChargingStations().size() && plannedCarAssignments.size() < data.getCars().size()) {
			System.out.println("WARNING: Less planned car assignments (" + plannedCarAssignments.size() + ") than available charging stations (" + data.getChargingStations().size() + ") for method=" + carAssignmentMethod);
		}
		
		return plannedCarAssignments;
	}
	
	private void addPlannedCarAssignments(List<CarAssignment> plannedCarAssignments, List<Integer> sortedCarIDs, DataGenerator data) {
		int nChargingStations = data.getChargingStations().size();
		int currentChargingStationID = 0;
		
		for (int indexN : sortedCarIDs) {
			Car car = data.getCars().get(indexN);
			//System.out.println("forecast SoC:" +  forecastedSoCs.get(indexN) + ", actual SoC: " + car.getCurrentCapacity() / car.maxCapacity);
			
			CarAssignment carAssignment = new CarAssignment(car, data.getChargingStations().get(currentChargingStationID));
			plannedCarAssignments.add(carAssignment);
			currentChargingStationID++;
			
			if (currentChargingStationID == nChargingStations) {
				break;
			}
		}
	}
	
	private List<Double> getForecastedSoCs(CarSoCForecast carSoCForecast, DataGenerator data, DataGenerator dataPreviousDay, 
			PerformanceMeasurementForecastingNextDayEVs measurement, PerformanceMeasurementForecastingNextDayEVs previousMeasurement) {
		List<Double> forecastedSoCs = new ArrayList<>();
		for (Car car : data.getCars()) {
			int indexN = car.getId();
			
			double forecastedSoC = 0;
			if (dataPreviousDay != null) {
				forecastedSoC = carSoCForecast.getExpectedSoC(dataPreviousDay.getCars().get(indexN), car, 
						previousMeasurement.day, measurement.day);
			}
			
			forecastedSoCs.add(forecastedSoC);
		}
		return forecastedSoCs;
	}
	
	

}






