package com.sap.charging.opt.lp.util;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.charging.util.FileIO;
import com.sap.charging.util.JSONKeys;

public class SolverGurobi extends Solver {

	@Override
	public String[] getCLCommand(String pathInputFile, String pathOutputFile) {
		return new String[] {
				"gurobi_cl",
				"ResultFile=" + pathOutputFile,
				pathInputFile
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject getSolutionJSON(String pathInputFileSolution) {
		String content = FileIO.readFile(pathInputFileSolution);
		JSONObject result = new JSONObject();
		JSONArray variables = new JSONArray();
		result.put(JSONKeys.JSON_KEY_VARIABLES, variables);
		
		String[] lines = content.split("\n");
		for (int i=0;i<lines.length;i++) {
			// First line: # Objective value = -7.1565458260869578e+04
			// Other lines: P_i0_j1_k14 5.6843418860808015e-14
			if (i==0) {
				String[] parts = lines[i].split("=");
				result.put(JSONKeys.JSON_KEY_OBJECTIVE_VALUE, Double.parseDouble(parts[1].trim()));
			}
			else {
				String[] parts = lines[i].split(" ");
				
				JSONObject variable = new JSONObject();
				variable.put(JSONKeys.JSON_KEY_VARIABLE_NAME, parts[0]);
				variable.put(JSONKeys.JSON_KEY_VARIABLE_VALUE, Double.parseDouble(parts[1]));
				// Variable objective coefficient format: (obj:-11.25)
				variables.add(variable); 
			}
			
		}
				
		return result;
	}
	
}
