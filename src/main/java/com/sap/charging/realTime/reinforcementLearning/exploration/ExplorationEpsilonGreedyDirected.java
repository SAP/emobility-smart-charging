package com.sap.charging.realTime.reinforcementLearning.exploration;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.realTime.model.PowerAssignment;
import com.sap.charging.realTime.reinforcementLearning.model.Action;

public class ExplorationEpsilonGreedyDirected extends ExplorationEpsilonGreedy {

	@Override
	public Action generateExplorativeAction(PowerAssignment powerAssignment) {
		// Use min max of car as guidance
		Car car = powerAssignment.car;
		ChargingStation chargingStation = powerAssignment.chargingStation;
		double min = car.minCurrentPerPhase;
		double max = Math.min(car.maxCurrentPerPhase, chargingStation.fusePhase1);
		double power = getNextRandom(min, max);
		//power = max;
		
		if (car.isFullyCharged()) {
			power = 0;
		}
		
		Action action = new Action(
				car.canLoadPhase1*power, 
				car.canLoadPhase2*power,
				car.canLoadPhase3*power);
		return action;
	}

}
