package com.sap.charging.util.performanceMeasurement;

import java.io.File;

import org.json.simple.JSONObject;

import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.util.Loggable;
import com.sap.charging.util.sqlite.SQLiteDB;

public abstract class DB extends SQLiteDB implements Loggable {

	public static int verbosity = 2;
	public int getVerbosity() {
		return verbosity;
	}
	
	public static final String dbUrl = "jdbc:sqlite:gen/performanceMeasurements.db";
	
	public DB() {
		super();
		if (tableExists(getTableName()) == false) {
			createTable();
		}
		tidyDirectory(CONSTANTS.PATH_DIR_GEN_TEMP);
	}
	
	@Override
	protected String getDBUrl() {
		return dbUrl;
	}
	
	/**
	 * Tidy gen/temp/ delete all files .lp and .sol files
	 */
	private void tidyDirectory(String path) {
		File folder = new File(path);
		File[] fList = folder.listFiles();
		for (File file : fList) {
			if (file.getName().endsWith(".lp") ||
				file.getName().endsWith(".sol"))  {
				file.delete();
			}
		}
	}
	
	protected abstract void createTable();
	protected abstract String getTableName();
	
	@SuppressWarnings("rawtypes")
	public abstract void insertMeasurement(PerformanceMeasurementTemplate measurementParam, JSONObject result);
	@SuppressWarnings("rawtypes")
	public abstract boolean measurementExists(PerformanceMeasurementTemplate measurementParam);
	
}
