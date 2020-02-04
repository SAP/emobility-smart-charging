package com.sap.charging.opt;

import com.sap.charging.util.configuration.Options;

public class CONSTANTS {
	
	/**
	 * M Large constant for setting arbitrary high/low bounds
	 */
	public static final double M_HIGH_BOUND = 10000;
	

	/**
	 * Fuse size of the root fuse (at the transformer)
	 */
	public static double FUSE_LEVEL_0_SIZE = 4000;
	
	/**
	 * Fuse size of the Steigschienen
	 */
	public static double FUSE_LEVEL_1_SIZE = 1250;
	
	/**
	 * Fuse size of the Versorgungsschienen
	 */
	public static double FUSE_LEVEL_2_SIZE =  800;

	/**
	 * Charging efficiency (typically between 0.8 and 0.95) - how much
	 * energy is lost during the charging process?
	 * 
	 * Default is 0.85. 
	 */
	public static final double CHARGING_EFFICIENCY = Options.get().chargingEfficiency;
	
	/***********************
	 * FILE PATHS 
	 ***********************/
	/**
	 * Path to write temp (e.g. .lp and .sol files) to
	 */
	public static String PATH_DIR_GEN_TEMP = "gen/temp/";
	
	public static final String PATH_DIR_GEN_SOLUTION_PERFORMANCE = "gen/performance/";
	
	
}














