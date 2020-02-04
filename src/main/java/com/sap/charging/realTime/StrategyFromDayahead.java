package com.sap.charging.realTime;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.opt.lp.Variable;
import com.sap.charging.opt.solution.model.DayaheadSchedule;

public class StrategyFromDayahead extends Strategy {
	
	private DayaheadSchedule schedule;
	
	public StrategyFromDayahead(DayaheadSchedule schedule) {
		this.schedule = schedule;
	}
	
	@Override
	public void reactCarArrival(State state, Car car) {
		int n = car.getId(); // n is car ID
		Variable variable = schedule.getVariableX(n);
		
		if (variable == null)
			// If variable is null, no variableX for car n was found
			return;
		int i = variable.getIndex("i"); // i is charging station ID
		ChargingStation chargingStation = state.getFreeChargingStation(i);
		if (chargingStation != null) {
			state.addCarAssignment(car, chargingStation);

			// Set car charging plan according to P
			// In practice, the complete plan for i is set
			// Get all P variables for this charging station and phase=1
			
			double[] currentPlan = schedule.getChargingStationPlan(i);
			double maxAllowed = Math.min(chargingStation.fusePhase1, car.maxCurrentPerPhase);
			// Convert this discrete plan to plan for chargingStation
			// Check if minCurrentPerPhase is being kept
			for (int k=0;k<currentPlan.length;k++) {
				
				if (currentPlan[k] > 0 && currentPlan[k] < car.minCurrentPerPhase) {
					log(2, "WARNING: currentPlan[" + k + "]=" + currentPlan[k] + " does not regard minCurrentPerPhase=" + car.minCurrentPerPhase + 
							". Using minCurrentPerPhase=" + car.minCurrentPerPhase + " instead (delta=" + (currentPlan[k]-car.minCurrentPerPhase) + ").");
					currentPlan[k] = car.minCurrentPerPhase;
				}
				if (currentPlan[k] > maxAllowed && currentPlan[k] < maxAllowed+1e-6) {
					log(2, "WARNING: currentPlan[" + k + "]=" + currentPlan[k] + " above maxAllowed by small delta, correcting to maxAllowed=" + maxAllowed +
							"( delta=" + (currentPlan[k]-maxAllowed) + ").");
					currentPlan[k] = maxAllowed;
				}
			}
			
			car.setCurrentPlan(currentPlan);
		}
	}

	@Override
	public void reactCarFinished(State state, Car carFinished) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reactCarDeparture(State state, Car carLeaving) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reactEnergyPriceChange(State state, EnergyPriceHistory newEnergyPriceHistory) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public String getMethod() {
		return getMethodStatic();
	}
	
	public static String getMethodStatic() {
		return "realTimeFromDayAhead";
	}
	
}
