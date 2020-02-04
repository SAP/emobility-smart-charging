package com.sap.charging.ocpp;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONConverter {
	
	private static ObjectMapper mapper = new ObjectMapper();
	private static JSONParser jsonSimpleParser = new JSONParser();
	
	
	public static JSONObject toJSONObject(OCPPData ocppData) {
		try {
			String text = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ocppData);
			JSONObject result = (JSONObject) jsonSimpleParser.parse(text);
			return result;
		} catch (JsonProcessingException | ParseException e) {
			e.printStackTrace();
		}
		return null;		
	}
	
	/*public ChargingProfile toChargingProfile(String input) {
		try {
			return mapper.readValue(input, ChargingProfile.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}*/
	
	
	
}
