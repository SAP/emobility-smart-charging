package com.sap.charging.util.performanceMeasurement.paperJournal2020NonlinearCharging;

import org.joda.time.DateTime;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.model.CarFactory.CarModel;
import com.sap.charging.model.battery.BatteryData_Sample;
import com.sap.charging.realTime.Strategy;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.StrategyGreedy;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureOracle;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.performanceMeasurement.MeasurementExecutor;
import com.sap.charging.util.random.ConstantDistribution;
import com.sap.charging.util.sqlite.SQLiteDB;

public class MeasurementExecutorPaperJournal2020NonlinearCharging extends MeasurementExecutor<PerformanceMeasurementPaperJournal2020NonlinearCharging>  {

	
	@Override
	public int getVerbosity() {
		return AppComparisonPerformancePaperJournal2020NonlinearCharging.verbosity;
	}
	
	public MeasurementExecutorPaperJournal2020NonlinearCharging(SQLiteDB db) {
		super(db);
	}
	public MeasurementExecutorPaperJournal2020NonlinearCharging(SQLiteDB db, boolean forceMeasurement) {
		super(db, forceMeasurement);
	}
	
	@Override
	public void accept(PerformanceMeasurementPaperJournal2020NonlinearCharging measurementTemp) {
		
		AppComparisonPerformancePaperJournal2020NonlinearCharging.currentSimulationIndex.incrementAndGet();
		final PerformanceMeasurementPaperJournal2020NonlinearCharging measurement = measurementTemp.cloneWithMethod(measurementTemp.method);
		
		if (forceMeasurement == true && db.rowExists(measurement) == true) {
			db.deleteRow(measurement);
		}
		if (db.rowExists(measurement)) {
			System.out.println(measurement.toString() + " already exists.");
			return;
		}
		
		log(0, "Running (tID=" + Thread.currentThread().getId() + ", " + AppComparisonPerformancePaperJournal2020NonlinearCharging.getSimulationIndexString() + ") measurement=" + measurement);
		
		DataGeneratorRandom data = new DataGeneratorRandom(measurement.seed, false);
		
		data.setIdealCars(true);
		data.setIdealChargingStations(true);
		data.setCarModels(new CarModel[] {CarModel.RENAULT_ZOE_ZE40});
		data.setCurCapacityDistribution(new ConstantDistribution(null, measurement.startSoC));
		data.setNonlinearCharging(true);
		
		data.setBatteryData(new BatteryData_Sample() {
			@Override
			public double getResistanceFromSOC(double soc) {
				return 0.003;
			}
			
		});
		
		data.generateEnergyPriceHistory(96)
			.generateCars(measurement.nCars)
			.generateChargingStations(measurement.nCars)
			.generateSimpleFuseTree();
			//.generateFuseTree(measurement.nCars, true);
		
		double[] energyPrices = data.getEnergyPriceHistory().getPrices();
		for (int i=0;i<energyPrices.length;i++) 
			energyPrices[i] = i;
		
		Strategy strategy = getStrategy(measurement);
		executeMeasurement(measurement, data, strategy);
	}
	
	private Strategy getStrategy(PerformanceMeasurementPaperJournal2020NonlinearCharging measurement) {
		if (measurement.method.equals("uncoordinated")) {
			return new StrategyGreedy();
		}
		else {
			return getStrategyAlgorithmic(measurement);
		}
	}
	private StrategyAlgorithmic getStrategyAlgorithmic(PerformanceMeasurementPaperJournal2020NonlinearCharging measurement) {
		StrategyAlgorithmic strategy = new StrategyAlgorithmic(new CarDepartureOracle());
		strategy.setReoptimizeOnStillAvailableAfterExpectedDepartureTimeslot(false);
		
		if (measurement.method.contains("Nonlinear")) 
			strategy.setRecognizeNonlinearCharging(true);
		else 
			strategy.setRecognizeNonlinearCharging(false);
		
		strategy.setRescheduleCarsWith0A(measurement.rescheduleCarsWith0A);
		strategy.getScheduler().setEnablePlannedCapacityCache(false);
		return strategy;
	}
		
	
	
	private void executeMeasurement(PerformanceMeasurementPaperJournal2020NonlinearCharging measurement, DataGenerator dataRealtime, Strategy strategy) {
		Simulation sim = new Simulation(dataRealtime, strategy);
		sim.setEnableCSVStorage(true);
		//sim.setIntervalReoptimizationEvents(1);
		sim.init();
		
		try {
			sim.simulate();
			
			FileIO.writeFile(measurement.filePath, sim.getSimulationResult().getSolvedProblemInstanceJSON());
			measurement.dateTimeEnd = DateTime.now().toString();
			db.insert(measurement);
		}
		catch (Exception e) {
			System.out.println("Error during simulation of following measurement: ");
			System.out.println(measurement.toString());
			e.printStackTrace();
		}
	}
	

	
}
