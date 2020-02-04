package com.sap.charging.util.performanceMeasurement.random;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.simple.JSONObject;

import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.Util;
import com.sap.charging.util.performanceMeasurement.DB;
import com.sap.charging.util.performanceMeasurement.PerformanceMeasurementTemplate;

public class DBRandom extends DB {
	
	
	private final String tableName = "performanceMeasurements";
	
	public DBRandom() {
		super();
	}
	
	
	@Override
	protected String getTableName() {
		return this.tableName;
	}
	
	@Override
	protected void createTable() {
		String sql = "CREATE TABLE " + tableName + "(" +
						"guid text, " +
						"filePath text, " + 
						"nCars int, " + 
						"nChargingStations int, " +
						"nTimeslots int," +
						"seed int," +
						"method text, " + 
						"PRIMARY KEY (nCars, nChargingStations, nTimeslots, seed, method)" +
					 ")";
		System.out.println("DBRandom::createTable Creating table " + tableName);
		executeStatement(sql);
	}
	
	@SuppressWarnings("rawtypes")
	public synchronized void insertMeasurement(PerformanceMeasurementTemplate measurementParam, JSONObject result) {
		PerformanceMeasurementRandom measurement = (PerformanceMeasurementRandom) measurementParam;
		String pathBase = CONSTANTS.PATH_DIR_GEN_SOLUTION_PERFORMANCE;
		String guid = Util.generateGUID();
		String filePath = pathBase + guid + ".json";
		
		String sql = "INSERT INTO " + tableName + " VALUES(" +
						"'" + guid + "','" + filePath + "'," +
						measurement.getNCars() + "," + 
						measurement.getNChargingStations ()+ "," + 
						measurement.getNTimeslots() + "," +
						measurement.getSeed() + ",'" + 
						measurement.getMethod() + "'" + 
				     ")";
		System.out.println("DBRandom::insertMeasurement Executing " + sql);
		executeStatement(sql);
		
		FileIO.writeFile(filePath, result);
	}
	
	@SuppressWarnings("rawtypes")
	public synchronized boolean measurementExists(PerformanceMeasurementTemplate measurementParam) {
		PerformanceMeasurementRandom measurement = (PerformanceMeasurementRandom) measurementParam;
		
		boolean dbEntryExists = false;
		String sql = "SELECT * FROM " + getTableName() + " " + 
				 	    "WHERE nCars=" + measurement.getNCars() + 
						"  AND nChargingStations=" + measurement.getNChargingStations() + 
						"  AND nTimeslots=" + measurement.getNTimeslots() + 
						"  AND seed=" + measurement.getSeed() + 
						"  AND method='" + measurement.getMethod() + "'";
		try {
			ResultSet rs = executeQuery(sql);
			if (rs.next() == true) {
				System.out.println("DBRandom::measurementExists " + measurement.toString() + " already exists.");
				dbEntryExists = true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return dbEntryExists;
	}
	
}




