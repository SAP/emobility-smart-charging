package com.sap.charging.realTime.reinforcementLearning.exploration;

import java.util.Random;

public abstract class ExplorationEpsilonGreedy extends Exploration {
	
	private Random random;
	private final double epsilon_min = 0.5;
	/**
	 * Small values lead to more exploration (since higher probability)
	 */
	private final double exponentialDecayFactor = 0.0001;
	
	public ExplorationEpsilonGreedy() {
		this.random = new Random(0);
	}

	public double getNextRandom() {
		return this.random.nextDouble();
	}
	public double getNextRandom(double min, double max) {
		return getNextRandom() * (max-min) + min;
	}
	
	public double getEpsilon(int t) {
		return Math.max(epsilon_min, 
						Math.pow(Math.E, -exponentialDecayFactor*t));
	}
	
	public boolean isExplorativeTimestep(int t) {
		return getNextRandom(0, 1) < getEpsilon(t) 
			   ? true : false;
	}
	
	
}
