package com.sap.charging.util.random;

import java.util.Random;

public class ConstantDistribution extends Distribution {
	
	private final double constant;
	
	public ConstantDistribution(Random random, double constant) {
		super(random);
		this.constant = constant;
	}

	public double getConstant() {
		return constant;
	}
	
	public double getNextDouble() {
		return constant;
	}

	@Override
	public Distribution clone(Random random) {
		return new ConstantDistribution(random, constant);
	}
	
}
