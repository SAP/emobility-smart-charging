package com.sap.charging.realTime.model.forecasting.soc;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.sap.charging.model.Car;
import com.sap.charging.opt.lp.Variable;

public class CarSoCForecastXGBoost extends CarSoCForecast {

	private RConnection rConnection;
	
	
	public CarSoCForecastXGBoost(String modelFilePath) {
		super();
		String rString = "tryCatch({\n"
				+ "setwd('" + getJavaWD() + "')\n"
				+ "if(!require(xgboost)) install.packages('xgboost') \n"
				+ "library(xgboost)\n"
				+ "model <- xgb.load('" + modelFilePath + "')\n"
				+ "}, error=function(e) {\n"
				+ "as.character(e) \n"
				+ "})";
		evalRString(rString);
		log(1, "XGBoost model loaded.");
	}
	
	
	@Override
	public double getExpectedSoC(Car carPreviousDay, Car car, int lastDay, int currentDay) {
		Variable[] variables = getVariablesArray(carPreviousDay, car, lastDay, currentDay);
		String valueString = "";
		for (int i=0;i<this.featureNames.length;i++) {
			String featureName = this.featureNames[i];
			Variable v = getVariableByName(variables, featureName);
			valueString += v.getValue();
			if (i != this.featureNames.length-1) { // Append comma if not last element
				valueString += ", ";
			}
		}
		
		
		// this.featureNames gives ordered list of features that go into matrix for xgboost
		String rString = "tryCatch({\n" 
				+ "\tpredict(model, matrix(c(" + valueString + "),nrow=1))\n"
				+ "}, error=function(e) {\n"
				+ "\tas.character(e) \n" 
				+ "})";
		REXP result = evalRString(rString);
		//System.out.println(rString);
		if (result.isNumeric()) {
			try {
				//System.out.println("PREDICTED=" + result.asDouble() + ", ACTUAL=" + (car.getCurrentCapacity() / car.maxCapacity) + ", valueString=" + valueString);
				return result.asDouble();
			} catch (REXPMismatchException e) {
				e.printStackTrace();
			}
		}
		else if (result.isString()) {
			try {
				throw new RuntimeException("Result was not numeric, probably error: " + result.asString());
			} catch (REXPMismatchException e) {
				e.printStackTrace();
			}
		}
		return -1;
	}
	
	
	
	
	private RConnection getRConnection() {
		if (rConnection == null) {
			try {
				rConnection = new RConnection();
			} catch (RserveException e) {
				e.printStackTrace();
			}
		}
		return rConnection;
	}
	
	private String getJavaWD() {
		String result = System.getProperty("user.dir");
		return result;
	}
	
	private REXP evalRString(String rString) {
		try {
			REXP rexp = getRConnection().eval(rString);
			return rexp;
		} catch (RserveException e) {
			System.out.println("Executed R string:");
			System.out.println(rString);
			e.printStackTrace();
		}
		return null;
	}
	
}
