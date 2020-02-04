package com.sap.charging.realTime.model.forecasting.soc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import com.sap.charging.model.Car;
import com.sap.charging.model.CarFactory.CarModel;
import com.sap.charging.opt.lp.Variable;
import com.sap.charging.realTime.model.forecasting.Forecast;
import com.sap.charging.util.FileIO;

public abstract class CarSoCForecast extends Forecast {

	public final String[] featureNames;
	
	public CarSoCForecast() {
		String featureNamesRaw = FileIO.readFile("gen/models/socForecastFeatures.csv");
		featureNames = featureNamesRaw.split("\\n");
	}
		
	
	public abstract double getExpectedSoC(Car carPreviousDay, Car car, int lastDay, int currentDay);

	public static CarSoCForecast getDefaultCarSoCForecast() {
		return new CarSoCForecastMedian();
	}

	/**
	 * Not in correct order of R data.table, this.featureNames has to be used for that
	 * @param carPreviousDay
	 * @param car
	 * @param lastDay
	 * @param currentDay
	 * @return
	 */
	protected Variable[] getVariablesArray(Car carPreviousDay, Car car, int lastDay, int currentDay) {
		
		// Reconstruct data.table of R
		int lastWDay = lastDay % 7;
		int currentWDay = currentDay % 7;
		ArrayList<Variable> result = new ArrayList<>();
		Variable[] variables = new Variable[] {
				new Variable("lastKnownArrivalSoC", false, carPreviousDay.getCurrentCapacity() / carPreviousDay.getMaxCapacity()),
				new Variable("lastKnownArrivalAbsSoC", false, carPreviousDay.getCurrentCapacity()),
				new Variable("lastKnownDepartureSoC", false, (carPreviousDay.getChargedCapacity()+carPreviousDay.getCurrentCapacity()) / carPreviousDay.getMaxCapacity()),
				new Variable("lastKnownDepartureAbsSoC", false, (carPreviousDay.getChargedCapacity()+carPreviousDay.getCurrentCapacity())),
				
				new Variable("nWorkdaysBetween", true, 1),
				new Variable("nDaysBetween", true, lastDay-currentDay),
				
				new Variable("isLastWDay0", true, (lastWDay==0) ? 1 : 0),
				new Variable("isLastWDay1", true, (lastWDay==1) ? 1 : 0),
				new Variable("isLastWDay2", true, (lastWDay==2) ? 1 : 0),
				new Variable("isLastWDay3", true, (lastWDay==3) ? 1 : 0),
				new Variable("isLastWDay4", true, (lastWDay==4) ? 1 : 0),
				
				new Variable("isCurrentWDay0", true, (currentWDay==0) ? 1 : 0),
				new Variable("isCurrentWDay1", true, (currentWDay==1) ? 1 : 0),
				new Variable("isCurrentWDay2", true, (currentWDay==2) ? 1 : 0),
				new Variable("isCurrentWDay3", true, (currentWDay==3) ? 1 : 0),
				new Variable("isCurrentWDay4", true, (currentWDay==4) ? 1 : 0),
				
				new Variable("isCarTypeBEV", true, (car.isBEV()) ? 1 : 0),
				new Variable("isCarTypePHEV", true, (car.isPHEV()) ? 1 : 0),
				
				new Variable("carMaxCapacity", false, car.getMaxCapacity())
		};
		
		for (Variable v : variables)
			result.add(v);
		
		// Car model names need some preprocessing
		for (CarModel carModel : CarModel.values()) {
			String carModelName = carModel.modelName.replaceAll(" ", "_");
			int value = (car.getModelName().replaceAll(" ", "_").equals(carModelName)) ? 1 : 0;
			result.add(new Variable("isCarModelName" + carModelName, true, value));
		}
		
		return result.toArray(new Variable[result.size()]);
	}
	
	protected Variable getVariableByName(Variable[] variables, String name) {
		Optional<Variable> o = Arrays.stream(variables).filter(v -> v.getName().equals(name)).findAny();
		if (o.isPresent() == false) {
			throw new RuntimeException("Could not find a variable in array for name=" + name);
		}
		
		return o.get();
	}
	
}





