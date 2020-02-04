package com.sap.charging.util.random;

import java.util.Random;

public class NormalDistribution extends Distribution {
	
	private final double mean;
	private final double standardDeviation;
	
	public NormalDistribution(Random random, double mean, double standardDeviation) {
		super(random);
		this.mean = mean;
		this.standardDeviation = standardDeviation;
	}

	public double getMean() {
		return mean;
	}

	public double getStandardDeviation() {
		return standardDeviation;
	}
	
	public double getNextDouble() {
		double result = this.getRandom().nextGaussian() * getStandardDeviation() + getMean();
		return result;  
	}

	@Override
	public Distribution clone(Random random) {
		return new NormalDistribution(random, getMean(), getStandardDeviation());
	}
	
	
	
}
