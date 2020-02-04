package com.sap.charging.realTime.reinforcementLearning.model;

public class Agent {

	private final Policy policy;	
	
	public Agent(Policy policy) {
		this.policy = policy;
	}
	
	public Policy getPolicy() {
		return this.policy;
	}
	
}
