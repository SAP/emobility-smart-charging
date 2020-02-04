package com.sap.charging.util.performanceMeasurement.paperJournal2019;

import org.joda.time.DateTime;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorRandomProcesses;
import com.sap.charging.model.CarProcessData;
import com.sap.charging.realTime.Strategy;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.StrategyGreedy;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureForecastLinearModel;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureForecastLinearModelAll;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureForecastMedianTimestamp;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureForecastNN_CarSample;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureForecastXGBoost_CarSample;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureOracle;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.performanceMeasurement.MeasurementExecutor;
import com.sap.charging.util.random.ConstantDistribution;
import com.sap.charging.util.random.Distribution;
import com.sap.charging.util.sqlite.SQLiteDB;

public class MeasurementExecutorPaperJournal2019 extends MeasurementExecutor<PerformanceMeasurementPaperJournal2019> {

	@Override
	public int getVerbosity() {
		return AppComparisonPerformancePaperJournal2019.verbosity;
	}
	
	public MeasurementExecutorPaperJournal2019(SQLiteDB db) {
		super(db);
	}
	public MeasurementExecutorPaperJournal2019(SQLiteDB db, boolean forceMeasurement) {
		super(db, forceMeasurement);
	}
	
	@Override
	public void accept(PerformanceMeasurementPaperJournal2019 measurementTemp) {
		
		AppComparisonPerformancePaperJournal2019.currentSimulationIndex.incrementAndGet();
		final PerformanceMeasurementPaperJournal2019 measurement = measurementTemp.cloneWithMethod(measurementTemp.method);
		
		if (forceMeasurement == true && db.rowExists(measurement) == true) {
			db.deleteRow(measurement);
		}
		if (db.rowExists(measurement)) {
			System.out.println(measurement.toString() + " already exists.");
			return;
		}
		
		log(0, "Running (tID=" + Thread.currentThread().getId() + ", " + AppComparisonPerformancePaperJournal2019.getSimulationIndexString() + ") measurement=" + measurement);
		
		DataGeneratorRandomProcesses data = null;
		if (measurementTemp.getCarProcessData() != null) {
			data = new DataGeneratorRandomProcesses(measurement.seed, measurementTemp.getCarProcessData());
		}
		else {
			data = new DataGeneratorRandomProcesses(measurement.seed, CarProcessData.sampleHistoricalCarProcesses(measurement.nCars, measurement.seed));
		}
		
		data.setIdealCars(true);
		data.setIdealChargingStations(true);
		
		//Distribution distribution = new NormalDistribution(data.getRandom(), 0.2, 0.2/3);
		Distribution distribution = new ConstantDistribution(data.getRandom(), 0.2);
		data.setCurCapacityDistribution(distribution);
		
		data.generateEnergyPriceHistory(96)
			.generateCars(measurement.nCars)
			.generateChargingStations(measurement.nCars)
			.generateFuseTree(measurement.nCars, true);
		
		double[] energyPrices = data.getEnergyPriceHistory().getPrices();
		for (int i=0;i<energyPrices.length;i++) 
			energyPrices[i] = i;
		
		Strategy strategy = null;
		if (measurement.forecastingMethod.equals("uncoordinated")) {
			strategy = new StrategyGreedy();
		}
		else {
			strategy = getStrategyAlgorithmic(measurement.forecastingMethod);
		}
		executeMeasurement(measurement, data, strategy);
	}
	
	
	private void executeMeasurement(PerformanceMeasurementPaperJournal2019 measurement, DataGenerator dataRealtime, Strategy strategy) {
		Simulation sim = new Simulation(dataRealtime, strategy);
		sim.init();
		sim.simulate();
		
		FileIO.writeFile(measurement.filePath, sim.getSimulationResult().getSolvedProblemInstanceJSON());
		measurement.dateTimeEnd = DateTime.now().toString();
		db.insert(measurement);
	}
	
	private StrategyAlgorithmic getStrategyAlgorithmic(String forecastingMethod) {
		StrategyAlgorithmic strategy = null;
		
		if (forecastingMethod.equals("oracle")) {
			strategy = new StrategyAlgorithmic(new CarDepartureOracle());
		}
		else if (forecastingMethod.contains("medianTimestamp")) {
			int medianTimestamp = Integer.parseInt(forecastingMethod.substring(15, forecastingMethod.length()));
			strategy = new StrategyAlgorithmic(new CarDepartureForecastMedianTimestamp(medianTimestamp));
			return strategy;
		}
		else if (forecastingMethod.equals("forecastLM")) {
			// Derived from lm() in R on 50.000+ charging processes
			strategy = new StrategyAlgorithmic(new CarDepartureForecastLinearModel(50639.3640, 0.2634));
		}
		else if (forecastingMethod.equals("forecastLMAll")) {
			// Derived from lm() in R on 50.000+ charging processes
			strategy = new StrategyAlgorithmic(new CarDepartureForecastLinearModelAll("gen/models/departureForecastLMAll.bin"));
		}
		else if (forecastingMethod.equals("forecastXGBoost")) {
			strategy = new StrategyAlgorithmic(new CarDepartureForecastXGBoost_CarSample("gen/models/departureForecastXGBoost.bin"));
		}
		else if (forecastingMethod.equals("forecastNN")) {
			strategy = new StrategyAlgorithmic(new CarDepartureForecastNN_CarSample("gen/models/departureForecastNN_Keras.hdf5"));
		}
		return strategy;
	}


}
