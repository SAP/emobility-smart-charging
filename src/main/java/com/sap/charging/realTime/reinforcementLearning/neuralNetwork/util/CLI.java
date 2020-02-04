package com.sap.charging.realTime.reinforcementLearning.neuralNetwork.util;

import java.util.Scanner;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import com.sap.charging.realTime.reinforcementLearning.neuralNetwork.Actor;
import com.sap.charging.realTime.reinforcementLearning.neuralNetwork.Critic;
import com.sap.charging.realTime.reinforcementLearning.neuralNetwork.NeuralNetwork;

public class CLI {
	
	/*@Override
	public int getVerbosity() {
		return 0;
	}*/
	
	private NeuralNetwork selectedNet = null;
	
	
	public void listen(Actor actor, Critic critic) {
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
		while (true) {
			if (selectedNet == null) {
				System.out.println("1) Enter 'actor' or 'critic' to select model: ");
				String modelString = scanner.nextLine();
				switch(modelString.toLowerCase()) {
					case "actor": selectedNet = actor; break;
					case "critic": selectedNet = critic; break;
					default: 
						System.out.println("Please enter 'actor' or 'critic'."); 
						continue;
				}
			}
			
			System.out.println("1) Selected '" + selectedNet.getClass().getSimpleName().toLowerCase() + "'.");
			
			System.out.println("2) Enter state as an array, e.g. [0.00, 1, 0.2, 16, ...], or 'back' to go back to model selection. "
					+ "Expected length: " + selectedNet.getNInputs());
			String arrayString = scanner.nextLine();
			if (arrayString.equals("back")) {
				selectedNet = null;
				continue;
			}
			
			arrayString = arrayString.trim(); // get rid of spaces
			try {
				arrayString = arrayString.substring(1, arrayString.length()-1); // get rid of []
			}
			catch (Exception e) {
				e.printStackTrace();
				System.out.println("Error during array string creation.");
				continue;
			}
			
			String arrayParts[] = arrayString.split(",");
			double arrayDouble[] = new double[arrayParts.length];
			for (int i=0;i<arrayParts.length;i++) {
				String arrayPartNumber = arrayParts[i].trim();
				//System.out.println("i=" + i + ", arrayPartNumber=" + arrayPartNumber);
				try {
					arrayDouble[i] = Double.valueOf(arrayPartNumber);
				}
				catch (Exception e) {
					e.printStackTrace();
					System.out.println("Invalid number entered at i=" + i + ": " + arrayPartNumber);
					continue;
				}
			}
			INDArray state = Nd4j.create(arrayDouble);
			System.out.println("2) Input: " + state);
			
			System.out.println("3) Outputs");
			try {
				System.out.println(selectedNet.getOutput(state));
			}
			catch (Exception e) {
				e.printStackTrace();
				System.out.println("Error getting output.");
				continue;
			}
			
		}
		
		
		
	}

}
