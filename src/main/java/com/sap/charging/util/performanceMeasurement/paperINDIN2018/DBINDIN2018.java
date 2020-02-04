package com.sap.charging.util.performanceMeasurement.paperINDIN2018;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.simple.JSONObject;

import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.Util;
import com.sap.charging.util.performanceMeasurement.DB;
import com.sap.charging.util.performanceMeasurement.PerformanceMeasurementTemplate;

public class DBINDIN2018 extends DB {

	private final String tableName = "performanceMeasurementsINDIN2018";
	
	public DBINDIN2018() {
		super();
	}
	
	@Override
	protected String getTableName() {
		return tableName;
	}
	
	@Override
	protected void createTable() {
		String sql = "CREATE TABLE " + getTableName() + "(" +
				"guid text, " +
				"filePath text, " + 
				"nCars integer, " + 
				"proportionEVs real, " +
				"gridConnection real, " +
				"method text, " + 
				"PRIMARY KEY (nCars, proportionEVs, gridConnection, method)" +
			 ")";
		log(1, "Creating table " + tableName);
		executeStatement(sql);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void insertMeasurement(PerformanceMeasurementTemplate measurementParam, JSONObject result) {
		PerformanceMeasurementINDIN2018 measurement = (PerformanceMeasurementINDIN2018) measurementParam;
		String pathBase = CONSTANTS.PATH_DIR_GEN_SOLUTION_PERFORMANCE;
		String guid = Util.generateGUID();
		String filePath = pathBase + guid + ".json";
		
		String sql = "INSERT INTO " + getTableName() + " VALUES(" +
						"'" + guid + "','" + filePath + "'," +
						measurement.getNCars() + "," + 
						measurement.getProportionEVs() + "," +
						measurement.getGridConnection() + ",'" +
						measurement.getMethod() + "')";
		log(2, "Executing " + sql);
		executeStatement(sql);

		FileIO.writeFile(filePath, result);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean measurementExists(PerformanceMeasurementTemplate measurementParam) {
		PerformanceMeasurementINDIN2018 measurement = (PerformanceMeasurementINDIN2018) measurementParam;
		
		String sql = "SELECT * FROM " + getTableName() + " " + 
						"WHERE nCars=" + measurement.getNCars() + " " +
						"  AND proportionEVs=" + measurement.getProportionEVs() + " " + 
						"  AND gridConnection=" + measurement.getGridConnection() + " " +
						"  AND method='" + measurement.getMethod() + "' ";
		
		boolean dbEntryExists = false;
		try {
			ResultSet rs = executeQuery(sql);
			if (rs.next() == true) {
				log(2, measurement.toString() + " already exists.");
				dbEntryExists = true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return dbEntryExists;
	}
	
}
