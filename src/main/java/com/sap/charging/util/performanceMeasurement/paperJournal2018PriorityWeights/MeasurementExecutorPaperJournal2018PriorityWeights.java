package com.sap.charging.util.performanceMeasurement.paperJournal2018PriorityWeights;

import java.util.ArrayList;

import org.json.simple.JSONObject;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.opt.heuristics.InstanceHeuristicAbsSoCLP;
import com.sap.charging.opt.heuristics.InstanceHeuristicLP;
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

public class MeasurementExecutorPaperJournal2018PriorityWeights extends MeasurementExecutor<PerformanceMeasurementPaperJournal2018PriorityWeights>{

	@Override
	public int getVerbosity() {
		return AppComparisonPerformancePaperJournal2018PriorityWeights.verbosity;
	}
	
	public MeasurementExecutorPaperJournal2018PriorityWeights(SQLiteDB db) {
		super(db);
	}
	public MeasurementExecutorPaperJournal2018PriorityWeights(SQLiteDB db, boolean forceMeasurement) {
		super(db, forceMeasurement);
	}

	private ArrayList<Equation> allRestrictions;
	
	public MeasurementExecutorPaperJournal2018PriorityWeights(SQLiteDB db, boolean forceMeasurement, ArrayList<Equation> allRestrictions) {
		super(db, forceMeasurement);
		this.allRestrictions = allRestrictions;
	}
	
	
	@Override
	public void accept(PerformanceMeasurementPaperJournal2018PriorityWeights measurementTemp) {
		// TODO Auto-generated method stub
		
		if (forceMeasurement == true && db.rowExists(measurementTemp) == true) {
			db.deleteRow(measurementTemp);
		}
		if (db.rowExists(measurementTemp)) {
			System.out.println(measurementTemp.toString() + " already exists.");
			return;
		}
		
		DataGenerator data = getDataDayhead(measurementTemp);
		
		final InstanceHeuristicLP instance = new InstanceHeuristicAbsSoCLP(data);
		
		PerformanceMeasurementPaperJournal2018PriorityWeights measurement = measurementTemp.cloneWithMethod(measurementTemp.methodDayahead + "+" + measurementTemp.methodRealtime);
		log(0, "Running " + measurement, false, true);
		
		while (AppComparisonPerformancePaperJournal2018PriorityWeights.currentSCIPThreads.get() >= AppComparisonPerformancePaperJournal2018PriorityWeights.maxSCIPThreads) {
			try {
				//System.out.println("Waiting for SCIP threads to be available...");
				Thread.sleep(1000);
			} catch (InterruptedException e) { e.printStackTrace(); }
		}
		
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				
				AppComparisonPerformancePaperJournal2018PriorityWeights.currentSCIPThreads.incrementAndGet();
				
				System.out.println("Running final step in other thread, id=" + Thread.currentThread().getId() + ", currently running " + AppComparisonPerformancePaperJournal2018PriorityWeights.currentSCIPThreads.get() + " threads");
				
				instance.scipApplyRelativeGapSetting = false;
				instance.getInstanceLP().setNormalizingCoefficients(true);
				instance.getInstanceLP().objectiveFairShare.setWeight(measurement.weightFairShare);
				instance.getInstanceLP().objectiveEnergyCosts.setWeight(measurement.weightCosts);
				instance.getInstanceLP().objectivePeakShaving.setWeight(measurement.weightPeakShaving);
				instance.getInstanceLP().objectiveLoadImbalance.setWeight(measurement.weightLoadImbalance);
				
				instance.constructProblem(allRestrictions);
				
				
				JSONObject solvedProblemInstance = instance.getSolvedProblemInstanceJSON();
				// FileIO.writeFile(measurement.filePath, solvedProblemInstance);
				
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
				
				AppComparisonPerformancePaperJournal2018PriorityWeights.currentSCIPThreads.decrementAndGet();
			}
		});
		
		thread.start(); // run is synchronous, start starts a new thread

	}

	public static DataGeneratorRandom getDataDayhead(PerformanceMeasurementPaperJournal2018PriorityWeights measurement) {
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













