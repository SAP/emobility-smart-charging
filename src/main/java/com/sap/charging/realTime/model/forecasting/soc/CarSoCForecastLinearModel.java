package com.sap.charging.realTime.model.forecasting.soc;

import java.util.Arrays;
import java.util.HashMap;

import com.sap.charging.model.Car;
import com.sap.charging.opt.lp.Variable;
import com.sap.charging.util.FileIO;

public class CarSoCForecastLinearModel extends CarSoCForecast {

	public final HashMap<String, Double> featureCoefficients;
	
	
	public CarSoCForecastLinearModel(String modelFilePath) {
		super(); 
		featureCoefficients = new HashMap<>();
		String featureCoefficientsRaw = FileIO.readFile(modelFilePath);
		String[] lines = featureCoefficientsRaw.split("\\n");
		for (String line : lines) {
			String[] keyValue = line.split(",");
			String key = keyValue[0].replaceAll("`", "");
			double value = (keyValue[1].equals("NA")) ? 0 : Double.parseDouble(keyValue[1]);
			featureCoefficients.put(key, value);
		}
		
	}
	
	@Override
	public double getExpectedSoC(Car carPreviousDay, Car car, int lastDay, int currentDay) {
		Variable[] variables = getVariablesArray(carPreviousDay, car, lastDay, currentDay);
		if (variables.length != featureCoefficients.size()-1) { // Intercept doesn't count
			System.out.println("Variables: " + Arrays.toString(variables));
			System.out.println("featureCoefficients: "); 
			System.out.println(featureCoefficients);
			throw new RuntimeException("Variables length (" + variables.length + ") != featureCoefficients.length (" + featureCoefficients.size() + ")");
		}
		
		double result = featureCoefficients.get("(Intercept)");
		for (Variable v : variables) {
			result += featureCoefficients.get(v.getName())  * v.getValue();
		}
		
		return result;
	}

	
}









