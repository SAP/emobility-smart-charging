package com.sap.charging.realTime.model.forecasting.departure;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;

import com.sap.charging.model.Car;
import com.sap.charging.model.CarProcessData;
import com.sap.charging.realTime.State;
import com.sap.charging.util.r.RConnector;

public class CarDepartureForecastLinearModelAll extends CarDepartureForecast {
	
	private RConnector rConnector;
	
	public CarDepartureForecastLinearModelAll(String modelFilePath) {
		rConnector = new RConnector();
		String rString = "load('" + modelFilePath + "')\n";
		rString = rConnector.addRTryCtach(rString);
		rConnector.evalRString(rString);
		log(3, "Executed rString:");
		log(3, rString);
		
		log(1, "LM all model loaded with path=" + modelFilePath);
		
	}
	
	@Override
	@Deprecated
	public int getExpectedDepartureTimeslot(State state, Car car) {
		return -1;
	}

	@Override
	public int getExpectedDepartureTimeSeconds(State state, Car car) {
		CarProcessData data = car.getCarProcessData();
		String matrixString = "data.table::" + data.oneHotEncodedRaw + "";
		String rString = rConnector.addRTryCtach("\tpredict(modelLMAll, " + matrixString + ")\n");
		
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
			throw new RuntimeException("Error executing LM All prediction");
		}
		return -1;
	}
	
	
}
