package com.sap.charging.realTime.model.forecasting.departure;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;

import com.sap.charging.model.Car;
import com.sap.charging.realTime.State;
import com.sap.charging.util.r.RConnector;

public class CarDepartureForecastNN_CarSample extends CarDepartureForecast {

	private RConnector rConnector;
	
	public CarDepartureForecastNN_CarSample(String modelFilePath) {
		rConnector = new RConnector();
		String rString = "library(keras)\n" + 
				"modelNN <- load_model_hdf5('" + modelFilePath + "')\n";
		rString = rConnector.addRTryCtach(rString);
		rConnector.evalRString(rString);
		log(3, "Executed rString:");
		log(3, rString);
		
		log(1, "NN model loaded with path=" + modelFilePath);
		
	}
	
	@Override
	public int getExpectedDepartureTimeSeconds(State state, Car car) {
		String matrixString = getRMatrixString(car);
		
		String rString = rConnector.addRTryCtach("\tpredict(modelNN, " + matrixString + ")[1,1]\n");
		
		log(2, "Executing r String: ");
		log(2, rString);
		REXP result = rConnector.evalRString(rString);
		if (result.isNumeric()) {
			try {
				int resultTimestamp = (int) result.asDouble();
				if (resultTimestamp <= 0) {
					resultTimestamp = 1;
				}
				return resultTimestamp;
			} catch (REXPMismatchException e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println(result.toDebugString());
			throw new RuntimeException("Error executing NN prediction");
		}
		return -1;
	}
	
	
	
	
	@Override
	@Deprecated
	public int getExpectedDepartureTimeslot(State state, Car car) {
		return -1;
	}
	
}
