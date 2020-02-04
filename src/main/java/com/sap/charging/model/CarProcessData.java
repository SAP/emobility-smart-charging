package com.sap.charging.model;

import java.util.ArrayList;
import java.util.List;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPString;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.r.RConnector;

public class CarProcessData {

	
	@JsonIgnore
	public static final String pathModelCarProcessDistributions = FileIO.readJSONFile("config.json").get("pathToCarProcessDistributions").toString() + "/carProcessDistributions.bin";
	@JsonIgnore
	public static final String pathScriptSampleCarProcessDistribution = FileIO.readJSONFile("config.json").get("pathToCarProcessDistributions").toString() + "/sampleCarProcessDistributions.R";
	@JsonIgnore
	public static final String pathModelDataPaperJournal2019 = FileIO.readJSONFile("config.json").get("pathToCarProcessDistributions").toString() + "/dataPaperJournal2019.bin";
	@JsonIgnore
	public static final String pathScriptSampleDataPaperJournal2019 = FileIO.readJSONFile("config.json").get("pathToCarProcessDistributions").toString() + "/sampleCars.R";
	
	/**
	 * Can be directly used as input for ML in R
	 */
	public final String oneHotEncodedRaw;
	
	public final String ID;
	public final String chargePointLabel;
	
	public final String carPark;
	public final String floor;
	public final String carType;
	public final int wday;
	public final int timestampArrival;
	public final int timestampDeparture;

	public CarProcessData(String carPark, String carType, int wday, int timestampArrival, int timestampDeparture) {
		this(null, null, null, carPark, null, carType, wday, timestampArrival, timestampDeparture);
	}
	
	public CarProcessData(String oneHotEncodedRaw, String ID, String chargePointLabel, String carPark, String floor, String carType, int wday, int timestampArrival, int timestampDeparture) {
		this.oneHotEncodedRaw = oneHotEncodedRaw;
		this.ID = ID;
		this.chargePointLabel = chargePointLabel;
		this.carPark = carPark;
		this.floor = floor;
		this.carType = carType;
		this.wday = wday;
		this.timestampArrival = timestampArrival;
		this.timestampDeparture = timestampDeparture;
	}
	
	@Override
	public String toString() {
		return "ID=" + ID + ", chargePointLabel=" + chargePointLabel + ", carPark=" + carPark + ", floor=" + floor + ", carType=" + carType + ", wday=" + wday + ", timestampArrival=" + timestampArrival + ", timestampDeparture=" + timestampDeparture;
	}
	
	
	/**
	 * Generate car process data according to many 2D probability density function 
	 * @param nCars
	 * @param seed
	 * @return
	 */
	public static List<CarProcessData> generateCarProcessData(int nCars, int seed) {
		
		RConnector rConnector = new RConnector();
		List<CarProcessData> result = new ArrayList<>();
		
		rConnector.log(1, "Sampling car process data from density for nCars=" + nCars + " via R...");
		long timeStart = System.currentTimeMillis();		
		
		// Load probability distributions
		String rCommand = "load('" + pathModelCarProcessDistributions + "')\n";
		rCommand += "source('" + pathScriptSampleCarProcessDistribution + "')\n";
		rCommand += "sampleCarProcessDistributions(" + nCars + ",seedParam=" + seed + ")\n";
		
		rCommand = rConnector.addRTryCtach(rCommand);
		
		REXP rResult = null;
		
		try {
			rResult = rConnector.evalRString(rCommand);
			//System.out.println(rResult.toDebugString());
			
			REXPString carParksR = (REXPString) rResult.asList().get(0);
			String[] carParks = carParksR.asStrings();
			
			REXPString carTypesR = (REXPString) rResult.asList().get(1);
			String[] carTypes = carTypesR.asStrings();
			
			REXPInteger wdaysR = (REXPInteger) rResult.asList().get(2);
			int[] wdays = wdaysR.asIntegers();
			
			REXPDouble timestampArrivalR = (REXPDouble) rResult.asList().get(3);
			double[] timestampArrivals = timestampArrivalR.asDoubles();
			
			REXPDouble timestampDepartureR = (REXPDouble) rResult.asList().get(4);
			double[] timestampDepartures = timestampDepartureR.asDoubles();
			
			for (int i=0;i<nCars;i++) {
				int timestampArrival = (int) timestampArrivals[i];
				int timestampDeparture = (int) timestampDepartures[i];
				if (timestampArrival > timestampDeparture) {
					timestampArrival = (int) timestampDepartures[i];
					timestampDeparture = (int) timestampArrivals[i];
				}
				
				CarProcessData carProcessData = new CarProcessData(carParks[i], carTypes[i], wdays[i], timestampArrival, timestampDeparture);
				result.add(carProcessData);
				rConnector.log(2, "Sampled: " + carProcessData.toString());
			}
			rConnector.log(1, "Finished sampling and built CarProcessData with nCars=" + nCars + " in " + (System.currentTimeMillis()-timeStart)/1000.0 + "s.");
			
		} catch (Exception e) {
			System.out.println("Executed R string:");
			System.out.println(rCommand);
			System.out.println("Result REXP:");
			System.out.println(rResult.toDebugString());
			e.printStackTrace();
		}
		
		return result;
	}
	
	
	/**
	 * Sample car process data from historical dataset (ID and stuff inclusive)
	 * @param args
	 */
	public static List<CarProcessData> sampleHistoricalCarProcesses(int nCars, int seed) {
		RConnector rConnector = new RConnector();
		List<CarProcessData> result = new ArrayList<>();
		
		rConnector.log(1, "Sampling car process data from density for nCars=" + nCars + " via R...");
		long timeStart = System.currentTimeMillis();		
		
		String rCommand = "load('" + pathModelDataPaperJournal2019 + "')\n";
		//rCommand += "install.packages('data.table')\n";
		rCommand += "source('" + pathScriptSampleDataPaperJournal2019 + "')\n";
		rCommand += "sampleCars(" + nCars + ", seedParam=" + seed + ")\n";
		
		rCommand = rConnector.addRTryCtach(rCommand);
		
		REXP rResult = null;
		
		try {
			rResult = rConnector.evalRString(rCommand);
			//System.out.println(rResult.toDebugString());
			
			REXPString oneHotEncodedRawsR = (REXPString) rResult.asList().get("one_hot_encoded");
			String[] oneHotEncodedRaws = oneHotEncodedRawsR.asStrings();
			
			REXPString IDsR = (REXPString) rResult.asList().get("ID");
			String[] IDs = IDsR.asStrings();
			
			REXPString chargePointLabelsR = (REXPString) rResult.asList().get("ChargePointLabel");
			String[] chargePointLabels = chargePointLabelsR.asStrings();
			
			
			REXPString carParksR = (REXPString) rResult.asList().get("CarPark");
			String[] carParks = carParksR.asStrings();
			
			REXPString floorsR = (REXPString) rResult.asList().get("FloorClean");
			String[] floors = floorsR.asStrings();
			
			REXPString carTypesR = (REXPString) rResult.asList().get("CarType");
			String[] carTypes = carTypesR.asStrings();
			
			REXPDouble wdaysR = (REXPDouble) rResult.asList().get("wday");
			int[] wdays = wdaysR.asIntegers();
			
			REXPDouble timestampArrivalR = (REXPDouble) rResult.asList().get("timestampArrival");
			double[] timestampArrivals = timestampArrivalR.asDoubles();
			
			REXPDouble timestampDepartureR = (REXPDouble) rResult.asList().get("timestampDeparture");
			double[] timestampDepartures = timestampDepartureR.asDoubles();
			
			for (int i=0;i<nCars;i++) {
				int timestampArrival = (int) timestampArrivals[i];
				int timestampDeparture = (int) timestampDepartures[i];

				CarProcessData carProcessData = new CarProcessData(oneHotEncodedRaws[i], IDs[i], chargePointLabels[i],  carParks[i], floors[i], carTypes[i], wdays[i], timestampArrival, timestampDeparture);
				result.add(carProcessData);
				rConnector.log(2, "Sampled: " + carProcessData.toString());
			}
			rConnector.log(1, "Finished sampling and built CarProcessData with nCars=" + nCars + " in " + (System.currentTimeMillis()-timeStart)/1000.0 + "s.");
			
		} catch (Exception e) {
			System.out.println("Executed R string:");
			System.out.println(rCommand);
			System.out.println("Result REXP:");
			System.out.println(rResult.toDebugString());
			e.printStackTrace();
		}
		
		return result;
	}
	
	
	public static void main(String[] args) {
		/*List<CarProcessData> result = CarProcessData.generateCarProcessData(10000, 0);
		int errorCount = 0;
		for (CarProcessData c : result) {
			if (c.timestampDeparture < c.timestampArrival) {
				errorCount++;
				System.out.println(c);
			}
		}
		if (errorCount > 0) System.out.println(errorCount);*/
		
		
		
		List<CarProcessData> result = CarProcessData.sampleHistoricalCarProcesses(100, 1);
		int errorCount = 0;
		for (CarProcessData c : result) {
			if (c.timestampDeparture < c.timestampArrival) {
				errorCount++;
				System.out.println(c);
			}
			System.out.println(c.oneHotEncodedRaw);
		}
		
		if (errorCount > 0) System.out.println(errorCount);
		
	}

}
