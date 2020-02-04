package com.sap.charging.realTime.reinforcementLearning.neuralNetwork;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.realTime.model.PowerAssignment;
import com.sap.charging.realTime.reinforcementLearning.neuralNetwork.util.Vis;

public class Critic extends NeuralNetwork {
	
	private static final int nInputs = 9;
	private static final int nHiddenNodes = 20;
	private static final int nOutputs = 1;
	
	public Critic() {
		super();
		Vis.getInstance().addNet(model);
	}
	
	/**
	 * For critic: beta
	 */
	public static final double learningRate = 0.001;
	
	public double getValueApproximation(PowerAssignment powerAssignment) {
		INDArray predicted = this.getOutput(powerAssignment);
		return predicted.getDouble(0);
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
	 * 7) Update via back propagation based on tdError
	 * @param tdError
	 */
	public void update(PowerAssignment powerAssignment, double valueTarget) {
		log(2, "Updating critic with valueTarget=" + valueTarget); // + " and carAssignment=" + powerAssignment);
		INDArray data = stateToINDArray(powerAssignment);
		INDArray label = Nd4j.create(new double[]{valueTarget});

		if (valueTarget < 0) {
			log(1, "Updating critic with negative reward: " + data.toString() + "; " + label.toString());
		}
		this.updateModel(data, label);
	}

	
	
	
	@Override
	public INDArray stateToINDArray(PowerAssignment powerAssignment) {
		Car car = powerAssignment.car;
		ChargingStation chargingStation = powerAssignment.chargingStation;
		double maxPower = Math.min(chargingStation.fusePhase1, car.maxCurrentPerPhase);
		// How far is assigned power over allowed power limit?
		double overLimit = Math.max(0, powerAssignment.phase1 - maxPower); 
		
		// How far is assigned power under allowed power limit
		double underLimit = (powerAssignment.phase1 == 0) ? 0 :
							Math.max(0, car.minCurrentPerPhase - powerAssignment.phase1);
		
		double[] data = new double[]{
				// Static parameters
				//chargingStation.fusePhase1,
				Math.max(overLimit, underLimit),
				car.canLoadPhase1 == 1 ? 1 : -1,
				car.canLoadPhase2 == 1 ? 1 : -1,
				car.canLoadPhase3 == 1 ? 1 : -1,
				//car.maxCurrentPerPhase,
				//car.minCurrentPerPhase,
				car.isFullyCharged() ? 1 : -1,
				//car.minLoadingState,
				
				// Dynamic parameters
				car.getMissingCapacity()
				
				// Action
				, powerAssignment.phase1,
				powerAssignment.phase2,
				powerAssignment.phase3
				
				// Will introduce later
				//, car.isImmediateStartNeeded() ? 1 : 0
				//, car.isSuspendable() ? 1 : 0
				//, car.canUseVariablePower() ? 1 : 0 
		};
		return Nd4j.create(data);
	}
	
}
