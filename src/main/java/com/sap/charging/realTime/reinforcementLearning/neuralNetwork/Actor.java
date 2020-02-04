package com.sap.charging.realTime.reinforcementLearning.neuralNetwork;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.realTime.model.PowerAssignment;
import com.sap.charging.realTime.reinforcementLearning.model.Action;

public class Actor extends NeuralNetwork {

	private static final int nInputs = 7;
	private static final int nHiddenNodes = 20;
	private static final int nOutputs = 1;
	
	/**
	 * For actor: alpha
	 */
	public static final double learningRate = 0.001;
	
	public Actor() {
		super();
	}
	
	public Action getActionApproximation(PowerAssignment powerAssignment) {
		Car car = powerAssignment.car;
		INDArray predicted = this.getOutput(powerAssignment);
		double power = predicted.getDouble(0);
		return new Action(car.canLoadPhase1*power,
				car.canLoadPhase2*power,
				car.canLoadPhase3*power);
	}


	@Override
	public double getLearningRate() {
		return learningRate;
	}
	
	@Override
	public int getNInputs() {
		return nInputs;
	}
	
	@Override
	public int getNHiddenNodes() {
		return nHiddenNodes;
	}
	
	@Override
	public int getNOutputs() {
		return nOutputs;
	}

	/**
	 * 9) Update actor 
	 * @param chosenAction
	 * @param currentState
	 */
	public void update(PowerAssignment powerAssignment, Action chosenAction) {
		// A(s)
		//Action currentPredictedAction = this.getActionApproximation(carAssignment); 
		
		// label = chosenAction - current output of actor
		// label = a - Ac(s)
		//double label = chosenAction.phase1 - currentPredictedAction.phase1;
		
		// label = a
		INDArray state = stateToINDArray(powerAssignment);
		double label = chosenAction.phase1;
		log(2, "Updating actor  with label=" + label + 
			   " and state=" + state);
		//log(2, "Actor::update Updating actor with label=" + label +
		//				"=a-Ac(s)=" + chosenAction.phase1 + "-" + 
		//				currentPredictedAction.phase1);
		
		INDArray labels = Nd4j.create(new double[] {label});
		this.updateModel(state, labels);
	}

	@Override
	public INDArray stateToINDArray(PowerAssignment powerAssignment) {
		Car car = powerAssignment.car;
		ChargingStation chargingStation = powerAssignment.chargingStation;
		double[] data = new double[]{
				// Static parameters
				chargingStation.fusePhase1,
				car.canLoadPhase1 == 1 ? 1 : -1,
				car.canLoadPhase2 == 1 ? 1 : -1,
				car.canLoadPhase3 == 1 ? 1 : -1,
				car.maxCurrentPerPhase,
				car.minCurrentPerPhase,
				car.isFullyCharged() ? 1 : -1
				//car.minLoadingState,
				
				// Dynamic parameters
				///, car.getMissingCapacity()
				
				// Will introduce later
				//, car.isImmediateStartNeeded() ? 1 : 0
				//, car.isSuspendable() ? 1 : 0
				//, car.canUseVariablePower() ? 1 : 0 
		};
		return Nd4j.create(data);
	}
	
	
}
