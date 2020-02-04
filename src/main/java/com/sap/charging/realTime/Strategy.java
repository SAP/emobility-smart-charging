package com.sap.charging.realTime;

import com.sap.charging.model.Car;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.sim.Simulation;
import com.sap.charging.sim.event.Event;
import com.sap.charging.sim.event.EventCarArrival;
import com.sap.charging.sim.event.EventCarDeparture;
import com.sap.charging.sim.event.EventCarFinished;
import com.sap.charging.sim.event.EventEnergyPriceChange;
import com.sap.charging.sim.event.EventReoptimize;
import com.sap.charging.util.Loggable;

/**
 * A strategy is responsible for assigning cars and creating a 
 * power consumption schedule in real time. 
 * 
 * Inputs:
 * day-ahead schedule
 * current state (t=0)
 * new car (n0)
 * 
 * Output:
 * new state (t=1)
 */
public abstract class Strategy implements Loggable{
	
	public int getVerbosity() {
		return Simulation.verbosity;
	}
	
	// Called once per simulation iteration (e.g. new car, new energy prices)
	// ==> min every 15 minutes
	public final void react(State currentState, Event event) {
		int minVerbosity = (event instanceof EventEnergyPriceChange) ? 1 : 2; // First 2 generates less messages
		log(minVerbosity, "Reacting to " + event.getClass().getSimpleName()
				+ " at t=" + event.getSecondsOfDay() + " (k=" + currentState.currentTimeslot + ")");
		
		if (event instanceof EventCarArrival) {
			reactCarArrivalWrapper(currentState, ((EventCarArrival) event).car);
		}
		if (event instanceof EventCarFinished) {
			reactCarFinishedWrapper(currentState, ((EventCarFinished) event).car);
		}
		if (event instanceof EventCarDeparture) {
			reactCarDepartureWrapper(currentState, ((EventCarDeparture) event).car);
		}
		if (event instanceof EventEnergyPriceChange) {
			reactEnergyPriceChangeWrapper(currentState, ((EventEnergyPriceChange) event).history);
		}
		if (event instanceof EventReoptimize) {
			reactReoptimizeWrapper(currentState); 
		}
	}
	
	// Called when a car arrives
	public final void reactCarArrivalWrapper(State state, Car newCar) {
		this.reactCarArrival(state, newCar);
		
		if (state.isCarCurrentlyAssigned(newCar) == false) {
			// If car was not assigned, add to unassigned cars
			state.addUnassignedCar(newCar);
		}
		
	}
	public abstract void reactCarArrival(State state, Car newCar);
	
	
	// Called when a car finishes charging
	public final void reactCarFinishedWrapper(State state, Car car) {
		if (state.isCarPowerAssigned(car)) {
			state.removePowerAssignment(car);
		}
		
		this.reactCarFinished(state, car);
	}
	public abstract void reactCarFinished(State state, Car carFinished);
	
	
	
	
	// Called when a car departs
	public final void reactCarDepartureWrapper(State state, Car car) {
		// Remove power consumption assignment
		if (state.isCarPowerAssigned(car)) {
			state.removePowerAssignment(car);
		}
				
		// Remove car from assignments
		if (state.isCarCurrentlyAssigned(car)) {
			state.removeCarAssignment(car);
		}
		else if (state.isCarCurrentlyUnassigned(car)) {
			state.removeUnassignedCar(car);
		}
		
		this.reactCarDeparture(state, car);
	}
	public abstract void reactCarDeparture(State state, Car carLeaving);
	
	// A strategy does not HAVE to react in this case: energy price change
	public final void reactEnergyPriceChangeWrapper(State state, EnergyPriceHistory newEnergyPriceHistory) {
		/*double currentSum = state.getCurrentPowerAssignments().stream().mapToDouble(p -> p.phase1).sum() +
							state.getCurrentPowerAssignments().stream().mapToDouble(p -> p.phase2).sum() +
							state.getCurrentPowerAssignments().stream().mapToDouble(p -> p.phase3).sum() ;
		log(1, "Current sum:" + currentSum);*/
		state.setEnergyPriceHistory(newEnergyPriceHistory);
		this.reactEnergyPriceChange(state, newEnergyPriceHistory);
	}
	public abstract void reactEnergyPriceChange(State state, EnergyPriceHistory newEnergyPriceHistory);
	
	public final void reactReoptimizeWrapper(State state) {
		this.reactReoptimize(state); 
	}
	
	public void reactReoptimize(State state) {
		throw new AbstractMethodError("Strategy method=" + this.getMethod() + " must override "
				+ "reactReoptimize for eventType=reoptimize"); 
	}
	
	public abstract String getMethod();
	
}







