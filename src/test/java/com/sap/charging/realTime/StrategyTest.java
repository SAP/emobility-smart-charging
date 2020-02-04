package com.sap.charging.realTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.sim.Simulation;
import com.sap.charging.sim.common.SimulationUnitTest;
import com.sap.charging.sim.event.EventCarArrival;
import com.sap.charging.sim.event.EventCarDeparture;
import com.sap.charging.sim.event.EventCarFinished;
import com.sap.charging.sim.event.EventEnergyPriceChange;
import com.sap.charging.sim.event.EventReoptimize;

public class StrategyTest extends SimulationUnitTest {

	State state; 
	
	@BeforeEach
	public void setup() {
		Simulation.verbosity = 0; 
		this.sim = new Simulation(dataSim, new StrategyAlgorithmic()); 
		this.sim.init(); 
		state = this.sim.getState(); 
	}



	@Test
	public void testStrategyReact() {
		
		final AtomicBoolean reactedEnergyPriceChange = new AtomicBoolean(false);
		final AtomicBoolean reactedCarFinished = new AtomicBoolean(false);
		final AtomicBoolean reactedCarDeparture = new AtomicBoolean(false);
		final AtomicBoolean reactedCarArrival = new AtomicBoolean(false);
		final AtomicBoolean reactedReoptimize = new AtomicBoolean(false);
		
		Car car = dataSim.getCar(0); 
		Car car2 = dataSim.getCar(1); 
		ChargingStation chargingStation = dataSim.getChargingStation(0); 
		
		
		Strategy strategy = new Strategy() {

			@Override
			public void reactCarArrival(State state, Car newCar) {
				reactedCarArrival.set(true);
				state.addCarAssignment(newCar, state.getFirstFreeChargingStation()); 
			}

			@Override
			public void reactCarFinished(State state, Car carFinished) {
				reactedCarFinished.set(true);
			}

			@Override
			public void reactCarDeparture(State state, Car carLeaving) {
				reactedCarDeparture.set(true);
			}

			@Override
			public void reactEnergyPriceChange(State state, EnergyPriceHistory newEnergyPriceHistory) {
				reactedEnergyPriceChange.set(true); 
			}
			
			@Override
			public void reactReoptimize(State state) {
				reactedReoptimize.set(true); 
			}

			@Override
			public String getMethod() {
				return "mock";
			}
		};

		assertEquals("mock", strategy.getMethod());

		// Check that wrapper functions are called
		// Car arrival
		state.currentTimeSeconds = car.timestampArrival.toSecondOfDay(); 
		strategy.react(state, new EventCarArrival(car.timestampArrival, car));

		// Check that car is now in list of assignments
		assertEquals(1, state.getCurrentCarAssignments().size());

		// External trigger	
		strategy.react(state, new EventEnergyPriceChange(car.timestampArrival, state.energyPriceHistory));

		// Car should still be assigned
		assertEquals(1, state.getCurrentCarAssignments().size());
		
		// Car finished
		strategy.react(state, new EventCarFinished(car.timestampArrival.plusSeconds(3600), car, chargingStation));

		// Car should still be assigned
		assertEquals(1, state.getCurrentCarAssignments().size());
		
		// Car 2 arrival
		strategy.react(state, new EventCarArrival(car2.timestampArrival, car2));

		// Check that car is now in list of assignments
		assertEquals(2, state.getCurrentCarAssignments().size());
		
		// Car departure
		strategy.react(state, new EventCarDeparture(car.timestampDeparture, car));
		

		// Car 1 should no longer be assigned
		assertEquals(1, state.getCurrentCarAssignments().size());
		assertEquals(1, state.getCurrentCarAssignments().get(0).car.getId());
		
		
		strategy.react(state, new EventReoptimize(car2.timestampDeparture));
		
		// Check that subclass methods were called
		assertTrue(reactedCarArrival.get());
		assertTrue(reactedEnergyPriceChange.get());
		assertTrue(reactedCarFinished.get());
		assertTrue(reactedCarDeparture.get());
		assertTrue(reactedReoptimize.get());

	}
}
