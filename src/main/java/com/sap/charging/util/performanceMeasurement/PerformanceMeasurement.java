package com.sap.charging.util.performanceMeasurement;

import com.sap.charging.util.sqlite.SQLiteAttributeIgnore;
import com.sap.charging.util.sqlite.SQLiteAttributeKey;
import com.sap.charging.util.sqlite.SQLiteTableRow;

public abstract class PerformanceMeasurement<SubType extends PerformanceMeasurement<SubType>> extends SQLiteTableRow  {

	@SQLiteAttributeIgnore
	public static int measurementNumber;
	@SQLiteAttributeIgnore
	public static int nMeasurements;
	
	
	
	@SQLiteAttributeKey
	public final String method;
	
	public final String guid;
	public final String filePath;
	public String dateTimeEnd;
	
	public PerformanceMeasurement() {
		this(null, null, null);
	}
	public PerformanceMeasurement(String guid, String filePath, String method) {
		this.guid = guid;
		this.filePath = filePath;
		this.method = method;
	}
	
	public abstract SubType cloneWithMethod(String method);
	
}




