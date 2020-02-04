package com.sap.charging.realTime.reinforcementLearning.exploration;

import com.sap.charging.realTime.model.PowerAssignment;
import com.sap.charging.realTime.reinforcementLearning.model.Action;

public class ExplorationEpsilonGreedyRandom extends ExplorationEpsilonGreedy {
	
	
	/**
	 * Generate value between 0 and max_number
	 * On 1, 2 or 3 phases
	 */
	@Override
	public Action generateExplorativeAction(PowerAssignment powerAssignment) {
		double howManyPhases = 0; //getNextRandom();
		int usePhase2 = 0;
		int usePhase3 = 0;
		if (howManyPhases > 0.333) {
			usePhase2 = 1;
		}
		if (howManyPhases > 0.666) {
			usePhase3 = 1;
		}
		double power = getNextRandom(3, 18);
		return new Action(power, usePhase2*power, usePhase3*power);
	}
	
}
