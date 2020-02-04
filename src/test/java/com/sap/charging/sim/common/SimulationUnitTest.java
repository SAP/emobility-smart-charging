package com.sap.charging.sim.common;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.common.DefaultDataGenerator;
import com.sap.charging.realTime.State;
import com.sap.charging.realTime.Strategy;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.sim.Simulation;

public abstract class SimulationUnitTest {

	protected DataGenerator dataSim;
	protected DataGenerator dataSimToy; 
	
	protected Simulation sim;
	protected State state; 
	protected Strategy strategy;
	
	public SimulationUnitTest() {
		this.dataSim = DefaultDataGenerator.getDefaultDataGenerator();
		this.dataSimToy = DefaultDataGenerator.getToyDataGenerator(); 
	}
	
	public void initSimulation(DataGenerator data) {
		this.strategy = new StrategyAlgorithmic(); 
		this.sim = new Simulation(data, this.strategy); 
		this.sim.init(); 
		this.state = this.sim.getState(); 
	}
	
	
	
	
	
}
