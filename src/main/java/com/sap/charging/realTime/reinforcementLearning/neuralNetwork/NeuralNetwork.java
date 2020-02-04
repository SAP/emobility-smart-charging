package com.sap.charging.realTime.reinforcementLearning.neuralNetwork;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import com.sap.charging.realTime.model.PowerAssignment;
import com.sap.charging.realTime.reinforcementLearning.CACLA;
import com.sap.charging.util.Loggable;

public abstract class NeuralNetwork implements Loggable {
	
	public int getVerbosity() {
		return CACLA.verbosity;
	}
	
	protected MultiLayerConfiguration conf;
	protected MultiLayerNetwork model;
	
	private INDArray lastTrainingInput;
	private INDArray lastTrainingLabel;
	
	public NeuralNetwork() {
		log(1, "Constructor with nInputs=" +getNInputs() + ", nHiddenNodes=" + getNHiddenNodes() + ", nOutputs=" + getNOutputs() + "...");
		conf = new NeuralNetConfiguration.Builder()
				.seed(0)
				//.iterations(1) // number of parameter updates in a row, for each minibatch
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
			
				//.learningRate(getLearningRate())
				
				//.updater(Updater.NESTEROVS)
				
				.list()
				.layer(0,  new DenseLayer.Builder().nIn(getNInputs()).nOut(getNHiddenNodes())
						.weightInit(WeightInit.ZERO) // XAVIER
						.activation(Activation.SIGMOID)
						.build())
				.layer(1, new OutputLayer.Builder(LossFunction.MSE).nIn(getNHiddenNodes()).nOut(getNOutputs())
						.weightInit(WeightInit.ZERO)
						.activation(Activation.IDENTITY) // linear activation
						.build()) // MSE is usual loss function for regression problems
				.pretrain(false) // no idea
				.backprop(true)
				.build();
		
		model = new MultiLayerNetwork(conf);
        model.init();
        //print the score with every 1 iteration
        //model.addListeners(new ScoreIterationListener(1));
	}
	
	public abstract double getLearningRate();
	public abstract int getNInputs();
	public abstract int getNHiddenNodes();
	public abstract int getNOutputs();
	
	
	public MultiLayerNetwork getModel() {
		return this.model;
	}
	
	public void updateModel(INDArray data, INDArray labels) {
		lastTrainingInput = data;
		lastTrainingLabel = labels;
		this.model.fit(data, labels);
	}
	
	public void printModel() {
		log(0, this.model.summary());
		for (String paramArrayName : this.model.paramTable().keySet()) {
			INDArray array = this.model.paramTable().get(paramArrayName);
			log(0, paramArrayName + "=" + array, false);
		}
		log(0, "Last input:");
		INDArray lastTraniningInput = getLastTrainingInput();
		INDArray lastTrainingLabel = getLastTrainingLabel();
		log(0, lastTraniningInput.toString() + "; " + lastTrainingLabel);
		
		//System.out.println(critic.getModel().params());
		//Layer l0 = model.getLayer(0); 
		//l0.para
	}
	
	/**
	 * 
	 * @param state
	 * @return
	 */
	public abstract INDArray stateToINDArray(PowerAssignment powerAssignment);
	
	public INDArray getOutput(INDArray data) {
		return model.output(data);
	}
	
	public INDArray getLastTrainingInput() {
		return lastTrainingInput;
	}
	public INDArray getLastTrainingLabel() {
		return lastTrainingLabel;
	}
	
	public INDArray getOutput(PowerAssignment powerAssignment) {
		INDArray data = stateToINDArray(powerAssignment);
		INDArray predicted = getOutput(data);
		return predicted;
	}
	
}
