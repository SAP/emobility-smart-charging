package com.sap.charging.util.random;

import java.util.Random;

public class DiscreteDistribution extends Distribution {
	
	private final double binWidth;
	private final double[] distribution;
	
	/**
	 * Wrapper for selecting random bin of java 1D array, weighted by value
	 * @param random
	 * @param distribution
	 */
	public DiscreteDistribution(Random random, double binWidth, double[] distribution) {
		super(random);
		this.binWidth = binWidth;
		this.distribution = distribution;
	}

	@Override
	public Distribution clone(Random random) {
		return new DiscreteDistribution(random, binWidth, distribution);
	}
	
	
	private int getBinFromDistribution(double random) {
		// Iterate over distributions per bin, summing up progressively
		// until we "find" the correct bin
		double distSoFar = 0;
		
		
		for (int i=0;i<distribution.length;i++) {
			distSoFar += distribution[i];
			if (random <= distSoFar) 
				return  i;
		}
		return -1;
	}

	@Override
	public double getNextDouble() {
		double random = this.getRandom().nextDouble();
		int bin = getBinFromDistribution(random);
		double lowerValueBound = bin*binWidth; // If 1st bin is found and binWidth is 500, then this value can be between 500 and 999.9999
		
		double binRandom = this.getRandom().nextDouble(); // randomize uniformly within bin
		
		double value = lowerValueBound + binRandom*binWidth; // to get uniform distribution within each bin
		return value;
	}

}







