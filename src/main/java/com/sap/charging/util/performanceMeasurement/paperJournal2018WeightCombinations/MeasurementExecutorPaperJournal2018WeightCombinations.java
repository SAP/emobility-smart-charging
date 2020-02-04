package com.sap.charging.util.performanceMeasurement.paperJournal2018WeightCombinations;

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
import com.sap.charging.realTime.StrategyGreedy;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.performanceMeasurement.MeasurementExecutor;
import com.sap.charging.util.random.Distribution;
import com.sap.charging.util.random.NormalDistribution;
import com.sap.charging.util.sqlite.SQLiteDB;

public class MeasurementExecutorPaperJournal2018WeightCombinations extends MeasurementExecutor<PerformanceMeasurementPaperJournal2018WeightCombinations>{

	@Override
	public int getVerbosity() {
		return AppComparisonPerformancePaperJournal2018WeightCombinations.verbosity;
	}
	
	public MeasurementExecutorPaperJournal2018WeightCombinations(SQLiteDB db) {
		super(db);
	}
	public MeasurementExecutorPaperJournal2018WeightCombinations(SQLiteDB db, boolean forceMeasurement) {
		super(db, forceMeasurement);
	}

	private ArrayList<Equation> allRestrictions;
	
	public MeasurementExecutorPaperJournal2018WeightCombinations(SQLiteDB db, boolean forceMeasurement, ArrayList<Equation> allRestrictions) {
		super(db, forceMeasurement);
		this.allRestrictions = allRestrictions;
	}
	
	
	@Override
	public void accept(PerformanceMeasurementPaperJournal2018WeightCombinations measurementTemp) {
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
		
		PerformanceMeasurementPaperJournal2018WeightCombinations measurement = measurementTemp.cloneWithMethod(measurementTemp.methodDayahead + "+" + measurementTemp.methodRealtime);
		log(0, "Running " + measurement, false, true);
		
		while (AppComparisonPerformancePaperJournal2018WeightCombinations.currentSCIPThreads.get() >= AppComparisonPerformancePaperJournal2018WeightCombinations.maxSCIPThreads) {
			try {
				//System.out.println("Waiting for SCIP threads to be available...");
				Thread.sleep(1000);
			} catch (InterruptedException e) { e.printStackTrace(); }
		}
		
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				
				AppComparisonPerformancePaperJournal2018WeightCombinations.currentSCIPThreads.incrementAndGet();
				
				System.out.println("Running final step in other thread, id=" + Thread.currentThread().getId() + ", currently running " + AppComparisonPerformancePaperJournal2018WeightCombinations.currentSCIPThreads.get() + " threads");
				
				Strategy strategy = null;
				double timeProblemConstructionDayahead = 0;
				double timeSolutionDayahead = 0;
				
				if (measurementTemp.methodDayahead != null) {
					instance.scipApplyRelativeGapSetting = true;
					instance.getInstanceLP().setNormalizingCoefficients(true);
					instance.getInstanceLP().objectiveFairShare.setWeight(measurement.weightFairShare);
					instance.getInstanceLP().objectiveEnergyCosts.setWeight(measurement.weightCosts);
					instance.getInstanceLP().objectivePeakShaving.setWeight(measurement.weightPeakShaving);
					instance.getInstanceLP().objectiveLoadImbalance.setWeight(measurement.weightLoadImbalance);
					
					instance.constructProblem(allRestrictions);
					
					
					JSONObject solvedProblemInstance = instance.getSolvedProblemInstanceJSON();
					// FileIO.writeFile(measurement.filePath, solvedProblemInstance);
					
					timeProblemConstructionDayahead = instance.timeProblemConstruction.getTime();
					timeSolutionDayahead = instance.timeSolution.getTime();
					
					DayaheadSchedule schedule = new DayaheadSchedule(solvedProblemInstance);
					strategy = new StrategyFromDayahead(schedule);
				}
				if (measurementTemp.methodRealtime.equals(StrategyGreedy.getMethodStatic())) {
					strategy = new StrategyGreedy();
				}
				
				
				Simulation sim = new Simulation(data, strategy);
				sim.init();
				sim.simulate();
				
				if (measurementTemp.methodDayahead != null) {
					sim.getSimulationResult().timeProblemConstruction.addTime(timeProblemConstructionDayahead);
					sim.getSimulationResult().timeSolution.addTime(timeSolutionDayahead);
				}
				
				FileIO.writeFile(measurement.filePath, sim.getSimulationResult().getSolvedProblemInstanceJSON());
				
				db.insert(measurement);
				
				AppComparisonPerformancePaperJournal2018WeightCombinations.currentSCIPThreads.decrementAndGet();
			}
		});
		
		thread.start(); // run is synchronous, start starts a new thread

	}

	public static DataGeneratorRandom getDataDayhead(PerformanceMeasurementPaperJournal2018WeightCombinations measurement) {
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













