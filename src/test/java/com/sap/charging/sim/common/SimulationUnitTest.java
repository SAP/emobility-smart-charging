package com.sap.charging.sim.common;

import java.time.LocalTime;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.common.DefaultDataGenerator;
import com.sap.charging.model.Car;
import com.sap.charging.model.CarFactory;
import com.sap.charging.model.CarFactory.CarModel;
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

	protected Car onePhaseCar; 
	protected Car threePhaseCar; 
	
	public SimulationUnitTest() {
		this.dataSim = DefaultDataGenerator.getDefaultDataGenerator();
		this.dataSimToy = DefaultDataGenerator.getToyDataGenerator(); 
		
		
		this.onePhaseCar = CarFactory.builder().set(CarModel.MERCEDES_GLC_350e)
    			.availableTimeslots(32, 95, 96)
    			.availableTimestamps(LocalTime.ofSecondOfDay(32*900), LocalTime.ofSecondOfDay(24*3600-1))
    			.build();
		this.onePhaseCar.setCurrentPlan(new double[96]);
		
		this.threePhaseCar = CarFactory.builder().set(CarModel.TESLA_MODEL_S)
    			.availableTimeslots(32, 95, 96)
    			.availableTimestamps(LocalTime.ofSecondOfDay(32*900), LocalTime.ofSecondOfDay(24*3600-1))
    			.build();
		this.threePhaseCar.setCurrentPlan(new double[96]);
		
	}
	
	public void initSimulation(DataGenerator data) {
		this.strategy = new StrategyAlgorithmic(); 
		this.sim = new Simulation(data, this.strategy); 
		this.sim.init(); 
		this.state = this.sim.getState(); 
	}
	
	
	
	
	
}
