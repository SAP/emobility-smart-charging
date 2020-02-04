package com.sap.charging.util.performanceMeasurement.paperJournal2018SensitivityAnalysis;

import java.util.ArrayList;

import org.json.simple.JSONObject;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.opt.heuristics.InstanceHeuristicRelSoCLP;
import com.sap.charging.opt.lp.Equation;
import com.sap.charging.opt.solution.model.DayaheadSchedule;
import com.sap.charging.realTime.Strategy;
import com.sap.charging.realTime.StrategyFromDayahead;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.performanceMeasurement.MeasurementExecutor;
import com.sap.charging.util.random.Distribution;
import com.sap.charging.util.random.NormalDistribution;
import com.sap.charging.util.sqlite.SQLiteDB;

public class MeasurementExecutorPaperJournal2018SensitivityAnalysis extends MeasurementExecutor<PerformanceMeasurementPaperJournal2018SensitivityAnalysis>{

	private ArrayList<Equation> allRestrictions;
	
	@Override
	public int getVerbosity() {
		return AppComparisonPerformancePaperJournal2018SensitivityAnalysis.verbosity;
	}
	
	public MeasurementExecutorPaperJournal2018SensitivityAnalysis(SQLiteDB db) {
		super(db);
	}
	public MeasurementExecutorPaperJournal2018SensitivityAnalysis(SQLiteDB db, boolean forceMeasurement) {
		super(db, forceMeasurement);
	}
	public MeasurementExecutorPaperJournal2018SensitivityAnalysis(SQLiteDB db, boolean forceMeasurement, ArrayList<Equation> allRestrictions) {
		super(db, forceMeasurement);
		this.allRestrictions = allRestrictions;
	}
	
	
	@Override
	public void accept(PerformanceMeasurementPaperJournal2018SensitivityAnalysis measurementTemp) {
		// TODO Auto-generated method stub
		
		if (forceMeasurement == true && db.rowExists(measurementTemp) == true) {
			db.deleteRow(measurementTemp);
		}
		if (db.rowExists(measurementTemp)) {
			System.out.println(measurementTemp.toString() + " already exists.");
			return;
		}
		
		PerformanceMeasurementPaperJournal2018SensitivityAnalysis measurement = measurementTemp.cloneWithMethod(measurementTemp.methodDayahead + "+" + measurementTemp.methodRealtime);
		log(0, "Running " + measurement, false, true);
		
		DataGenerator data = getDataDayhead(measurement);
		
		InstanceHeuristicRelSoCLP instance = new InstanceHeuristicRelSoCLP(data);
		instance.getInstanceLP().setNormalizingCoefficients(true);
		instance.getInstanceLP().objectiveFairShare.setWeight(measurement.weightFairShare);
		instance.getInstanceLP().objectiveEnergyCosts.setWeight(measurement.weightCosts);
		instance.getInstanceLP().objectivePeakShaving.setWeight(measurement.weightPeakShaving);
		instance.getInstanceLP().objectiveLoadImbalance.setWeight(measurement.weightLoadImbalance);
		
		instance.constructProblem(allRestrictions);
		
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (AppComparisonPerformancePaperJournal2018SensitivityAnalysis.currentSCIPThreads.get() >= AppComparisonPerformancePaperJournal2018SensitivityAnalysis.maxSCIPThreads) {
					try {
						//System.out.println("Waiting for SCIP threads to be available...");
						Thread.sleep(1000);
					} catch (InterruptedException e) { e.printStackTrace(); }
				}
				
				AppComparisonPerformancePaperJournal2018SensitivityAnalysis.currentSCIPThreads.incrementAndGet();
				
				System.out.println("Running final step in other thread...");
				
				JSONObject solvedProblemInstance = instance.getSolvedProblemInstanceJSON();
				
				double timeProblemConstructionDayahead = instance.timeProblemConstruction.getTime();
				double timeSolutionDayahead = instance.timeSolution.getTime();
				
				DayaheadSchedule schedule = new DayaheadSchedule(solvedProblemInstance);
				Strategy strategy = new StrategyFromDayahead(schedule);
				
				Simulation sim = new Simulation(data, strategy);
				sim.init();
				sim.simulate();
				
				sim.getSimulationResult().timeProblemConstruction.addTime(timeProblemConstructionDayahead);
				sim.getSimulationResult().timeSolution.addTime(timeSolutionDayahead);
				
				FileIO.writeFile(measurement.filePath, sim.getSimulationResult().getSolvedProblemInstanceJSON());
				db.insert(measurement);
				
				AppComparisonPerformancePaperJournal2018SensitivityAnalysis.currentSCIPThreads.decrementAndGet();
			}
		});
		
		
		thread.start(); // run is synchronous, start starts a new thread
		
	}

	public static DataGeneratorRandom getDataDayhead(PerformanceMeasurementPaperJournal2018SensitivityAnalysis measurement) {
		DataGeneratorRandom data = new DataGeneratorRandom(measurement.seed, false);
		
		data.setIdealCars(true);
		data.setIdealChargingStations(true);
		
		Distribution normalDistribution = new NormalDistribution(data.getRandom(), 0.2, 0.2/3);
		data.setCurCapacityDistribution(normalDistribution);
		
		data.generateEnergyPriceHistory(96)
			.generateCars(measurement.nCars)
			.generateChargingStations(measurement.nChargingStations)
			.generateFuseTree(measurement.nCars, true);
		
		return data;
	}
	
}













