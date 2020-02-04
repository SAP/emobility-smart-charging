package com.sap.charging.util.cli;

import java.util.ArrayList;
import java.util.HashMap;

import com.sap.charging.opt.CONSTANTS;

public class CLArgumentsManager {
	
	// General arguments
	public final CLArgument<Integer> verbosity = new CLArgument<Integer>("verbosity", Integer.class, 
			true, 2, null);
	public final CLArgument<String> mode = new CLArgument<>("mode", String.class,
			true, "realTime", new String[]{"dayAhead", "realTime", "ocpp"});
	public final CLArgument<String> dataSource = new CLArgument<>("dataSource", String.class,
			true, "random", new String[]{"deterministic", "fromFile" ,"random"});
	
	/**
	 * Required only if dataSource = "fromFile"
	 */
	public final CLArgument<String> pathDataFromFile = new CLArgument<>("pathDataFromFile", String.class,
			true, null, null);
	
	public final CLArgument<String> pathDirGenTemp = new CLArgument<>("pathDirGenTemp", String.class,
			true, getDefaultPathDirGenTemp(), null);
	public final CLArgument<String> pathSolvedProblemInstanceJSON = new CLArgument<>("pathSolvedProblemInstanceJSON", String.class,
			true, getDefaultPathSolvedProblemInstanceJSON(), null);
	
	// Simulation parameters
	public final CLArgument<String> method = new CLArgument<String>("method", String.class,
			true, "algorithmic", new String[]{"LP","greedy","greedyLP","absSoCLP",
											  "algorithmic", "algorithmicFromDayahead"});
	public final CLArgument<Integer> nCars = new CLArgument<Integer>("nCars", Integer.class,
			true, 5, null);
	public final CLArgument<Integer> nChargingStations = new CLArgument<Integer>("nChargingStations", Integer.class,
			true, 5, null);
	public final CLArgument<Integer> nTimeslots = new CLArgument<Integer>("nTimeslots", Integer.class,
			true, 96, null);
	public final CLArgument<Integer> nChargingStationsPerGroup = new CLArgument<Integer>("nChargingStationsPerGroup", Integer.class,
			true, 10, null);
	public final CLArgument<Integer> seed = new CLArgument<Integer>("seed", Integer.class,
			true, 0, null);
	
	public final CLArgument<Integer> fuseLevel0 = new CLArgument<Integer>("fuseLevel0", Integer.class,
			true, (int) CONSTANTS.FUSE_LEVEL_0_SIZE, null);
	public final CLArgument<Integer> fuseLevel1 = new CLArgument<Integer>("fuseLevel1", Integer.class,
			true, (int) CONSTANTS.FUSE_LEVEL_1_SIZE, null);
	public final CLArgument<Integer> fuseLevel2 = new CLArgument<Integer>("fuseLevel2", Integer.class,
			true, (int) CONSTANTS.FUSE_LEVEL_2_SIZE, null);
	
	
	private boolean inTestingEnvironment = false;
	private boolean isHelpPrinted = false;
	
	
	@SuppressWarnings("rawtypes")
	private ArrayList<CLArgument> args;
	
	public CLArgumentsManager() {
		args = new ArrayList<>();
		args.add(verbosity);
		args.add(mode);
		args.add(dataSource);
		args.add(pathDataFromFile);
		args.add(pathDirGenTemp);
		args.add(pathSolvedProblemInstanceJSON);
		
		args.add(method);
		args.add(nCars);
		args.add(nChargingStations);
		args.add(nTimeslots);
		args.add(nChargingStationsPerGroup);
		args.add(seed);
		args.add(fuseLevel0);
		args.add(fuseLevel1);
		args.add(fuseLevel2);
	}
	
	@SuppressWarnings("rawtypes")
	public ArrayList<CLArgument> getPossibleArgs() {
		return args;
	}
	
	@SuppressWarnings("rawtypes")
	private void printHelp() {
		String helpString = "Call with:\n";
		helpString += "java -jar NAME.jar param1=value1 param2=value2\n";
		helpString += "com.sap.charging.playground help:\n";
		for (CLArgument arg : args) {
			helpString += "\t" + arg.toString() + "\n";
		}
		System.out.println(helpString);
		isHelpPrinted = true;
	}
	
	private HashMap<String,String> getRawKeyValue(String[] args) throws CLArgumentInvalidFormat {
		HashMap<String,String> result = new HashMap<>();
		for (int i=0;i<args.length;i++) {
			if (args[i].equals("--help")) {
				printHelp();
				break;
			}
				
			
			String[] parts = args[i].split("=");
			
			if (parts.length != 2) {
				throw new CLArgumentInvalidFormat(args[i]);
			}
			
			String argName = parts[0];
			String argValue = parts[1];
			result.put(argName, argValue);
		}
		return result;
	}
	 
	@SuppressWarnings("rawtypes")
	public CLArgument getCLArgument(String name) throws CLArgumentInvalidName {
		for (CLArgument arg : args) {
			if (name.equals(arg.name)) {
				return arg;
			}
		}
		throw new CLArgumentInvalidName(name);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void init(String[] cliArgs) throws CLArgumentMissing, 
										   CLArgumentInvalidValue, 
										   CLArgumentInvalidName, 
										   CLArgumentInvalidValueType, 
										   CLArgumentInvalidFormat {
		HashMap<String,String> raw = getRawKeyValue(cliArgs);
		if (isHelpPrinted == true && inTestingEnvironment == false) {
			System.exit(0);
		}
		for (String argName : raw.keySet()) {
			// Set values from missing params, either default values or missing
			CLArgument arg = getCLArgument(argName);
			String rawValue = raw.get(argName);
			try {
				Object value;
				if (arg.type == Integer.class) {
					value = Integer.parseInt(rawValue);
				}
				else if (arg.type == Double.class) {
					value = Double.parseDouble(rawValue);
				}
				else {
					value = rawValue;
				}
				arg.setValue(value);
				System.out.println("CLArgumentsManager::init Setting " + 
						arg.name + "=" + value);
			} catch (NumberFormatException e) {
				throw new CLArgumentInvalidValueType(arg, rawValue);
			}
		}
		// Apply default value if missing
		for (CLArgument arg : args) {
			if (arg.isValueSet() == false) {
				if (arg.optional == false) {
					throw new CLArgumentMissing(arg.name);
				}
				System.out.println("CLArgumentsManager::init Setting " + 
										arg.name + "=" + arg.defaultValue + " [DEFAULT]");
				arg.setValue(arg.defaultValue);
			}
		}
	}
	
	
	
	
	// See https://www.mkyong.com/java/how-to-detect-os-in-java-systemgetpropertyosname/
	private String getOSString() {
		return System.getProperty("os.name").toLowerCase();
	}
	private boolean isOSWindows() {
		return getOSString().indexOf("win") >= 0;
	}
	private boolean isOSUnix() {
		return (getOSString().indexOf("nix") >= 0 || 
				getOSString().indexOf("nux") >= 0 || 
				getOSString().indexOf("aix") > 0 );

	}
	private String getDefaultPathDirGenTemp() {
		if (isOSUnix()){
			return "/tmp/ChargeTest/";
		}
		if (isOSWindows()) {
			return CONSTANTS.PATH_DIR_GEN_TEMP;
		}
		return CONSTANTS.PATH_DIR_GEN_TEMP;
	}
	private String getDefaultPathSolvedProblemInstanceJSON() {
		if (isOSWindows()) {
			return "solution.json";
		}
		if (isOSUnix()) {
			return "solution.json";
			//return "/usr/src/ChargeTest/vis/data/";
		}
		return "solution.json";
	}
	
	public void setInTestingEnvironment() {
		this.inTestingEnvironment = true;
	}
	
	
}
