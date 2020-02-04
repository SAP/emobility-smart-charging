package com.sap.charging;

import com.sap.charging.model.battery.BatterySim;
import com.sap.charging.model.battery.BatterySimParameters;

public class AppNonlinearBatteryRuntime {



	public static void main(String[] args) {
		
		long startTime = System.currentTimeMillis();
		for (int simIndex=0;simIndex<10000;simIndex++) {
			if (simIndex % 100 == 0) {
				System.out.println("simIndex=" + simIndex);
			}
			doOneSim();
		}
		
		long finishTime = System.currentTimeMillis();
		long diff = finishTime - startTime;
		
		
		System.out.println("Done, took: " + diff + "ms. Time per sim: " + diff/10000 + "ms");
		
	}
	
	private static void doOneSim() {

		BatterySimParameters params = BatterySimParameters.buildDefaultParams();
		BatterySim sim = new BatterySim(params);
		
		for (int step=0;step<8*3600;step++) {
			sim.simulateNextStep(params.constantCurrent);
		}
		//System.out.println(soc);
	}
	
	
}
