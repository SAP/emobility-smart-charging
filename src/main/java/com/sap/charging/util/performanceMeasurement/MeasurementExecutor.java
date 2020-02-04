package com.sap.charging.util.performanceMeasurement;

import java.util.function.Consumer;

import com.sap.charging.util.Loggable;
import com.sap.charging.util.sqlite.SQLiteDB;

public abstract class MeasurementExecutor<PerformanceMeasurementType extends PerformanceMeasurement<PerformanceMeasurementType>> 	
	implements Consumer<PerformanceMeasurementType>, Loggable {

	protected final SQLiteDB db;
	
	protected final boolean forceMeasurement;
	
	public MeasurementExecutor(SQLiteDB db) {
		this(db, false);
	}

	public MeasurementExecutor(SQLiteDB db, boolean forceMeasurement) {
		this.db = db;
		this.forceMeasurement = forceMeasurement;
	}
	
	@Override
	public int getVerbosity() {
		return 2;
	}
	
}
