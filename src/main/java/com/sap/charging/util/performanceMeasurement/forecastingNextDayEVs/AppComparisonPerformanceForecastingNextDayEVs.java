package com.sap.charging.util.performanceMeasurement.forecastingNextDayEVs;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.realTime.model.forecasting.soc.CarSoCForecastLinearModel;
import com.sap.charging.realTime.model.forecasting.soc.CarSoCForecastXGBoost;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.configuration.Options;
import com.sap.charging.util.sqlite.SQLiteDB;

public class AppComparisonPerformanceForecastingNextDayEVs {

	public static void main(String[] args) {
		
		Simulation.verbosity = 0;
		
		int nThreads = 30;
		System.out.println("AppComparisonPerformanceForecastingNextDayEVs::main Init with nThreads=" + nThreads + "...");
		Options.set(24, 15, 0.85);
		
		CONSTANTS.FUSE_LEVEL_0_SIZE = 999999;
		CONSTANTS.FUSE_LEVEL_1_SIZE = 999999;
		CONSTANTS.FUSE_LEVEL_2_SIZE = 999999;
		
		int[] nCarsParams = new int[] {100};
		int[] nChargingStationsParams = new int[]{50};
		String[] carAssignmentMethods = new String[]{"forecastLinear", "forecastLinearAbs", "forecastXGBoost", "forecastXGBoostAbs"}; //
		
		int[] daysParams = IntStream.range(0, 1400).toArray();
		int[] seeds = IntStream.range(0, 0+1).toArray();
		
		System.out.println("Running nCarsParams=" + nCarsParams.length + 
							", nChargingStationsParams=" + nChargingStationsParams.length + 
							", carAssignmentMethods=" + carAssignmentMethods.length + 
							", daysParams=" + daysParams.length + 
							", seeds=" + seeds.length);
		
		List<PerformanceMeasurementForecastingNextDayEVs> measurementJobs = new ArrayList<>();
		
		for (int nCars : nCarsParams) {
			for (int nChargingStations : nChargingStationsParams) {
				for (int day : daysParams) {
					for (int seed : seeds) {
						for (String carAssignmentMethod : carAssignmentMethods) {
							PerformanceMeasurementForecastingNextDayEVs job = new PerformanceMeasurementForecastingNextDayEVs(nCars, nChargingStations, carAssignmentMethod, day, seed);
							measurementJobs.add(job);
						}
					}
				}
			}
		}
		
		
		// Stream jobs in parallel
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "" + nThreads);
		SQLiteDB db = new SQLiteDB("jdbc:sqlite:gen/performanceMeasurements.db");
		//measurementJobs.stream().forEach(new MeasurementExecutor(db));
		//measurementJobs.stream().forEach(new MeasurementExecutorINDIN2018(db));
		
		CarSoCForecastLinearModel carSoCForecastLinearModel = new CarSoCForecastLinearModel("gen/models/socForecastLinearModel.csv");
		CarSoCForecastLinearModel carSoCForecastLinearModelAbs = new CarSoCForecastLinearModel("gen/models/socForecastLinearModelAbs.csv");
		
		CarSoCForecastXGBoost carSoCForecastXGBoost = new CarSoCForecastXGBoost("gen/models/socForecastXGBoost.bin");
		CarSoCForecastXGBoost carSoCForecastXGBoostAbs = new CarSoCForecastXGBoost("gen/models/socForecastXGBoostAbs.bin");
		
		measurementJobs.stream().forEach(new MeasurementExecutorForecastingNextDayEVs(db, false, 
				carSoCForecastLinearModel, carSoCForecastLinearModelAbs, carSoCForecastXGBoost, carSoCForecastXGBoostAbs));	
		
	}

}














