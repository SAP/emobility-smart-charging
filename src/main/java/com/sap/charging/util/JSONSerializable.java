package com.sap.charging.util;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public interface JSONSerializable {
	
	public JSONObject toJSONObject();
	
	static int getJSONAttributeAsInt(Object value) {
		if (value instanceof Integer) {
			return (int) value;
		}
		else if (value instanceof Long) {
			return Math.toIntExact((long) value);
		}
		throw new RuntimeException("Weird value: " + value);
	}
	
	static double getJSONAttributeAsDouble(Object value) {
		if (value instanceof Long) {
			return ((Long) value).doubleValue();
		}
		else if (value instanceof Double) {
			return (Double) value;
		}
		throw new RuntimeException("Weird value: " + value);
	}
	
	static double[] getJSONAttributeAsDoubleArray(Object value) {
		double[] result;
		if (value instanceof JSONArray) {
			JSONArray array = (JSONArray) value;
			result = new double[array.size()];
			for (int i=0;i<array.size();i++) {
				result[i] = getJSONAttributeAsDouble(array.get(i));
			}
		}
		else if (value instanceof Double[] || value instanceof double[]) {
			result = (double[]) value;
		}
		else if (value == null) {
			throw new RuntimeException("value is null!");
		}
		else {
			throw new RuntimeException("Weird value: " + value + ", class=" + value.getClass());
		}
		return result;
	}
	  
	
}
