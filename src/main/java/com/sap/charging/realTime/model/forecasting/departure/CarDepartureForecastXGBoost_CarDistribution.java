package com.sap.charging.realTime.model.forecasting.departure;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;

import com.sap.charging.model.Car;
import com.sap.charging.model.CarProcessData;
import com.sap.charging.realTime.State;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.TimeUtil;
import com.sap.charging.util.r.RConnector;



public class CarDepartureForecastXGBoost_CarDistribution extends CarDepartureForecast {
	
	private RConnector rConnector;
	public final String[] featureNames;
	
	public CarDepartureForecastXGBoost_CarDistribution(String modelFilePath) {
		rConnector = new RConnector();
		String rString = "model <- xgboost::xgb.load('" + modelFilePath + "')\n";
		rString = rConnector.addRTryCtach(rString);
		rConnector.evalRString(rString);
		log(3, "Executed rString:");
		log(3, rString);
		
		log(1, "XGBoost model loaded.");
		
		String featureNamesRaw = FileIO.readFile("gen/models/departureForecastFeatures.csv");
		featureNames = featureNamesRaw.split("\\n");
	}
	
	@Deprecated
	@Override
	public int getExpectedDepartureTimeslot(State state, Car car) {
		String rString = "tryCatch({\n" 
				+ "\tpredict(model, matrix(" + state.currentTimeSeconds + "))\n"
				+ "}, error=function(e) {\n"
				+ "\tas.character(e) \n" 
				+ "})";
		REXP result = rConnector.evalRString(rString);
		if (result.isNumeric()) {
			try {
				int resultTimeslot = TimeUtil.getTimeslotFromSeconds((int) result.asDouble());
				return Math.min(resultTimeslot+1, 95);
			} catch (REXPMismatchException e) {
				e.printStackTrace();
			}
		}
		return -1;
	}
	
	private String getMatrixString(CarProcessData data) {
		String matrixString = "matrix(c(";
		
		// Create one-hot encoded matrix that was used to train xgboost
		int count=0;
		for (int i=0;i<featureNames.length;i++) {
			String featureName = featureNames[i];
			if (featureName.startsWith("carPark")) {
				// Example: carPark_WDF40
				if (featureName.contains(data.carPark)) {
					matrixString += "1";
				}
				else {
					matrixString += "0";
				}
				count++;
			}
			else if (featureName.startsWith("wday")) {
				// Example: wday_1
				if (featureName.contains(data.wday + "")) {
					matrixString += "1";
				}
				else {
					matrixString += "0";
				}
				count++;
			}
			else if (featureName.startsWith("carType")) {
				// Example: carType_B
				if (featureName.contains(data.carType)) {
					matrixString += "1";
				}
				else {
					matrixString += "0";
				}
				count++;
			}
			else if (featureName.startsWith("timestampArrival")) {
				matrixString += data.timestampArrival + "";
				count++;
			}
			
			if (i != featureNames.length-1)
				matrixString += ",";
		}
		if (count != featureNames.length)
			throw new RuntimeException("Error: Matrix is shorter than it should be!");
		
		matrixString += "), nrow=1)";
		
		log(2, "matrixString:");
		log(2, matrixString);
		return matrixString;
	}
	
	@Override
	public int getExpectedDepartureTimeSeconds(State state, Car car) {
		CarProcessData data = car.getCarProcessData();
		String matrixString = getMatrixString(data);  
		
		String rString = rConnector.addRTryCtach("\tpredict(model, " + matrixString + ")\n");
		
		log(2, "Executing r String: ");
		log(2, rString);
		REXP result = rConnector.evalRString(rString);
		if (result.isNumeric()) {
			try {
				int resultTimestamp = (int) result.asDouble();
				return resultTimestamp;
			} catch (REXPMismatchException e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println(result.toDebugString());
			throw new RuntimeException("Error executing XGBoost prediction");
		}
		return -1;
	}
	
	
	
}
