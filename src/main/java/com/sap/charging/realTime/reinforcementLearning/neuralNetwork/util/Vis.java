package com.sap.charging.realTime.reinforcementLearning.neuralNetwork.util;

import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;

public class Vis {
	
	private UIServer uiServer;
	private StatsStorage statsStorage; 
	
	private static Vis instance;
	
	private Vis() {
		uiServer = UIServer.getInstance();
		statsStorage = new InMemoryStatsStorage(); //Alternative: new FileStatsStorage(File), for saving and loading later
		uiServer.attach(statsStorage);
	}
	
	public void addNet(MultiLayerNetwork net) {
		net.addListeners(new StatsListener(statsStorage));
	}
	
	public static Vis getInstance() {
		if (instance==null) {
			instance = new Vis();
		}
		return instance;
	}
	
	
}
