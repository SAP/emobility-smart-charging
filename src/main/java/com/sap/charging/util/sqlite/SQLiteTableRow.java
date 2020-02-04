package com.sap.charging.util.sqlite;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class SQLiteTableRow {
	
	@SQLiteAttributeIgnore
	private ArrayList<SQLiteAttribute> attributes;
	
	public SQLiteTableRow() {
	}
	
	public abstract String getTableName();
	
	private ArrayList<SQLiteAttribute> buildAttributes() throws IllegalArgumentException, IllegalAccessException {
		ArrayList<SQLiteAttribute> result = new ArrayList<>();
		Field[] fields = this.getClass().getFields();
		
		for (Field field : fields) {
			
			boolean ignoreThis = field.isAnnotationPresent(SQLiteAttributeIgnore.class);
			if (ignoreThis == true)
				continue;
			
			boolean isKey = field.isAnnotationPresent(SQLiteAttributeKey.class);
			boolean isNotNull = field.isAnnotationPresent(SQLiteAttributeNotNull.class);
			
			Class<?> attributeClass = field.getType();
			String name = field.getName();

			SQLiteAttribute attribute = new SQLiteAttribute(
					field.get(this),
					name, 
					this.getSQLiteDataType(attributeClass),
					isKey, 
					isNotNull);
			
			result.add(attribute);
		}
		return result;
	}
	
	private ArrayList<SQLiteAttribute> getAttributes() {
		if (this.attributes == null) {
			try {
				this.attributes = buildAttributes();
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		return this.attributes;
	}
	
	private void resetAttributesList() {
		this.attributes = null;
	}
	
	protected String getSQLiteDataType(Class<?> attributeClass) {
		if (attributeClass == Integer.class || attributeClass == int.class) {
			return "INTEGER";
		}
		if (attributeClass == Double.class || attributeClass == double.class) {
			return "REAL";
		}
		if (attributeClass == String.class) {
			return "TEXT";
		}
		if (attributeClass == boolean.class || attributeClass == Boolean.class) {
			return "BOOLEAN";
		}
		
		throw new NullPointerException("No class defined for attributeClass: " + attributeClass);
	}
	
	
	public String getCreateTableStatement() {
		ArrayList<SQLiteAttribute> attributes = this.getAttributes();
		
		String result = "CREATE TABLE " + getTableName() + "(\n";
		
		// Column definitions
		for (int i=0;i<attributes.size();i++) {
			SQLiteAttribute attribute = attributes.get(i);
			result += "\t" + attribute.name + " " + attribute.dataType;
			if (attribute.isNotNull)
				result += " NOT NULL";
			
			if (i != attributes.size()-1)
				result += ", \n";
		}
		
		// Primary key 
		boolean isFirst = true;
		List<SQLiteAttribute> primaryKeyAttributes = attributes.stream().filter(a -> a.isKey).collect(Collectors.toList());
		for (int i=0;i<primaryKeyAttributes.size();i++) {
			SQLiteAttribute attribute = primaryKeyAttributes.get(i);
			
			if (isFirst == true) {
				result += ", \n" + "\t" + "PRIMARY KEY (";
				isFirst = false;
			}
			result += attribute.name;
			
			if (i != primaryKeyAttributes.size()-1)
				result += ", ";
		}
		if (isFirst == false) { // Cheeck if any were added
			result += ")";
		}
		
		
		result += "\n);";
		return result;
	}

	public String getInsertStatement() {
		
		resetAttributesList();
		ArrayList<SQLiteAttribute> attributes = this.getAttributes();
		
		String result = "INSERT INTO " + getTableName() + " (";
		
		// Add list of columns
		for (int i=0;i<attributes.size();i++) {
			SQLiteAttribute attribute = attributes.get(i);
			result += attribute.name;
			
			if (i != attributes.size()-1)
				result += ", ";
		}
				
		result += ")\nVALUES (";
		for (int i=0;i<attributes.size();i++) {
			SQLiteAttribute attribute = attributes.get(i);
			result += attribute.getValueString();
			
			if (i != attributes.size()-1)
				result += ", ";
		}
		result += ");";
		return result;
	}
	
	
	/**
	 * All non-null attributs are searched for that are in the primary key
	 * @return
	 */
	public String getSelectRowsStatement() {
		List<SQLiteAttribute> primaryKeyAttributes = getAttributes().stream().filter(a -> a.isKey && a.getValue() != null).collect(Collectors.toList());
		
		String result = "SELECT * FROM " + getTableName() + " WHERE \n";
		
		for (int i=0;i<primaryKeyAttributes.size();i++) {
			SQLiteAttribute attribute = primaryKeyAttributes.get(i);
			
			if (attribute.getValue() != null) {
				result += "\t" + attribute.name + "=" + attribute.getValueString();
			}
			
			if (i != primaryKeyAttributes.size()-1) // If not last attribute, add " AND "
				result += " AND\n";
			//System.out.println("i=" + i + ", attribute=" + attribute.toString());
		}
		return result;
	}
	
	/**
	 * Uses all attributes and must match to be deleted
	 * @return
	 */
	public String getDeleteStatement() {
		List<SQLiteAttribute> primaryKeyAttributes = getAttributes().stream().filter(a -> a.isKey && a.getValue() != null).collect(Collectors.toList());
		
		String result = "DELETE FROM " + getTableName() + " WHERE \n";
		
		for (int i=0;i<primaryKeyAttributes.size();i++) {
			SQLiteAttribute attribute = primaryKeyAttributes.get(i);
			
			if (attribute.getValue() != null) {
				result += "\t" + attribute.name + "=" + attribute.getValueString();
			}
			
			if (i != primaryKeyAttributes.size()-1)
				result += " AND\n";
		}
		return result;
	}
	
	
	
	@Override
	public final String toString() {
		String result = this.getClass().getSimpleName() + ": "; // "SQLiteTableRow: ";
		for (int i=0;i < this.getAttributes().size(); i++) {
			SQLiteAttribute attribute = this.getAttributes().get(i);
			
			String valueString = attribute.getValue() != null ? attribute.getValueString() : "null";
			result += attribute.name + "=" + valueString;
			
			if (i != this.getAttributes().size()-1) {
				result += ", ";
			}
		}
		return result;
	}
	
	

	/**
	 * OriginalRow will be changed (even final fields), call by reference
	 * @param resultSet Should already be at the correct row (e.g. resultSet.next() has already been called)
	 * @param originalRow
	 * @return
	 * @throws SQLException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws SecurityException 
	 * @throws NoSuchFieldException 
	 */
	public static void fromResultSet(ResultSet resultSet, SQLiteTableRow originalRow) throws SQLException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		ResultSetMetaData metaData = resultSet.getMetaData();
		int nColumns = metaData.getColumnCount(); 
		
		//ArrayList<SQLiteAttribute> attributes = originalRow.getAttributes();
		
		Field[] fields = originalRow.getClass().getFields();
		for (Field field : fields) {
			
			boolean ignoreThis = field.isAnnotationPresent(SQLiteAttributeIgnore.class);
			if (ignoreThis == true)
				continue;
			
			// Need to map from SQLite resultset column names to file names
			int resultSetColumnIndex = -1;
			for (int i=1;i<nColumns+1;i++) { // Column indexes are 1-indexed
				if (metaData.getColumnName(i).equals(field.getName())) {
					resultSetColumnIndex = i;
					break;
				}
			}
			
			//Field modifiersField = Field.class.getDeclaredField("modifiers");
			//modifiersField.setAccessible(true);
			//modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
			
			field.setAccessible(true); // Set to non-final 
			Object fieldValue = resultSet.getObject(resultSetColumnIndex);
			
			
			
			field.set(originalRow, fieldValue);
			//System.out.println(field + "=" + fieldValue + ", isAccessible=" + field.isAccessible() + ", field.getValue=" + field.get(originalRow));
			//field.setAccessible(false); // Reset back to final
			
		}
		originalRow.resetAttributesList();
	}
	
	
	/**
	 * resultSet.next() should already have been called
	 * @param resultSet
	 * @param clazz
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws SQLException 
	 */
	@SuppressWarnings("unchecked")
	public static SQLiteTableRow fromResultSet(ResultSet resultSet, Class<? extends SQLiteTableRow> clazz) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, SQLException {
		ResultSetMetaData metaData = resultSet.getMetaData();
		int nColumns = metaData.getColumnCount(); 
		
		Constructor<?>[] constructors =  clazz.getConstructors();

		Constructor<? extends SQLiteTableRow> constructor = null; 
		for (Constructor<?> constructorLoop : constructors) { // Make sure to get constructor with 0 parameters
			if (constructorLoop.getParameterTypes().length == 0) 
				constructor = (Constructor<? extends SQLiteTableRow>) constructorLoop;
			
		}
		SQLiteTableRow resultRow = constructor.newInstance();
		
		
		Field[] fields = clazz.getFields();
		for (Field field : fields) {
			
			boolean ignoreThis = field.isAnnotationPresent(SQLiteAttributeIgnore.class);
			if (ignoreThis == true)
				continue;
			
			// Need to map from SQLite resultset column names to file names
			int resultSetColumnIndex = -1;
			for (int i=1;i<nColumns+1;i++) { // Column indexes are 1-indexed
				if (metaData.getColumnName(i).equals(field.getName())) {
					resultSetColumnIndex = i;
					break;
				}
			}
			if (resultSetColumnIndex == -1) {
				throw new NullPointerException("Column not found, columnIndex=-1: " + field.getName());
			}
			
			
			field.setAccessible(true); // Set to non-final 
			Object fieldValue = resultSet.getObject(resultSetColumnIndex);
			
			field.set(resultRow, fieldValue);
		}
		resultRow.resetAttributesList();
		return resultRow;
	}
	
	
	
	
	
}
