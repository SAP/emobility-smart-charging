package com.sap.charging;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorDeterministic;
import com.sap.charging.dataGeneration.DataGeneratorFromFile;
import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.ocpp.OCPPEntrypoint;
import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.opt.heuristics.InstanceHeuristicAbsSoCLP;
import com.sap.charging.opt.heuristics.InstanceHeuristicGreedy;
import com.sap.charging.opt.heuristics.InstanceHeuristicGreedyLP;
import com.sap.charging.opt.lp.InstanceLP;
import com.sap.charging.opt.lp.util.SolverSCIP;
import com.sap.charging.opt.solution.model.DayaheadSchedule;
import com.sap.charging.realTime.Strategy;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.StrategyFromDayahead;
import com.sap.charging.realTime.StrategyGreedy;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.cli.CLArgumentInvalidFormat;
import com.sap.charging.util.cli.CLArgumentInvalidName;
import com.sap.charging.util.cli.CLArgumentInvalidValue;
import com.sap.charging.util.cli.CLArgumentInvalidValueType;
import com.sap.charging.util.cli.CLArgumentMissing;
import com.sap.charging.util.cli.CLArgumentsManager;

/**
 * 
 * Command line interface for optimization, in the format param1=value1 param2=value2. 
 * Available arguments are: see util.cli.CLIArgumentsManager
 */
public class AppCLI {

	public static void main(String[] args) throws CLArgumentMissing, CLArgumentInvalidValue, CLArgumentInvalidName, CLArgumentInvalidValueType, CLArgumentInvalidFormat, ParseException {
		CLArgumentsManager cli = new CLArgumentsManager();
		cli.init(args);
		
		int verbosity = cli.verbosity.getValue();
		Simulation.verbosity = verbosity;
		InstanceLP.verbosity = verbosity;
		
		int fuseLevel0 = cli.fuseLevel0.getValue();
		CONSTANTS.FUSE_LEVEL_0_SIZE = fuseLevel0;
		int fuseLevel1 = cli.fuseLevel1.getValue();
		CONSTANTS.FUSE_LEVEL_1_SIZE = fuseLevel1;
		int fuseLevel2 = cli.fuseLevel2.getValue();
		CONSTANTS.FUSE_LEVEL_2_SIZE = fuseLevel2;
		System.out.println("Setting fuseLevel0=" + fuseLevel0 + ", fuseLevel1=" + fuseLevel1 + ", fuseLevel2=" + fuseLevel2);
		
		// Retrieve data or generate it
		String dataSource = cli.dataSource.getValue();
		DataGenerator dataGenerator = getDataGenerator(dataSource, cli);
		
		// Make sure temp directory exists to create .lp and .sol files in
		CONSTANTS.PATH_DIR_GEN_TEMP = cli.pathDirGenTemp.getValue();
		FileIO.createDir(CONSTANTS.PATH_DIR_GEN_TEMP);
		
		String simulationMode = cli.mode.getValue();
		String method = cli.method.getValue();
		
		JSONObject solvedProblemInstance;
		switch (simulationMode) {
		case "dayAhead":
			switch (method) {
			case "LP": 
				InstanceLP instanceLP = new InstanceLP(dataGenerator);
				instanceLP.constructVariables();
				instanceLP.constructProblem();
				
				instanceLP.setSolver(new SolverSCIP());
				instanceLP.solveProblem();
				
				solvedProblemInstance = instanceLP.getSolvedProblemInstanceJSON();
				break;
			case "greedy":
				InstanceHeuristicGreedy instanceHeuristic = new InstanceHeuristicGreedy(dataGenerator);
				solvedProblemInstance = instanceHeuristic.getSolvedProblemInstanceJSON();
				break;
			default:
			case "greedyLP": 
				InstanceHeuristicGreedyLP instanceGreedyLP = new InstanceHeuristicGreedyLP(dataGenerator);
				solvedProblemInstance = instanceGreedyLP.getSolvedProblemInstanceJSON();
				break;
			}
			break;
		case "ocpp":
			String pathDataFromFile = cli.pathDataFromFile.getValue();
			JSONParser parser = new JSONParser();
			try {
				JSONObject input = (JSONObject) parser.parse(FileIO.readFile(pathDataFromFile));
				OCPPEntrypoint ocppEntrypoint = new OCPPEntrypoint(input);
				
				solvedProblemInstance = ocppEntrypoint.generateOutput();
				
			}
			catch (Exception e) {
				throw new RuntimeException("Invalid json supplied in file=" + pathDataFromFile);
			}
			
			break;			
		case "realTime":
		default:
			String strategyString = cli.method.getValue();
			Strategy strategy;
			switch (strategyString) {
			case "LP":
				InstanceLP instanceLP = new InstanceLP(dataGenerator);
				instanceLP.constructVariables();
				instanceLP.constructProblem();
				
				instanceLP.setSolver(new SolverSCIP());
				instanceLP.solveProblem();
				DayaheadSchedule scheduleLP = new DayaheadSchedule(instanceLP.getSolvedProblemInstanceJSON());
				strategy = new StrategyFromDayahead(scheduleLP);
				break;
			case "greedy":
				strategy = new StrategyGreedy();
				break;
			case "greedyLP":
				InstanceHeuristicGreedyLP instanceGreedyLP = new InstanceHeuristicGreedyLP(dataGenerator);
				DayaheadSchedule scheduleGreedyLP = new DayaheadSchedule(instanceGreedyLP.getSolvedProblemInstanceJSON());
				strategy = new StrategyFromDayahead(scheduleGreedyLP);
				break;
			case "absSoCLP":
				InstanceHeuristicAbsSoCLP instanceAbsSoCLP = new InstanceHeuristicAbsSoCLP(dataGenerator);
				DayaheadSchedule scheduleAbsSoCLP = new DayaheadSchedule(instanceAbsSoCLP.getSolvedProblemInstanceJSON());
				strategy = new StrategyFromDayahead(scheduleAbsSoCLP);
				break;
			case "algorithmicFromDayAhead":
				instanceGreedyLP = new InstanceHeuristicGreedyLP(dataGenerator);
				scheduleGreedyLP = new DayaheadSchedule(instanceGreedyLP.getSolvedProblemInstanceJSON());
				strategy = new StrategyAlgorithmic(scheduleGreedyLP);
				break;
			case "algorithmic":
			default:
				strategy = new StrategyAlgorithmic();
				break;
			}
			
			Simulation sim = new Simulation(dataGenerator, strategy);
			sim.setEnableJSONStorage(true);
			sim.init();
			sim.simulate();
			solvedProblemInstance = sim.getSimulationResult().getSolvedProblemInstanceJSON();
			
			break;
		}
		
		FileIO.writeFile(cli.pathSolvedProblemInstanceJSON.getValue(), solvedProblemInstance);
		
		
		
	}
	
	private static DataGenerator getDataGenerator(String dataSource, CLArgumentsManager cli) throws ParseException {
		DataGenerator dataGenerator;
		switch (dataSource) {
		case "deterministic":
			dataGenerator = new DataGeneratorDeterministic();
			
			break;
		case "fromFile": 
			String pathDataFromFile = cli.pathDataFromFile.getValue();
			dataGenerator = new DataGeneratorFromFile(pathDataFromFile);
			((DataGeneratorFromFile) dataGenerator).generateAll();
			break;
		case "random":
		default:
			int nCars = cli.nCars.getValue();
			int nChargingStations = cli.nChargingStations.getValue();
			int nTimeslots = cli.nTimeslots.getValue();
			int nChargingStationsPerGroup = cli.nChargingStationsPerGroup.getValue();
			int seed = cli.seed.getValue();
			
			dataGenerator = new DataGeneratorRandom(seed, false);
			dataGenerator.generateEnergyPriceHistory(nTimeslots)
						 .generateCars(nCars)
						 .generateChargingStations(nChargingStations)
						 .generateFuseTree(nChargingStationsPerGroup, true);
			break;
		}
		return dataGenerator;
	}

}
