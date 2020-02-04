package com.sap.charging.realTime.util;

import java.util.Arrays;

public class PlannedCapacityKey {

	public double soc;
	public int currentTimeSeconds;
	public int intervalSecondsStart;
	public int intervalSecondsEnd;
	public double[] currentPlan;
	
	
	
	public PlannedCapacityKey(double soc, int currentTimeSeconds, int intervalSecondsStart, int intervalSecondsEnd,
			double[] currentPlan) {
		super();
		this.soc = soc;
		this.currentTimeSeconds = currentTimeSeconds;
		this.intervalSecondsStart = intervalSecondsStart;
		this.intervalSecondsEnd = intervalSecondsEnd;
		this.currentPlan = currentPlan;
	}
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(currentPlan);
		result = prime * result + currentTimeSeconds;
		result = prime * result + intervalSecondsEnd;
		result = prime * result + intervalSecondsStart;
		long temp;
		temp = Double.doubleToLongBits(soc);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PlannedCapacityKey other = (PlannedCapacityKey) obj;
		if (!Arrays.equals(currentPlan, other.currentPlan))
			return false;
		if (currentTimeSeconds != other.currentTimeSeconds)
			return false;
		if (intervalSecondsEnd != other.intervalSecondsEnd)
			return false;
		if (intervalSecondsStart != other.intervalSecondsStart)
			return false;
		if (Double.doubleToLongBits(soc) != Double.doubleToLongBits(other.soc))
			return false;
		return true;
	}
	
	
	
	
	
	
	
	
}
