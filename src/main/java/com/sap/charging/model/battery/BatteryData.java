package com.sap.charging.model.battery;

import java.util.Arrays;

public abstract class BatteryData {
	
	
	
	public int getIDX(double[] x, double xi) {
		int result = Arrays.binarySearch(x, xi);
		
		// If between values: binary search returns upper index but negative
		if (result < 0) 
			return -result - 1;
		else 
			return result;
	}
	
	/**
	 * IMPORTANT: Requires equidistant x values!
	 * x[0] must be 0!
	 * @return
	 */
	public int getIDX_fast(double[] x, double xi) {
		double diffSOC = x[1] - x[0]; // Value from one x value to next. Only valid for equidistant x array
		return (int) Math.ceil(xi / diffSOC);            // for SOC(1) < 0... 03/23/10: Find indices in OCV vector
	}
	
	/**
	 * https://github.com/andrewning/jMath/blob/master/src/jMath/linalg/Array1D.java
	 * 1d linear interpolation to find yi, the approximate value of y(x) at xi.
	 * x must already be sorted in ascending order.
	 * If xi is not in bounds of interpolation it returns the closest value in y
	 * TODO: maybe it would be better to linearly extrapolate?
	 * Assumes x and y are the same size.
	 * @param x
	 * @param y
	 * @param xi
	 * @return an interpolated approximation for y(xi)
	 */
	protected double interp1(double[] x, double[] y, double xi){
		int n = x.length;
		
		int ip = getIDX_fast(x, xi); // index of x_i (if x_i is between two values, return the index of the upper value)
		
		if (ip == 0) return y[0];
		if (ip == n) return y[n-1];
		
		int im = ip - 1; // index of x_i minus one (previous value)
		return y[im] + (xi - x[im])/(x[ip] - x[im]) *  // Fraction of interval in x
				(y[ip] - y[im]);
	}
	
	
	
	public abstract double[] getOCVArray();
	public abstract double[] getOCV_SOCArray(); // Must have same length as ocv array
	
	public abstract double[] getResistanceArray();
	public abstract double[] getResistance_SOCArray(); // Must have same length as resistance array
	
	
	public abstract double getDefaultCapacity();
	public abstract ChargeAlgorithm getDefaultChargeAlgorithm();
	public abstract double getDefaultConstantCurrent();
	public abstract double getDefaultConstantPower();
	public abstract double getDefaultTerminalVoltage();
	
	
	
	public double getOCVFromSOC(double soc) {
		return interp1(getOCV_SOCArray(), getOCVArray(), soc);
	}


	public double getResistanceFromSOC(double soc) {
		//return 0.006;
		return interp1(getResistance_SOCArray(), getResistanceArray(), soc);
	}
	
	
	
}
