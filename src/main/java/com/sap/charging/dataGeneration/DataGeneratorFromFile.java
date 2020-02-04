package com.sap.charging.dataGeneration;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.FuseTree;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.JSONKeys;


public class DataGeneratorFromFile extends DataGenerator {
	
	private final String jsonFilePath;
	private JSONObject problemInstance;
	
	public DataGeneratorFromFile(String jsonFilePath) {
		this.jsonFilePath = jsonFilePath;
		JSONParser parser = new JSONParser();
		String jsonFileContent = FileIO.readFile(jsonFilePath);
		try {
			JSONObject jsonFile = (JSONObject) parser.parse(jsonFileContent);
			this.init((JSONObject) jsonFile.get(JSONKeys.JSON_KEY_PROBLEM_INSTANCE));
		} catch (ParseException e) {
			e.printStackTrace();
			this.init(null);
		}
	}
	
	public DataGeneratorFromFile(JSONObject object) {
		this.jsonFilePath = null;
		
		JSONObject result = null;
		if (object.containsKey(JSONKeys.JSON_KEY_PROBLEM_INSTANCE)) {
			result = (JSONObject) object.get(JSONKeys.JSON_KEY_PROBLEM_INSTANCE);
		}
		else if (object.containsKey(JSONKeys.JSON_KEY_ENERGY_PRICE_HISTORY)) {
			result = object;
		}
		else {
			throw new RuntimeException("Object has unexpected structure: " + object.toString());
		}
		
		this.init(result);
	}
	
	private void init(JSONObject problemInstance) {
		this.problemInstance = problemInstance;
	}
	
	public JSONObject getProblemInstance() {
		return this.problemInstance;
	}

	@Override
	public DataGenerator generateChargingStations(int nChargingStations) {
		this.chargingStations = new ArrayList<>();
		JSONArray jsonChargingStations = (JSONArray) problemInstance.get(JSONKeys.JSON_KEY_CHARGING_STATIONS);
		for (int i=0;i<jsonChargingStations.size();i++) {
			JSONObject jsonChargingStation = (JSONObject) jsonChargingStations.get(i);
			ChargingStation chargingStation = ChargingStation.fromJSON(jsonChargingStation);
			this.chargingStations.add(chargingStation);
		}
		return this;
	}

	@Override
	public DataGenerator generateCars(int nCars) {
		this.cars = new ArrayList<>();
		JSONArray jsonCars = (JSONArray) problemInstance.get(JSONKeys.JSON_KEY_CARS);
		for (int i=0;i<jsonCars.size();i++) {
			JSONObject jsonCar = (JSONObject) jsonCars.get(i);
			Car car = Car.fromJSON(jsonCar, this.energyPriceHistory.getNTimeslots());
			this.cars.add(car);
		}
		return this;
	}

	@Override
	public DataGenerator generateEnergyPriceHistory(int lengthK) {
		JSONObject jsonEnergyPriceHistory = (JSONObject) problemInstance.get(JSONKeys.JSON_KEY_ENERGY_PRICE_HISTORY);
		if (jsonEnergyPriceHistory == null) {
			throw new RuntimeException("energyPriceHistory not found in JSON");
		}
		this.energyPriceHistory = EnergyPriceHistory.fromJSON(jsonEnergyPriceHistory);
		return this;
	}
	
	@Override
	public DataGenerator generateFuseTree(int nChargingStationsPerLevel2, boolean doRotatePhases) {
		JSONObject jsonTree = (JSONObject) problemInstance.get(JSONKeys.JSON_KEY_FUSE_TREE);
		this.fuseTree = FuseTree.fromJSON(jsonTree, getChargingStations());
		return this;
	}
	
	public DataGenerator generateAll() {
		return this.generateEnergyPriceHistory(-1)
				   .generateCars(-1)
				   .generateChargingStations(-1)
				   .generateFuseTree(-1, true);
	}
	
	@Override
	public DataGenerator clone() {
		return new DataGeneratorFromFile(this.jsonFilePath);
	}
	
	
}
