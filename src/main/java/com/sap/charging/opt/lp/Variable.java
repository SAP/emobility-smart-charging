package com.sap.charging.opt.lp;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import com.sap.charging.util.JSONKeys;
import com.sap.charging.util.JSONSerializable;

public class Variable extends Indexable implements JSONSerializable{

	public final boolean isInteger;
	
	private double coefficient = Double.MIN_VALUE;
	
	/**
	 * A variable may be either a decision variable, or set by another algorithm in advance. 
	 */
	private double value = Double.NEGATIVE_INFINITY;
	
	public boolean hasValue() {
		return !Double.isInfinite(value);
	}
	public void setValue(double value) {
		this.value = value;
		//if (this.hasValue())
		//	System.out.println("Variable::setValue Setting value to " + value);
	}
	public double getValue() {
		return value;
	}
	
	public Variable(String name, boolean isInteger) {
		super(name);
		this.isInteger = isInteger;
	}
	
	public Variable(String name, boolean isInteger, double value) {
		super(name);
		this.isInteger = isInteger;
		this.setValue(value);
	}
	
	private Variable(String name, boolean isInteger, double value, HashMap<String,Integer> otherIndices) {
		super(name);
		this.isInteger = isInteger;
		this.setValue(value);
		for (String key : otherIndices.keySet()) {
			this.setIndex(key, otherIndices.get(key));
		}
	}
	
	@Override
	public Variable clone() {
		return new Variable(this.getName(), this.isInteger, this.value, this.indices);
	}
	
	public Variable setCoefficient(double value) {
		this.coefficient = value;
		return this;
	}
	
	public double getCoefficient() {
		return this.coefficient;
	}
	
	@Override
	public String toString() {
		String result = coefficient >= 0 ? "+" : "";
		result += coefficient + "*" + this.getNameWithIndices();
		return result;
	}
	
	
	/**
	 * Constructs JSON object from variable. Sample output:
	 * {
			"variableName": "P_i0_j1_k12",
			"variableValue": 32.0,
			"variableObjCoefficient": 0.5 [depending on whether it is set]
			"isInteger": false
		}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJSONObject() {
		JSONObject result = new JSONObject();
		result.put(JSONKeys.JSON_KEY_VARIABLE_NAME, getNameWithIndices());
		result.put(JSONKeys.JSON_KEY_VARIABLE_VALUE, getValue());
		
		if (getCoefficient() > Double.MIN_VALUE) {
			result.put(JSONKeys.JSON_KEY_VARIABLE_OBJECTIVE_COEFFICIENT, getCoefficient());
			
		}
		
		result.put(JSONKeys.JSON_KEY_VARIABLE_IS_INTEGER, isInteger);
		return result;
	}
	
	/**
	 * Constructs variable from JSON object. Sample input:
	 * {
			"variableName": "P_i0_j1_k12",
			"variableValue": 32.0
			
		}
		WARNING: isInteger may not be imported since it may not be exported.
	 * @param object
	 * @return
	 */
	public static Variable fromJSON(JSONObject object) {
		// variableName
		String variableNameWithIndices = (String) object.get("variableName");
		String[] parts = variableNameWithIndices.split("_");
		
		// isInteger
		boolean isInteger = false;
		if (object.get("isInteger") != null) {
			isInteger = (boolean) object.get("isInteger");
		}
		
		// Construct variable
		Variable result = new Variable(parts[0], isInteger);
		
		// variableValue
		if (object.get("variableValue") != null) {
			double value = Double.parseDouble(object.get("variableValue").toString() + "");
			result.setValue(value);
		}
		
		// variable object coefficient
		if (object.get("variableObjCoefficient") != null) {
			double coefficient = Double.parseDouble(object.get("variableObjCoefficient").toString() + "");
			result.setCoefficient(coefficient);
		}
		
		// set indices
		for (int i=1;i<parts.length;i++) {
			Matcher matcher = Pattern.compile("\\d+").matcher(parts[i]);
			matcher.find();
			int index = parts[i].indexOf(matcher.group());
			String indexName = parts[i].substring(0, index);
			int indexValue = Integer.parseInt(parts[i].substring(index));
			result.setIndex(indexName, indexValue);
		}
		return result;
	}
	
	
}







