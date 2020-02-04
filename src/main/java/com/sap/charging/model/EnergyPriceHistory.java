package com.sap.charging.model;

import java.util.Arrays;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.charging.util.JSONKeys;
import com.sap.charging.util.JSONSerializable;

public class EnergyPriceHistory implements JSONSerializable {
	
	/**
	 * Date describing the day from which this energyPriceHistory is.
	 * Can be null in the case of data generation.
	 * Example: "2017-12-31"
	 */
	private final String date;
	
	/**
     * Describes the price of energy for each timeslot in €/kWh (c_k).
     */
    private final double[] energyPrices;
    
    @JsonCreator
    public EnergyPriceHistory(
    		@JsonProperty("numberOfTimeslots") int nTimeslots, 
    		@JsonProperty("energyPrices") double[] energyPrices) {
    	this((energyPrices != null) ? energyPrices : new double[nTimeslots]); 
    } 
    
    public EnergyPriceHistory(
    		double[] energyPrices) {
    	this(energyPrices, null);
    }
    
    public EnergyPriceHistory(double[] energyPrices, String date) {
    	if (energyPrices == null) {
    		throw new IllegalArgumentException("energyPrices should not be null!"); 
    	}
    	if (energyPrices.length <= 0) {
    		throw new IllegalArgumentException("energyPrices.length=" + energyPrices.length + " but should be at least of length 1"); 
    	}
    	
    	this.energyPrices = energyPrices;
    	this.date = date;
    }
	
    @JsonGetter("energyPrices")
    public double[] getPrices() {
		return energyPrices;
	}
    
    /**
     * 
     * @param k Timeslot k
     * @return
     */
    public double getPrice(int k) {
    	return this.energyPrices[k];
    }
    
    @JsonIgnore
    public int getNTimeslots() {
    	return this.energyPrices.length;
    }

    @JsonIgnore
    public double getHighestPrice() {
    	return Arrays.stream(energyPrices).max().getAsDouble();
    }
    
    /**
     * Returns a JSON object with the following format:
     * {
     * 	  "date": "2017-12-31",
	 * 	  "energyPrices": [
	 * 		 50, 30, 50, 40, 10
	 * 	  ]
	 * }
     */
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJSONObject() {
		JSONObject result = new JSONObject();
		
		result.put(JSONKeys.JSON_KEY_ENERGY_DATE, date);
		
		JSONArray prices = new JSONArray();
		result.put(JSONKeys.JSON_KEY_ENERGY_PRICES, prices);
		for (double price : energyPrices) {
			prices.add(price);
		}
		
		return result;
	}
	
	
	/**
	 * Expected format: 
	 * {
	 * 	  "date": "2017-12-31", 
	 * 	  "energyPrices": [
	 * 		 50, 30, 50, 40, 10
	 * 	  ]
	 * }
	 * @param jsonObject
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static EnergyPriceHistory fromJSON(JSONObject jsonObject) {
		String date = (String) jsonObject.get(JSONKeys.JSON_KEY_ENERGY_DATE);
		
		JSONArray jsonPrices = (JSONArray) jsonObject.get(JSONKeys.JSON_KEY_ENERGY_PRICES);
		double[] energyPrices = jsonPrices.stream()
				.mapToDouble(p -> Double.valueOf(p.toString()))
				.toArray();
		return new EnergyPriceHistory(energyPrices, date);
	}
	
	/**
	 * Trims energyPrices (like substring) given a startTimeslot and endTimeslot (both inclusive). 
	 * Example: <br>
	 * energyPrices = [5, 10, 20, 10, 15] <br>
	 * startTimeslot = 1 <br>
	 * endTimeslot = 3 <br>
	 * result = [10, 20, 10]
	 * 
	 * @param startTimeslot 
	 * @param endTimeslot
	 * 
	 */
	public EnergyPriceHistory trim(int startTimeslot, int endTimeslot) {
		if (startTimeslot > endTimeslot) {
			throw new RuntimeException("EnergyPriceHistory::trim startTimeslot=" + startTimeslot + " is greater than"
					+ " endTimeslot=" + endTimeslot + ".");
		}
		double[] energyPricesSubset = Arrays.copyOfRange(energyPrices, startTimeslot, endTimeslot+1); // add 1 since copyOfRange is exlusive
		return new EnergyPriceHistory(energyPricesSubset, this.date);
	}
	

	/**
	 * Returns the date for this energy price history. Can be null in the
	 * case of data generation.
	 * Example: "2017-12-31"
	 * @return
	 */
	public String getDate() {
		return date;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EnergyPriceHistory other = (EnergyPriceHistory) obj;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (!Arrays.equals(energyPrices, other.energyPrices))
			return false;
		return true;
	}

	
}
