package com.sap.charging.opt.lp.util;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;

import org.json.simple.JSONObject;

import com.sap.charging.opt.lp.InstanceLP;
import com.sap.charging.util.Loggable;


public abstract class Solver implements Loggable {
	
	public int getVerbosity() {
		return InstanceLP.verbosity;
	}

	protected abstract String[] getCLCommand(String pathInputFile, String pathOutputFile);
	
	/**
	 * Execute either SCIP or Gurobi to solve the constructed file, e.g. .lp or .mps file
	 */
	public void solveProblem(String pathInputFile, String pathOutputFile) {
		
		log(1, "Running command: ");
		String[] command = getCLCommand(pathInputFile, pathOutputFile);
		log(1, Arrays.toString(command));
		
		try {
			ProcessBuilder b = new ProcessBuilder(command);
			
			if (InstanceLP.verbosity >= 2) {
				b.redirectOutput(Redirect.INHERIT);
			}
			b.redirectError(Redirect.INHERIT);

			Process pr = b.start();
			pr.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}	
		
	}
	
	
	public abstract JSONObject getSolutionJSON(String pathInputFileSolution);
	
	protected boolean isWindows() {
		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("win") >= 0);

	}
	
	
}









