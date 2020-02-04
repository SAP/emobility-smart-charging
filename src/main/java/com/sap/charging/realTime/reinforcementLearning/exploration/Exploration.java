package com.sap.charging.realTime.reinforcementLearning.exploration;

import com.sap.charging.realTime.model.PowerAssignment;
import com.sap.charging.realTime.reinforcementLearning.model.Action;

public abstract class Exploration {
	
	public abstract boolean isExplorativeTimestep(int t);

	public abstract Action generateExplorativeAction(PowerAssignment powerAssignment);
	
}
