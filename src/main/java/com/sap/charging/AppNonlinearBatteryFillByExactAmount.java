package com.sap.charging;

import com.sap.charging.model.battery.BatteryData_Sample;
import com.sap.charging.model.battery.BatterySim;
import com.sap.charging.model.battery.BatterySimParameters;
import com.sap.charging.opt.CONSTANTS;

public class AppNonlinearBatteryFillByExactAmount {

	public static void main(String[] args) {
		// We want to fill EXACTLY by 1Ah
		// We know z_0 and z_1 = z_0 + 1Ah/Q
		// We know i_1(z_1) (at end of interval)
		// Want to find i_0 charge plan to fit exactly 10Ah
		// Simulate backward until rest can be filled with linear approach

		double initialSoC = 0.8;
		double desiredCapacity = 5.7; // 5.7 stops after 457 steps

		double maxCurrentAllowed = 96;
		boolean enableEfficiency = true;

		BatterySimParameters paramsSimBackwards = BatterySimParameters.buildFromBatteryData(new BatteryData_Sample() {
			@Override
			public double getResistanceFromSOC(double soc) {
				return 0.006;
			}

		});
		paramsSimBackwards.maxtime = 900;
		paramsSimBackwards.initialSoC = initialSoC + (desiredCapacity / paramsSimBackwards.capacity); // z_0+1Ah/Q
		BatterySim simBackwards = new BatterySim(paramsSimBackwards, false, enableEfficiency);

		System.out.println("Starting backwards sim with initialSoC=" + paramsSimBackwards.initialSoC);

		double capacitySoFarNonlinear = 0;
		for (int step = 900; step > 0; step--) {
			simBackwards.simulatePreviousStep(maxCurrentAllowed);

			double nextCurrent = simBackwards.getCurrentBasedOnSoC(simBackwards.getSoC(), maxCurrentAllowed);

			// How much capacity (Ah) would we get: Convert Ampere in remaining time in this
			// timeslot to Ah
			double capacityRestWithLinear = nextCurrent * step / 3600.0 * CONSTANTS.CHARGING_EFFICIENCY;

			// If we can fill rest of the capacity with linear in order to exactly reach
			// desiredCapacity, stop taking max current here and instead take linear current
			System.out.println("step=" + step + ", nextCurrent=" + nextCurrent + "A, capacitySoFarNonlinear="
					+ capacitySoFarNonlinear + "Ah, capacityRestWithLinear=" + capacityRestWithLinear + "Ah");
			if (capacityRestWithLinear + capacitySoFarNonlinear > desiredCapacity) {
				// Don't use nextCurrent (since this might be way too much), instead recalculate
				// exact amount

				double maxLinearCurrent = (desiredCapacity - capacitySoFarNonlinear) * (3600.0 / 900); // Convert from
																										// Ah in 900s
																										// (A/(1/4H)) to
																										// A: How much A
																										// do I need to
																										// charge with
																										// to reach Ah
																										// in 15
																										// minutes?
				maxLinearCurrent = maxLinearCurrent / CONSTANTS.CHARGING_EFFICIENCY; // efficiency
				maxLinearCurrent = maxLinearCurrent / (step / 900.0); // maybe we are in middle of timeslot

				maxCurrentAllowed = maxLinearCurrent;
				System.out.println("Stopping with maxCurrentAllowed=" + maxCurrentAllowed + "A");
				break;
			}

			// How much have we charged so far in the nonlinear part?
			capacitySoFarNonlinear = -simBackwards.getChargedAh();

		}

		// Check the result by simulating forwards with resulting maxCurrentAllowed
		BatterySimParameters paramsSimForwards = paramsSimBackwards.copy();
		paramsSimForwards.initialSoC = initialSoC;
		BatterySim simForwards = new BatterySim(paramsSimForwards, true, enableEfficiency);

		for (int step = 0; step < 900; step++) {
			simForwards.simulateNextStep(maxCurrentAllowed);
		}

		System.out.println(simForwards.getChargedAh());
		simForwards.writeResultsToCSV("gen/data/fillChargingPlanNonlinearCorrect.csv");

		BatterySimParameters paramsSimForwardsLinear = paramsSimBackwards.copy();
		paramsSimForwardsLinear.initialSoC = initialSoC;
		BatterySim simForwardsLinear = new BatterySim(paramsSimForwardsLinear, true, enableEfficiency);

		double maxLinearCurrent = desiredCapacity * (3600.0 / 900); // Convert from Ah in 900s (A/(1/4H)) to A: How much
																	// A do I need to charge with to reach Ah in 15
																	// minutes?
		maxLinearCurrent = maxLinearCurrent / CONSTANTS.CHARGING_EFFICIENCY; // efficiency

		System.out.println("Max linear current approximation: " + maxLinearCurrent + "A");

		for (int step = 0; step < 900; step++) {
			simForwardsLinear.simulateNextStep(maxLinearCurrent);
		}

		System.out.println(simForwardsLinear.getChargedAh());
		simForwardsLinear.writeResultsToCSV("gen/data/fillChargingPlanNonlinearLinearApproximation.csv");

		BatterySimParameters paramsSimForwardsMax = paramsSimBackwards.copy();
		paramsSimForwardsMax.initialSoC = initialSoC;
		BatterySim simForwardsMax = new BatterySim(paramsSimForwardsMax, true, enableEfficiency);

		for (int step = 0; step < 900; step++) {
			simForwardsMax.simulateNextStep(96);
		}

		System.out.println(simForwardsMax.getChargedAh());
		simForwardsMax.writeResultsToCSV("gen/data/fillChargingPlanNonlinearMaxCurrent.csv");

	}

}
