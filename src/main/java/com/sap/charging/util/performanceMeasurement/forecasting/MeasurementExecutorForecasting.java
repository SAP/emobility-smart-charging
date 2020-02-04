package com.sap.charging.util.performanceMeasurement.forecasting;

import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.realTime.Strategy;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureForecastLinearModel;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureForecastMedianTimeslot;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureForecastXGBoost_CarDistribution;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureOracle;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.performanceMeasurement.MeasurementExecutor;
import com.sap.charging.util.random.Distribution;
import com.sap.charging.util.random.NormalDistribution;
import com.sap.charging.util.sqlite.SQLiteDB;

@SuppressWarnings("deprecation")
public class MeasurementExecutorForecasting 
	extends MeasurementExecutor<PerformanceMeasurementForecasting> {

	public MeasurementExecutorForecasting(SQLiteDB db) {
		super(db);
	}
	public MeasurementExecutorForecasting(SQLiteDB db, boolean forceMeasurement) {
		super(db, forceMeasurement);
	}
	
	@Override
	public void accept(PerformanceMeasurementForecasting measurement) {

		DataGeneratorRandom data = new DataGeneratorRandom(measurement.seed, false);
		data.setIdealCars(true);
		data.setIdealChargingStations(true);
		
		Distribution normalDistribution = new NormalDistribution(data.getRandom(), 0.2, 0.2/3);
		data.setCurCapacityDistribution(normalDistribution);
		
		data.generateEnergyPriceHistory(96)
			.generateCars(measurement.nCars)
			.generateChargingStations(measurement.nCars)
			.generateFuseTree(measurement.nCars, true);
		
		
		double[] energyPrices = data.getEnergyPriceHistory().getPrices();
		for (int i=0;i<energyPrices.length;i++) 
			energyPrices[i] = 100-i/2;
		
		PerformanceMeasurementForecasting measurementHeuristic = measurement.cloneWithMethod(StrategyAlgorithmic.getMethodWithoutScheduleStatic());
		if (forceMeasurement == true && db.rowExists(measurementHeuristic) == true) {
			db.deleteRow(measurementHeuristic);
		}
		if (db.rowExists(measurementHeuristic) == false) {
			System.out.println("Running measurement with nCars=" + measurement.nCars + ", forecastingMethod=" + measurement.forecastingMethod + ", seed=" + measurement.seed +
					 		   " in thread=" + Thread.currentThread().getId());
			
			Strategy strategy = getStrategyAlgorithmic(measurementHeuristic.forecastingMethod);
			
			Simulation sim = new Simulation(data, strategy);
			sim.init();
			sim.simulate();
			
			db.insert(measurementHeuristic);
			FileIO.writeFile(measurementHeuristic.filePath, sim.getSimulationResult().getSolvedProblemInstanceJSON());
		}
		else {
			System.out.println(measurementHeuristic.toString() + " already exists.");
		}
	}

	public Strategy getStrategyAlgorithmic(String forecastingMethod) {
		Strategy strategy = null;
		if (forecastingMethod.contains("median")) {
			int medianTimeslot = Integer.parseInt(forecastingMethod.substring(6, forecastingMethod.length()));
			strategy = new StrategyAlgorithmic(new CarDepartureForecastMedianTimeslot(medianTimeslot));
			return strategy;
		}
		else if (forecastingMethod.equals("forecastLMOfGenerated")) {
			// Derived from lm() in R on 5000 generated cars (rmse of 3800)
			strategy = new StrategyAlgorithmic(new CarDepartureForecastLinearModel(51196.5446, 0.2502));
		}
		else if (forecastingMethod.equals("forecastLMOfReal")) {
			// Derived from lm() in R on 24000 historical processes (rmse of 6466.36 on real data)
			strategy = new StrategyAlgorithmic(new CarDepartureForecastLinearModel(51510.9274, 0.2422));
		}
		else if (forecastingMethod.equals("forecastXGBoost")) {
			strategy = new StrategyAlgorithmic(new CarDepartureForecastXGBoost_CarDistribution("gen/models/xgboost.bin"));
		}
		else if (forecastingMethod.equals("oracle")) {
			strategy = new StrategyAlgorithmic(new CarDepartureOracle());
		}
		
		return strategy;
	}

	
	
}
