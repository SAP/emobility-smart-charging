package com.sap.charging.util.random;

import java.util.Random;

public abstract class Distribution {

	private final Random random;
	
	public Distribution(Random random) {
		this.random = random;
	}
	
	public Random getRandom() {
		return random;
	}
	
	public abstract Distribution clone(Random random);
	
	public abstract double getNextDouble();
	
}
