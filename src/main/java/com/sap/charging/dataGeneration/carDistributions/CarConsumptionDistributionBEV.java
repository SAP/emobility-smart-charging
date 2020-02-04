package com.sap.charging.dataGeneration.carDistributions;

public class CarConsumptionDistributionBEV {

	/**
	 *  Wh
	 */
	public static final double binSize = 1000; 
	
	/**
	 * Insert your consumption data here: Each cell of the matrix is the probability of being selected. The dimensions in the 2nd array can be of differing length. 
	 * For example, distribution[0][5] gives the probability of an EV charging 5kWh on a Monday. 
	 */
	public static final double[][] distribution = {
			{000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000},
			{000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000},
			{000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000},
			{000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000},
			{000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000, 000000000}
		};
	
}
