package com.sap.charging.util.random;

import java.util.Random;

public class UniformDistribution extends Distribution {
	
	private final double min;
	private final double max;
	
	public UniformDistribution(Random random, double min, double max) {
		super(random);
		this.min = min;
		this.max = max;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}
	
	public double getNextDouble() {
		return (this.getRandom().nextDouble()*(max-min))+min;
	}

	@Override
	public Distribution clone(Random random) {
		return new UniformDistribution(random, getMin(), getMax());
	}
	
}
