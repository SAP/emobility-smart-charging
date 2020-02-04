package com.sap.charging.realTime.reinforcementLearning.model;

import com.sap.charging.realTime.model.PowerAssignment;
import com.sap.charging.realTime.reinforcementLearning.CACLA;
import com.sap.charging.realTime.reinforcementLearning.exploration.Exploration;
import com.sap.charging.realTime.reinforcementLearning.neuralNetwork.Actor;
import com.sap.charging.util.Loggable;

public class Policy implements Loggable {
	
	public int getVerbosity() {
		return CACLA.verbosity;
	}
	
	private final Exploration exploration;
	
	public Policy(Exploration exploration) {
		this.exploration = exploration;
	}
	
	public Action chooseAction(PowerAssignment powerAssignment, Actor actor, int timestep) {
		Action result;
		if (exploration.isExplorativeTimestep(timestep)) {
			// Generate explorative step
			result = exploration.generateExplorativeAction(powerAssignment);
			log(2, "Generating explorative action: " + result.toString());
		}
		else {
			result = actor.getActionApproximation(powerAssignment);
			log(2, "Selecting action from actor: " + result.toString());
		}
		return result;
	}

}






