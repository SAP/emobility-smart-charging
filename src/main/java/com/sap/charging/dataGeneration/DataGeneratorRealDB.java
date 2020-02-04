package com.sap.charging.dataGeneration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.FuseTree;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.sqlite.SQLiteDB;

public class DataGeneratorRealDB extends SQLiteDB {
	
	public static final String dbPath = FileIO.readJSONFile("config.json").get("pathToDataChargeLogs").toString() + "/dataChargeLog.db";
	public static final String dbUrl = "jdbc:sqlite:" + dbPath;
	
	private final String tableNameEnergyPriceHistory = "energyPriceHistory";
	private final String tableNameCars = "cars";
	private final String tableNameFuseTree = "fuseTree";
	
	public DataGeneratorRealDB() {
		super();
	}
	
	@Override
	protected String getDBUrl() {
		return dbUrl;
	}
	
	/**
	 * Optionally, limit the number of charging stations 
	 * (pass 0 for no limit). 
	 * @param limitNumberChargingStations
	 * @return
	 */
	public FuseTree retrieveFuseTree(int limitNumberChargingStations, List<ChargingStation> chargingStations) {
		String sql = "SELECT fuseTreeJSON FROM " + tableNameFuseTree;
		ResultSet result = executeQuery(sql);
		try {
			if (result.next()) {
				String json = result.getString("fuseTreeJSON");
				JSONParser parser = new JSONParser();
				JSONObject jsonFuseTree = (JSONObject) parser.parse(json);
				FuseTree fuseTree = FuseTree.fromJSON(jsonFuseTree, limitNumberChargingStations, chargingStations);
				return fuseTree;
			}
			else {
				throw new NullPointerException("No fuse tree data was found.");
			}
		}
		catch (SQLException | ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Available fields in original tables are:
	 * date TEXT,
	 * energyPriceHistoryJSON TEXT
	 * @param date
	 * @return
	 */
	public EnergyPriceHistory retrieveEnergyPriceHistory(String date) {
		String sql = "SELECT eneryPriceHistoryJSON FROM " + tableNameEnergyPriceHistory +
				" WHERE date='" + date + "'";
		ResultSet result = executeQuery(sql);
		try {
			if (result.next()) {
				String json = result.getString(1);
				JSONParser parser = new JSONParser();
				EnergyPriceHistory energyPriceHistory = EnergyPriceHistory.fromJSON((JSONObject) parser.parse(json));
				return energyPriceHistory;
			}
			else {
				throw new NullPointerException("No data was found in energyPriceHistory for day=" + date);
			}
		}
		catch (SQLException | ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
	

	/**
	 * Filtered by charging processes only in WDF16, 
	 * sorted by timestampArrival (in seconds). 
	 * 
	 * Available fields in result set:
	 * originalID TEXT,
     * date TEXT,
     * carPark TEXT, 
     * chargePointLabel TEXT,
     * timestampArrival INT,
     * timestampDeparture INT,
     * amountCharged REAL, (double)
     * carType TEXT
	 * @param date
	 * @return
	 */
	public ResultSet retrieveCars(String date, String carParkName) {
		String sql = "SELECT * FROM " + tableNameCars + 
				" WHERE date='" + date + "'" +
				"   AND carPark='" + carParkName + "'" +
				" ORDER BY timestampArrival";
		return executeQuery(sql);
	}
	
	public ArrayList<String> retrieveAvailableDates(String carParkName) {
		ArrayList<String> result = new ArrayList<>();
		
		String sql = "SELECT distinct date FROM " + tableNameCars + 
					  " WHERE CarPark='" + carParkName + "'" +
					  " ORDER BY date";
		ResultSet resultSet = executeQuery(sql);
		try {
			while (resultSet.next()) {
				result.add(resultSet.getString("date"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	
}
