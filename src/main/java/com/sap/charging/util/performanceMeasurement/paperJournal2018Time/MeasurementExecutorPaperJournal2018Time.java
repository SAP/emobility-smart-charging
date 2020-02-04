package com.sap.charging.util.performanceMeasurement.paperJournal2018Time;

import org.joda.time.DateTime;
import org.json.simple.JSONObject;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.dataGeneration.DataRandomizer;
import com.sap.charging.opt.heuristics.InstanceHeuristicRelSoCLP;
import com.sap.charging.opt.solution.model.DayaheadSchedule;
import com.sap.charging.realTime.Strategy;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.StrategyFromDayahead;
import com.sap.charging.realTime.StrategyGreedy;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureForecastMedianTimeslot;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.performanceMeasurement.MeasurementExecutor;
import com.sap.charging.util.sqlite.SQLiteDB;

@SuppressWarnings("deprecation")
public class MeasurementExecutorPaperJournal2018Time  extends MeasurementExecutor<PerformanceMeasurementPaperJournal2018Time> {

	@Override
	public int getVerbosity() {
		return AppComparisonPerformancePaperJournal2018Time.verbosity;
	}
	
	public MeasurementExecutorPaperJournal2018Time(SQLiteDB db) {
		super(db);
	}
	public MeasurementExecutorPaperJournal2018Time(SQLiteDB db, boolean forceMeasurement) {
		super(db, forceMeasurement);
	}
	
	@Override
	public void accept(PerformanceMeasurementPaperJournal2018Time measurementTemp) {
		
		final PerformanceMeasurementPaperJournal2018Time measurement = measurementTemp.cloneWithMethod(measurementTemp.methodDayahead + "+" + measurementTemp.methodRealtime);
		
		if (forceMeasurement == true && db.rowExists(measurement) == true) {
			db.deleteRow(measurement);
		}
		if (db.rowExists(measurement)) {
			System.out.println(measurement.toString() + " already exists.");
			return;
		}
		
		log(0, "Running measurement=" + measurement);
		
		DataGeneratorRandom dataDayahead = getDataDayhead(measurement);
		DataGenerator dataRealtime = getDataRealtime(measurement, dataDayahead);
		
		// Check day-ahead planning
		if (measurement.methodDayahead != null) {
			if (measurement.methodDayahead.equals(InstanceHeuristicRelSoCLP.getMethodStatic())) {
				//InstanceHeuristicAbsSoCLP instance = new InstanceHeuristicAbsSoCLP(dataDayahead);
				InstanceHeuristicRelSoCLP instance = new InstanceHeuristicRelSoCLP(dataDayahead);
				instance.solver.useScip600 = true;
				instance.constructProblem();
				
				
				/*log(0, "scheduleDayahead:");
				log(0, scheduleDayahead.toString());
				String powerString = "";
				for (Variable variableP : scheduleDayahead.getVariablesPByTimeslot(43)) {
					log(0, variableP.getNameWithIndices() + "=" + variableP.getValue(), false, true);
				}
				System.out.println();*/
				
				// Start SCIP in another thread! 
				Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						while (AppComparisonPerformancePaperJournal2018Time.currentSCIPThreads.get() >= AppComparisonPerformancePaperJournal2018Time.maxSCIPThreads) {
							try {
								//System.out.println("Waiting for SCIP threads to be available...");
								Thread.sleep(1000);
							} catch (InterruptedException e) { e.printStackTrace(); }
						}
						
						AppComparisonPerformancePaperJournal2018Time.currentSCIPThreads.incrementAndGet();
						
						System.out.println("Running final step in other thread...");
						
						// Weights for energy costs vs peak shaving
						//instance.getInstanceLP().objectiveEnergyCosts.setWeight(0);
						//instance.getInstanceLP().objectivePeakShaving.setWeight(1e4);
						
						JSONObject solvedProblemInstance = instance.getSolvedProblemInstanceJSON();
						
						double timeProblemConstructionDayahead = instance.timeProblemConstruction.getTime();
						double timeSolutionDayahead = instance.timeSolution.getTime();
						
						FileIO.writeFile("vis/data/solution_absSoCLP.json", solvedProblemInstance);
						
						
						DayaheadSchedule scheduleDayahead = new DayaheadSchedule(solvedProblemInstance);
						
						Strategy strategy = null;
						if (measurement.methodRealtime.equals(StrategyFromDayahead.getMethodStatic())) {
							strategy = new StrategyFromDayahead(scheduleDayahead);
						}
						if (measurement.methodRealtime.equals(StrategyAlgorithmic.getMethodWithScheduleStatic())) {
							strategy = new StrategyAlgorithmic(new CarDepartureForecastMedianTimeslot(), scheduleDayahead);
						}
						
						executeMeasurement(measurement, dataRealtime, strategy, timeProblemConstructionDayahead, timeSolutionDayahead);
						
						AppComparisonPerformancePaperJournal2018Time.currentSCIPThreads.decrementAndGet();
					}
				});
				
				//thread.run();
				thread.run(); // run is synchronous, start starts a new thread
			}
		}
		else {
			Strategy strategy = null;
			if (measurement.methodRealtime.equals(StrategyGreedy.getMethodStatic())) {
				strategy = new StrategyGreedy();
			}
			if (measurement.methodRealtime.equals(StrategyAlgorithmic.getMethodWithoutScheduleStatic())) {
				strategy = new StrategyAlgorithmic(new CarDepartureForecastMedianTimeslot());
			}
			executeMeasurement(measurement, dataRealtime, strategy, 0, 0);
		}
		
		
		
			
		/*log(0, "dataDayahead starting SoC:");
		for (Car car : dataDayahead.getCars()) {
			log(0, "n=" + car.getId() + "=" + car.getCurrentCapacity() + ", ", false, false);
		}
		System.out.println();
		log(0, "dataRealtime starting SoC:");
		for (Car car : dataRealtime.getCars()) {
			log(0, "n=" + car.getId() + "=" + car.getCurrentCapacity() + ", ", false, false);
		}
		System.out.println();*/
		
		
		
	}
	
	
	private void executeMeasurement(PerformanceMeasurementPaperJournal2018Time measurement, DataGenerator dataRealtime, Strategy strategy, double timeProblemConstructionDayahead, double timeSolutionDayahead) {
		Simulation sim = new Simulation(dataRealtime, strategy);
		sim.init();
		sim.simulate();
		
		sim.getSimulationResult().timeProblemConstruction.addTime(timeProblemConstructionDayahead);
		sim.getSimulationResult().timeSolution.addTime(timeSolutionDayahead);
		
		FileIO.writeFile(measurement.filePath, sim.getSimulationResult().getSolvedProblemInstanceJSON());
		measurement.dateTimeEnd = DateTime.now().toString();
		db.insert(measurement);
	}
	
	
	private DataGeneratorRandom getDataDayhead(PerformanceMeasurementPaperJournal2018Time measurement) {
		DataGeneratorRandom data = new DataGeneratorRandom(measurement.seed, false);
		
		data.setIdealCars(true);
		data.setIdealChargingStations(true);
		
		//Distribution normalDistribution = new NormalDistribution(data.getRandom(), 0.2, 0.2/3);
		//data.setCurCapacityDistribution(normalDistribution);
		
		data.generateEnergyPriceHistory(96)
			.generateCars(measurement.nCars)
			.generateChargingStations(measurement.nChargingStations)
			.generateFuseTree(measurement.nCars, true);
		
		return data;
	}
	
	private DataGenerator getDataRealtime(PerformanceMeasurementPaperJournal2018Time measurement, DataGenerator dataDayahead) {
		DataRandomizer dataRealtime = new DataRandomizer(dataDayahead, measurement.randomness, measurement.seed);
		dataRealtime.generateAll();
		
		return dataRealtime;
	}
	
	
	

}
