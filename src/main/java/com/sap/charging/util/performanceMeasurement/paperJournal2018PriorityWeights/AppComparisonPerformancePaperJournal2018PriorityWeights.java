package com.sap.charging.util.performanceMeasurement.paperJournal2018PriorityWeights;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.opt.heuristics.InstanceHeuristicAbsSoCLP;
import com.sap.charging.opt.heuristics.InstanceHeuristicRelSoCLP;
import com.sap.charging.opt.lp.Equation;
import com.sap.charging.opt.lp.InstanceLP;
import com.sap.charging.realTime.StrategyFromDayahead;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.configuration.Options;
import com.sap.charging.util.sqlite.SQLiteDB;

public class AppComparisonPerformancePaperJournal2018PriorityWeights {

	public static final int verbosity = 2;
	
	
	public static final int maxSCIPThreads = 10;
	public static AtomicInteger currentSCIPThreads = new AtomicInteger(0);
	
	
	public static void main(String[] args) {
		InstanceLP.verbosity = 2;
		Simulation.verbosity = 0;
		
		int nThreads = 20;
		System.out.println("AppComparisonPerformancePaperJournal2018PriorityWeights::main Init with nThreads=" + nThreads + "...");
		Options.set(24, 15, 0.85);
		
		double fuseSize = 999999; // Constant
		
		CONSTANTS.FUSE_LEVEL_0_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_1_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_2_SIZE = fuseSize;
		
		List<PerformanceMeasurementPaperJournal2018PriorityWeights> measurementJobs = new ArrayList<>();
		// Fill list of jobs

		int[] nCarsParams = new int[]{25};  // Constant 
		int[] nChargingStationsParams = new int[] {25}; // Constant 
		double[] randomnessParams = new double[] {0}; // Variable
		
		int[] seeds = IntStream.range(0, 0+1).toArray(); // Variable
		
		boolean normalizeWeights = true; // Should weights lie between 0 and 1? (0 vs 1, 0.001 vs 0.999, 0.002 vs 0.998)
		int nWeights = 1000; // 10000000; // when normalizing
		double[] weightParams = new double[nWeights+1];
		for (int i=0;i<weightParams.length;i++) { // 0 to 1 inclusive
			weightParams[i] = 1.0*i;
			if (normalizeWeights == true) {
				weightParams[i] /= (1.0*nWeights);
			}
		}
		
		
		PerformanceMeasurementPaperJournal2018PriorityWeights jobBasic = new PerformanceMeasurementPaperJournal2018PriorityWeights(nCarsParams[0], 
				nChargingStationsParams[0], randomnessParams[0], InstanceHeuristicRelSoCLP.getMethodStatic(), StrategyFromDayahead.getMethodStatic(),
				1e12, 1, 1, 1, seeds[0]);
		DataGenerator data = MeasurementExecutorPaperJournal2018PriorityWeights.getDataDayhead(jobBasic);
		new InstanceHeuristicAbsSoCLP(data);
		//InstanceHeuristicAbsSoCLP instance = new InstanceHeuristicAbsSoCLP(data);
		//instance.prepareInstanceLP();
		
		//instance.getInstanceLP().printNormalizingCoefficients();
		
		/*double coefficientFairShare = instance.getInstanceLP().getNormalizingCoefficientFairShare();
		double coefficientEnergyCosts = instance.getInstanceLP().getNormalizingCoefficientEnergyCosts();
		double coefficientPeakShaving = instance.getInstanceLP().getNormalizingCoefficientPeakShaving();
		double coefficientLoadImbalance = instance.getInstanceLP().getNormalizingCoefficientLoadImbalance();*/
		
		//if (true) return;
		
		//ArrayList<Equation> allRestrictions = instance.getInstanceLP().constructAllRestrictions();
		ArrayList<Equation> allRestrictions = null;
		
		System.out.println("Running nCarsParams=" + nCarsParams.length + ", nChargingStations=" + nChargingStationsParams.length + ", seeds=" + seeds.length);
		
		for (int nCars : nCarsParams) {
			for (int nChargingStations: nChargingStationsParams) {
				for (double randomness : randomnessParams) {
					for (int seed : seeds) {
						for (double variableWeight : weightParams) {
							
							for (int weightIndex=0; weightIndex<=3;weightIndex++) {
									
								double weightFairShare = 1e12;
								double weightCosts = 0;
								double weightPeakShaving = 0;
								double weightLoadImbalance = 0;
								
								if (weightIndex != 0) continue;
								
								switch (weightIndex) {
								case 0: // Fair share tradeoff
									weightFairShare = variableWeight*100000;
									weightCosts = 1;
									break;
								case 1: // Cost tradeoff
									weightCosts = variableWeight;
									weightPeakShaving = 1;
									break;
								case 2: // Peak tradeoff
									continue;
								case 3: // Load imbalance tradeoff
									weightCosts = 1;
									weightLoadImbalance = variableWeight*100000;
									break;
									
								}
								
								
								/*double weightPeakShaving;
								
								if (normalizeWeights == true) {
									weightPeakShaving = 1; // 1.0-weightCosts;
								}
								else {
									weightPeakShaving = nWeights - variableWeight;
								}*/
								
								//if (measurementJobs.size() >= 6) continue;
								
								PerformanceMeasurementPaperJournal2018PriorityWeights job2 = new PerformanceMeasurementPaperJournal2018PriorityWeights(nCars, 
										nChargingStations, randomness, InstanceHeuristicAbsSoCLP.getMethodStatic(), StrategyFromDayahead.getMethodStatic(),
										weightFairShare, weightCosts, weightPeakShaving, weightLoadImbalance, seed);
								
								measurementJobs.add(job2);
							}
							
							
						}
						
						
						
					}
				}
			}
		}
		//PerformanceMeasurementForecasting job = new PerformanceMeasurementForecasting(50, "forecastXGBoost", 0);
		//measurementJobs.add(job);
		
		//Collections.shuffle(measurementJobs); // Shuffle so that jobs are not done round robin but randomly
		
		// Stream jobs in parallel
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "" + nThreads);
		SQLiteDB db = new SQLiteDB("jdbc:sqlite:gen/performanceMeasurements.db");
		//measurementJobs.parallelStream().forEach(new MeasurementExecutorRandom2018_06(db, false));	
	
		measurementJobs.stream().forEach(new MeasurementExecutorPaperJournal2018PriorityWeights(db, false, allRestrictions));	
	}
	
	
	
}
