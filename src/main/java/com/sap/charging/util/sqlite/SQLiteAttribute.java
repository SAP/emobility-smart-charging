package com.sap.charging.util.sqlite;

public class SQLiteAttribute {
	
	private final Object value;
	public final String name;
	public final String dataType;
	public final boolean isKey;
	public final boolean isNotNull;
	
	public SQLiteAttribute(Object value, String name, String dataType, boolean isKey, boolean isNotNull) {
		this.value = value;
		this.name = name;
		this.dataType = dataType;
		this.isKey = isKey;
		this.isNotNull = isNotNull;
	}

	public String getValueString() {
		if (value == null) 
			return "null";
		else if (dataType.equals("TEXT")) // Strings
			return "'" + value.toString() + "'";
		else if (dataType.equals("BOOLEAN")) 
			return (boolean) value ? "1" : "0";
		else 
			return value.toString();
	}

	public Object getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return "Name=" + name + ", value=" + value + ", dataType=" + dataType + ", isKey=" + isKey + ", isNotNull=" + isNotNull;
	}
	
}
