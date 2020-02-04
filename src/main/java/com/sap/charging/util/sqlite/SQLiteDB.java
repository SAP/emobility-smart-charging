package com.sap.charging.util.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class SQLiteDB {

	private String dbUrl;
	private final Connection connection;
	
	
	public SQLiteDB() {
		this(null);
	}
	
	public SQLiteDB(String dbUrl) {
		this.dbUrl = dbUrl;
		this.connection = getDBConnection();
	}
	
	protected Connection getDBConnection() {
		if (connection != null) 
			return connection;
		
		try  {
			Connection connection = DriverManager.getConnection(getDBUrl());
            return connection;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
		return null;
	}
	
	protected String getDBUrl() {
		return dbUrl;
	}
	
	protected synchronized ResultSet executeQuery(String sql) {
		try {
			Statement stmt = getDBConnection().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			return rs;
		} catch (SQLException e) {
			System.out.println("Executed statement:");
			System.out.println(sql);
			e.printStackTrace();
		}
		return null;
	}
	protected synchronized void executeStatement(String sql) {
		try {
			Statement stmt = getDBConnection().createStatement();
			stmt.execute(sql);
		} catch (SQLException e) {
			System.out.println("Executed statement:");
			System.out.println(sql);
			e.printStackTrace();
		}
	}
	
	protected synchronized boolean tableExists(String tableName) {
		String sql = "SELECT name FROM sqlite_master "
				+ "WHERE type='table'" 
				+ "  AND name='" + tableName + "'";
		try {
			while (executeQuery(sql).next()) {
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("SQLiteDB::tableExists Table '" + tableName + "' does not exist yet!");
		return false;
	}
	
	public synchronized boolean tableExists(SQLiteTableRow tableRow) {
		return tableExists(tableRow.getTableName());
	}
	
	public synchronized void createTable(SQLiteTableRow tableRow) {
		String sql = tableRow.getCreateTableStatement();
		executeStatement(sql);
	}
	
	public synchronized void insert(SQLiteTableRow tableRow, boolean forceTableCreate) {
		if (forceTableCreate == true && tableExists(tableRow) == false)
			createTable(tableRow);

		String sql = tableRow.getInsertStatement();
		executeStatement(sql);
	}
	public synchronized void insert(SQLiteTableRow tableRow) {
		insert(tableRow, true);
	}
	
	
	public synchronized boolean rowExists(SQLiteTableRow tableRow, boolean forceTableCreate) {
		if (forceTableCreate == true && tableExists(tableRow) == false)
			createTable(tableRow);
		
		String sql = tableRow.getSelectRowsStatement();
		ResultSet resultSet = executeQuery(sql);
		try {
			while (resultSet.next()) 
				return true;
		} catch (SQLException e) {
			System.out.println("Executed statement:");
			System.out.println(sql);
			e.printStackTrace();
		}
		return false;
	}
	public synchronized boolean rowExists(SQLiteTableRow tableRow) {
		return rowExists(tableRow, true);
	}
	
	
	/**
	 * Used for UPDATING a tableRow (the java object) with non-key attributes, e.g. retrieve GUID and filePath for given nCars and table
	 * @param tableRow
	 * @return
	 */
	public synchronized void retrieveRow(SQLiteTableRow tableRow) {
		String sql = tableRow.getSelectRowsStatement();
		ResultSet resultSet = executeQuery(sql);
		try {
			boolean rowUpdated = false;
			while (resultSet.next()) {
				SQLiteTableRow.fromResultSet(resultSet, tableRow);
				rowUpdated = true;
			}
			if (rowUpdated == false) {
				throw new RuntimeException("Row was NOT updated, not present in the database: " + tableRow.toString());
			}
			
		} catch (SQLException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			System.out.println("Executed statement:");
			System.out.println(sql);
			e.printStackTrace();
		}
	}
	
	public synchronized ArrayList<? extends SQLiteTableRow> getAllRows(String tableName, Class<? extends SQLiteTableRow> clazz) {
		String sql = "SELECT * FROM " + tableName;
		ArrayList<SQLiteTableRow> result = new ArrayList<>();
		ResultSet resultSet = executeQuery(sql);
		try {
			while (resultSet.next()) {
				SQLiteTableRow row = SQLiteTableRow.fromResultSet(resultSet, clazz);
				result.add(row);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	
	/**
	 * Deletes using all non-null attributes in the primary key
	 * @param tableRow
	 */
	public synchronized void deleteRow(SQLiteTableRow tableRow) {
		String sql = tableRow.getDeleteStatement();
		executeStatement(sql);
	}
	
}



