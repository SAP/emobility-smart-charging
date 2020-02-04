package com.sap.charging.opt.lp.util;

import java.util.ArrayList;
import java.util.Arrays;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.charging.opt.lp.InstanceLP;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.JSONKeys;

public class SolverSCIP extends Solver {

	private boolean applyRelativeGapSetting = false;
	public boolean useScip600 = false;
	
	@Override
	public String[] getCLCommand(String pathInputFile, String pathOutputFile) {

		ArrayList<String> commands = new ArrayList<>();
		
		if (useScip600) {
			commands.add("/usr/bin/scip");
		}
		else {
			commands.add("scip");
		}
		
		
		if (this.applyRelativeGapSetting == true) {
			commands.add("-c");
			if (isWindows()) {
				commands.add("'set limits gap 0.01'");
			}
			else {
				commands.add("set limits gap 0.01");
				// For debugging: more output?
				commands.add("-c");
				commands.add("set display verblevel 5");
			}
		}
		
		
		if (isWindows()) {
			commands.addAll(Arrays.asList(new String[] {
					"-c", "'read " + pathInputFile + "'",
					"-c", "optimize",
					"-c", "'write solution " + pathOutputFile + "'",
					"-c", "quit"}));
		}
		else {
			commands.addAll(Arrays.asList(new String[] {
					"-c", "read " + pathInputFile + "",
					"-c", "optimize",
					"-c", "write solution " + pathOutputFile + "",
					"-c", "quit"}));
		}
		
		
		if (isWindows()) {
			String scipCommand = String.join(" ", commands);
			commands.clear();
			commands.add("bash.exe");
			commands.add("-c");
			commands.add(scipCommand);
		}
		
		if (getVerbosity() < 2) {
			commands.add("-q");
		}

		return commands.toArray(new String[commands.size()]);
		
		/*return new String[] {
				"bash.exe", 
				"-c", "scip -c 'read " + pathInputFile + "' "
						 + "-c optimize "
						 + "-c 'write solution " + pathOutputFile + "' " 
						 + "-c quit"	
		};*/
	}
	
	
	public void setApplyRelativeGapSetting(boolean applyRelativeGapSetting) {
		this.applyRelativeGapSetting = applyRelativeGapSetting;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject getSolutionJSON(String pathInputFileSolution) {
		String solutionFileContent = FileIO.readFile(pathInputFileSolution); 
		String[] lines = solutionFileContent.split("\n");
		
		int nLines = (InstanceLP.verbosity >= 3) ? lines.length :
			Math.min(10, lines.length);
		
		for (int i=0;i<nLines; i++) {
			log(1, lines[i], false, true);
		}
		
		JSONObject solution = new JSONObject();
		JSONArray variables = new JSONArray();
		solution.put(JSONKeys.JSON_KEY_VARIABLES, variables);
		
		for (int i=0;i<lines.length;i++) {
			String line = lines[i];
			// First line:   "solution status: optimal solution found"
			// Second line:  "objective value:                    -10288.6956521739"
			// Other lines:  "X_i1_n0                            VALUE (obj:0)"
			if (i == 0) {
				String[] parts = line.split(":");
				solution.put(JSONKeys.JSON_KEY_SOLUTION_STATUS, parts[1].trim());
				if (parts[1].trim().equals("infeasible")) {
					throw new RuntimeException("Problem is infeasible!");
				}
			}
			else if (i == 1) { 
				// Objective value
				String[] parts = line.split(":");
				solution.put(JSONKeys.JSON_KEY_OBJECTIVE_VALUE, Double.parseDouble(parts[1].trim()));
			}
			else {
				String[] parts = line.split("\\s+"); // Split on at at least one whitespace
				JSONObject variable = new JSONObject();
				variable.put(JSONKeys.JSON_KEY_VARIABLE_NAME, parts[0]);
				
				if (parts[1].contains("+infinity") == false) {
					variable.put(JSONKeys.JSON_KEY_VARIABLE_VALUE, Double.parseDouble(parts[1]));
				}
				else {
					variable.put(JSONKeys.JSON_KEY_VARIABLE_VALUE, Double.POSITIVE_INFINITY);
				}
				
				
				// Variable objective coefficient format: (obj:-11.25)
				String objCoef = parts[2].split(":")[1];
				objCoef = objCoef.substring(0, objCoef.length()-1); // Strip last character ")" 
				variable.put(JSONKeys.JSON_KEY_VARIABLE_OBJECTIVE_COEFFICIENT, Double.parseDouble(objCoef));
				variables.add(variable); 
			}
		}
		
		return solution;
	}

	
	
	
}
