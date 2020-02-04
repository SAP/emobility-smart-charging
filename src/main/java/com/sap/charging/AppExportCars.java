package com.sap.charging;

import org.json.simple.JSONArray;

import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.model.Car;
import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.util.FileIO;

public class AppExportCars {
	
	private static String path = "gen/carsData/";
	
	public static void main(String[] args) {
		exportRandomCars();
	}
	
	@SuppressWarnings("unchecked")
	private static void exportRandomCars() {
		CONSTANTS.FUSE_LEVEL_0_SIZE = 4000;
		CONSTANTS.FUSE_LEVEL_1_SIZE = 4000;
		CONSTANTS.FUSE_LEVEL_2_SIZE = 4000;
		int seed = 0;
		
		for (int nCars=100;nCars<=2000;nCars=nCars+100) {
			DataGeneratorRandom data = new DataGeneratorRandom(seed, false);
			data.setIdealCars(true);
			data.setIdealChargingStations(true);
			data.generateEnergyPriceHistory(96)
				.generateCars(nCars);
			
			JSONArray carsArray = new JSONArray();
			for (Car car : data.getCars()) {
				carsArray.add(car.toJSONObject());
			}
			String filePath = path + "cars" + nCars + ".json";
			FileIO.writeFile(filePath, carsArray.toJSONString());
		}
		
		
		
		
		
	}
	
	/*private static void exportAllRealCars() {
		DataGeneratorRealDB dbDates = new DataGeneratorRealDB();
		ArrayList<String> dates = dbDates.retrieveAvailableDates();
		JSONArray carsArrayAll = new JSONArray();
		
		for (String date : dates) {
			DataGeneratorReal data = new DataGeneratorReal(date, 0, true, true);
			data.generateEnergyPriceHistory(-1).generateCars(-1);
			
			JSONArray carsArray = new JSONArray();
			for (Car car : data.getCars()) {
				carsArray.add(car.toJSONObject());
				carsArrayAll.add(car.toJSONObject());
			}
			String filePath = path + date + ".json";
			FileIO.writeFile(filePath, carsArray.toJSONString());
		}
		FileIO.writeFile(path + "all.json", carsArrayAll.toJSONString());
	}
	*/
}
