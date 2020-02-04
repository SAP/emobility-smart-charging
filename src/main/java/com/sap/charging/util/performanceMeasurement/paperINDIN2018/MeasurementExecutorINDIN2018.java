package com.sap.charging.util.performanceMeasurement.paperINDIN2018;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.model.Car;
import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.realTime.Strategy;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.StrategyGreedy;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.JSONKeys;

public class MeasurementExecutorINDIN2018 implements Consumer<PerformanceMeasurementINDIN2018Template> {
	
	private DBINDIN2018 db;
	public static int seed = 0;
	
	public MeasurementExecutorINDIN2018(DBINDIN2018 db) {
		this.db = db;
	}
	
	@Override
	public void accept(PerformanceMeasurementINDIN2018Template measurement) {
		
		CONSTANTS.FUSE_LEVEL_0_SIZE = measurement.getGridConnection();
		CONSTANTS.FUSE_LEVEL_1_SIZE = measurement.getGridConnection();
		CONSTANTS.FUSE_LEVEL_2_SIZE = measurement.getGridConnection();
		
		DataGeneratorRandom data = new DataGeneratorRandom(seed, false);
		data.setIdealCars(true);
		data.setIdealChargingStations(true);
		data.generateEnergyPriceHistory(96)
			.generateCars(measurement.getNCars()) 
			.generateChargingStations(measurement.getNCars())
			.generateFuseTree(measurement.getNCars(), true);
			//.generateFuseTree((int) Math.ceil(measurement.getNCars()/6.0) , true);
		
		
		System.out.println("MeasurementExecutorINDIN2018::nCars=" + data.getCars().size() 
				+ ", nChargingStations=" + data.getChargingStations().size());
		
		// Compare:
		// realtime greedy (scenario 1)
		// realtime algorithmic (scenario 2)
		
		
		System.out.println("---------- Real-time Greedy -----------");
		PerformanceMeasurementINDIN2018 measurementRealTimeGreedy = measurement.cloneWithMethod(StrategyGreedy.getMethodStatic());
		if (!db.measurementExists(measurementRealTimeGreedy)) {
			Strategy strategyGreedy = new StrategyGreedy();
			executeMeasurement(measurementRealTimeGreedy, data.clone(), strategyGreedy);
		}
		
		System.out.println("---------- Real-time algorithmic (without schedule) -----------");
		PerformanceMeasurementINDIN2018 measurementRealTimeAlgorithmic = measurement.cloneWithMethod(StrategyAlgorithmic.getMethodWithoutScheduleStatic());
		if (!db.measurementExists(measurementRealTimeAlgorithmic)) {
			Strategy strategyAlgorithmic = new StrategyAlgorithmic();
			executeMeasurement(measurementRealTimeAlgorithmic, data.clone(), strategyAlgorithmic);
		}
		
		/*System.out.println("---------- Real-time algorithmic using cars data from scenario 3 -----------");
		PerformanceMeasurementINDIN2018 measurementRealTimeScenario3 = measurement.cloneWithMethod("realTimeAlgorithmicScenario3");
		if (!db.measurementExists(measurementRealTimeScenario3)) {
			Strategy strategyAlgorithmic = new StrategyAlgorithmic();
			List<Car> modifiedCars = getCarsFromJsonScenario3(measurement.getNCars(), data);
			
			data.getCars().clear();
			data.getCars().addAll(modifiedCars);
			
			
			executeMeasurement(measurementRealTimeScenario3, data, strategyAlgorithmic);
		}*/
		
	}
	
	@SuppressWarnings("unchecked")
	public List<Car> getCarsFromJsonScenario3(int nCars, DataGenerator originalData) {
		String carsJsonFilePath = "gen/carsData_INDIN2018_scenario3_v3/cars_" + nCars + ".json";
		ArrayList<Car> modifiedCars = new ArrayList<>();
		try {
			JSONArray carsJson = (JSONArray) (new JSONParser()).parse(FileIO.readFile(carsJsonFilePath));
			for (Object carObject : carsJson) {
				JSONObject carJson = (JSONObject) carObject;
				double curCapacityKWh = (double) carJson.get(JSONKeys.JSON_KEY_CAR_CUR_CAPACITY);
				double curCapacityAh = curCapacityKWh*1000/230; 
				double maxCapacity = (double) carJson.get(JSONKeys.JSON_KEY_CAR_MAX_CAPACITY);
				
				// Set minLoadingState to maxCapacity - curCapacity
				carJson.put(JSONKeys.JSON_KEY_CAR_MIN_LOADING_STATE, maxCapacity - curCapacityAh);
				
				
				// Use curCapacity from original array
				int indexN = (int) (long) carJson.get(JSONKeys.JSON_KEY_INDEX_N);
				Car originalCar = originalData.getCars().stream().filter(car -> car.getId() == indexN).findFirst().get();
				carJson.put(JSONKeys.JSON_KEY_CAR_CUR_CAPACITY, originalCar.getCurrentCapacity());

				Car car = Car.fromJSON(carJson, 96);
				//System.out.println(car);
				modifiedCars.add(car);
			}
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
		return modifiedCars;
	}
	
	
	public void executeMeasurement(PerformanceMeasurementINDIN2018 performanceMeasurement, DataGeneratorRandom data, Strategy strategy) {
		Simulation sim = new Simulation(data, strategy);
		sim.init();
		sim.simulate();
		JSONObject result = sim.getSimulationResult().getSolvedProblemInstanceJSON();
		db.insertMeasurement(performanceMeasurement, result);
	}
	
}
