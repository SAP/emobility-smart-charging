package com.sap.charging.model;

import static org.junit.Assert.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.dataGeneration.common.DefaultDataGenerator;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.util.JSONKeys;

public class EnergyPriceHistoryTest {

	
	private EnergyPriceHistory getDefaultHistory() {
		DataGenerator data = DefaultDataGenerator.getDefaultDataGenerator();
		return data.getEnergyPriceHistory();
	}
	
	@Test
	public void testToJSON() {
		EnergyPriceHistory energyPriceHistory = getDefaultHistory();
		double[] originalPrices = energyPriceHistory.getPrices();
		JSONObject result = energyPriceHistory.toJSONObject();
		assertNotNull(result.get(JSONKeys.JSON_KEY_ENERGY_PRICES));
		assertNull(result.get(JSONKeys.JSON_KEY_ENERGY_DATE));
		
		JSONArray jsonPrices = (JSONArray) result.get(JSONKeys.JSON_KEY_ENERGY_PRICES);
		for (int k=0;k<jsonPrices.size();k++) {
			double price = (double) jsonPrices.get(k);
			assertEquals(originalPrices[k], price, 1e-8);
		}
	}

	@Test
	public void testFromJSONWithDate() throws ParseException {
		String json = "{"
				+ "\"date\": \"2017-12-31\","
				+ "\"energyPrices\": [10.5, 20.0, 30.0]" +
				"}";
		
		JSONObject jsonObject = (JSONObject) new JSONParser().parse(json);
		
		EnergyPriceHistory result = EnergyPriceHistory.fromJSON(jsonObject);
		assertEquals(10.5, result.getPrice(0), 1e-8);
		assertEquals(20, result.getPrice(1), 1e-8);
		assertEquals(30, result.getPrice(2), 1e-8);
		assertEquals("2017-12-31", result.getDate());
	}
	
	@Test
	public void testFromJSONWithoutDate() throws ParseException {
		String jsonWithoutDate = "{"
				+ "\"energyPrices\": [10.5, 20.0, 30.0]" +
				"}";
	
		JSONObject jsonObjectWithoutDate = (JSONObject) new JSONParser().parse(jsonWithoutDate);
		
		EnergyPriceHistory resultWithoutDate = EnergyPriceHistory.fromJSON(jsonObjectWithoutDate);
		assertEquals(10.5, resultWithoutDate.getPrice(0), 1e-8);
		assertEquals(20, resultWithoutDate.getPrice(1), 1e-8);
		assertEquals(30, resultWithoutDate.getPrice(2), 1e-8);
		assertEquals(null, resultWithoutDate.getDate());
	}
	
	@Test
	public void testToAndFromJSON() throws ParseException {
		double[] prices = DataGeneratorRandom.defaultPrices;
		EnergyPriceHistory original = new EnergyPriceHistory(prices);
		
		// Export
		String json = original.toJSONObject().toString();
		JSONObject jsonObject = (JSONObject) new JSONParser().parse(json);
		
		// Import
		EnergyPriceHistory copy = EnergyPriceHistory.fromJSON(jsonObject);
		
		for (int k=0;k<original.getPrices().length;k++) {
			assertEquals(original.getPrice(k), copy.getPrice(k), 1e-8);
		}
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
