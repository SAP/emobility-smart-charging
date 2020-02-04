package com.sap.charging.sim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.model.Car;
import com.sap.charging.model.CarFactory.CarModel;
import com.sap.charging.model.EnergyUtil;
import com.sap.charging.model.battery.BatteryData;
import com.sap.charging.model.battery.BatteryData_Sample;
import com.sap.charging.model.battery.BatterySim;
import com.sap.charging.model.battery.BatterySimParameters;
import com.sap.charging.util.random.ConstantDistribution;

public class SimulationNonlinearBatteryTest {
	
	Car car;
	final double maxCurrentAllowedStatic = 96;
	
	@BeforeEach
	public void setup() {
		Simulation.verbosity = 0; 
		
		Simulation.verbosity = 0;
		
		DataGeneratorRandom data = new DataGeneratorRandom(5, false);
		data.setCarModels(new CarModel[] {CarModel.RENAULT_ZOE_ZE40});
		
		data.setIdealCars(true);
		data.setIdealChargingStations(true);
		data.setCurCapacityDistribution(new ConstantDistribution(null, 0));
		data.setNonlinearCharging(true);
		
		data.generateEnergyPriceHistory(96)
			.generateChargingStations(1)
			.generateCars(1)
			.generateFuseTree(100, true);
		
		car = data.getCar(0);
	}
	
	
	
	@Test
	public void testInterp1_getIDX() {
		// Two variations: binary search (slow, exact, does not require equidistant x array)
		BatteryData d = new BatteryData_Sample();
		
		
		double[] x = new double[] {0, 0.05, 0.1, 0.15, 0.2};
		
		// Exact match
		assertEquals(3, d.getIDX(x, 0.15));
		assertEquals(3, d.getIDX_fast(x, 0.15));
		
		// Between two values
		assertEquals(4, d.getIDX(x, 0.16));
		assertEquals(4, d.getIDX_fast(x, 0.16));
		
		
	}
	
	
	
	@Test
	public void testNonlinearBatterySim_NoChargePlan_ConstantCurrent() {
		int nTimeslots = 1;
		int startTimeSeconds = 300;
		int endTimeSeconds = nTimeslots*900;
		
		BatterySimParameters params1 = BatterySimParameters.buildDefaultParams();
		BatterySim sim1 = new BatterySim(params1, false, false);
		sim1.simulate(startTimeSeconds, endTimeSeconds, car, maxCurrentAllowedStatic);
		
		double expectedChargedAh = EnergyUtil.getAmpereHours(endTimeSeconds-startTimeSeconds, maxCurrentAllowedStatic);
		assertEquals(expectedChargedAh, sim1.getChargedAh(), 1e-8);
		
	}
	
	
	@Test
	public void testNonlinearBatterySim_NoChargePlan_FullCharge() {
		int nTimeslots = 96/2;
		int startTimeSeconds = 0;
		int endTimeSeconds = nTimeslots*900;
		
		BatterySimParameters params1 = BatterySimParameters.buildDefaultParams();
		BatterySim sim1 = new BatterySim(params1, true, false);
		sim1.simulate(startTimeSeconds, endTimeSeconds, car, maxCurrentAllowedStatic);
		
		//System.out.println(sim1.getChargedAh());
		//System.out.println(params1.capacity - sim1.getChargedAh());
		//System.out.println(sim1.getSoC());
		
		double expectedChargedAh = params1.capacity;
		assertEquals(expectedChargedAh, sim1.getChargedAh(), 1e-8);
		
		//sim1.writeResultsToCSV("gen/data/NonlinearBattery.csv");
				
	}
	
	
	@Test
	public void testNonlinearBatterySim_WithChargePlan() {
		
		int nTimeslots = 2;
		int startTimeSeconds = 600;
		int endTimeSeconds = nTimeslots*900;
		
		BatterySimParameters params1 = BatterySimParameters.buildDefaultParams();
		BatterySim sim1 = new BatterySim(params1, false, false);
		double[] chargePlan1 = new double[] { 32, 8 };
		car.setCurrentPlan(chargePlan1);
		
		sim1.simulate(startTimeSeconds, endTimeSeconds, car, maxCurrentAllowedStatic);
		
		// Assume linear charging
		double expectedChargedAh1 = EnergyUtil.getAmpereHours(900-startTimeSeconds, car.sumUsedPhases*chargePlan1[0]) + EnergyUtil.getAmpereHours(900, car.sumUsedPhases*chargePlan1[1]);
		assertEquals(expectedChargedAh1, sim1.getChargedAh(), 1e-8);
		
		BatterySimParameters params2 = params1.copy();
		BatterySim sim2 = new BatterySim(params2, false, false);
		double[] chargePlan2 = new double[] { 8, 16 };
		car.setCurrentPlan(chargePlan2);
		
		sim2.simulate(startTimeSeconds, endTimeSeconds, car, maxCurrentAllowedStatic);
		
		double expectedChargedAh2 = EnergyUtil.getAmpereHours(900-startTimeSeconds, car.sumUsedPhases*chargePlan2[0]) + EnergyUtil.getAmpereHours(900, car.sumUsedPhases*chargePlan2[1]);
		assertEquals(expectedChargedAh2, sim2.getChargedAh(), 1e-8);
		
	}
	
	
	@Test
	public void testNonlinearBatterySim_NextCurrent() {
		
		BatterySimParameters params1 = BatterySimParameters.buildDefaultParams();
		BatterySim sim1 = new BatterySim(params1, false, false);
		double[] chargePlan1 = new double[96];
		Arrays.fill(chargePlan1, 32);
		car.setCurrentPlan(chargePlan1);
		
		sim1.simulate(0, 0, null, 96);
		
		assertEquals(96, sim1.getNextCurrent(), 0);
		
		sim1.simulate(0, 86400, car, maxCurrentAllowedStatic);

		assertEquals(0, sim1.getNextCurrent(), 1e-8);
		
	}
	
	
	@Test
	public void testNonlinearBatterySim_NextStep_And_PreviousStep_LowInitialSoC() {
		// At low SoC: Behaviour is linear (so 96A once and -96A once)
		
		BatterySimParameters params1 = BatterySimParameters.buildDefaultParams();
		BatterySim sim1 = new BatterySim(params1, false, false);
		
		sim1.simulateNextStep(maxCurrentAllowedStatic);
		
		assertTrue(sim1.getChargedAh() > 0);
		assertTrue(sim1.getSoC() > 0);
		
		sim1.simulatePreviousStep(maxCurrentAllowedStatic);
		
		assertEquals(0, sim1.getChargedAh(), 0);
		assertEquals(0, sim1.getSoC(), 0);
		
	}
	
	
	
	@Test
	public void testNonlinearBatterySim_NextStep_And_PreviousStep_HighInitialSoC() {
		// At low SoC: Behaviour is linear (so 96A once and -96A once)
		double initialSoC = 0.99;
		
		BatterySimParameters params1 = BatterySimParameters.buildDefaultParams();
		params1.initialSoC = initialSoC;
		BatterySim sim1 = new BatterySim(params1, false, false);
		
		sim1.simulateNextStep(maxCurrentAllowedStatic);

		assertTrue(sim1.getSoC() > initialSoC);
		assertTrue(sim1.getChargedAh() > 0);
		
		sim1.simulatePreviousStep(maxCurrentAllowedStatic);
		
		//System.out.println(sim1.getSoC());
		//System.out.println(sim1.getChargedAh());
		
		assertEquals(0, sim1.getChargedAh(), 1e-5); 
		
	}
	
	
	
	@Test
	public void testNonlinearBatterySim_NextStep_And_PreviousStep_HighInitialSoC_ManySteps() {
		// At low SoC: Behaviour is linear (so 96A once and -96A once)
		double initialSoC = 0.99;
		
		BatterySimParameters params1 = BatterySimParameters.buildDefaultParams();
		params1.initialSoC = initialSoC;
		BatterySim sim1 = new BatterySim(params1, false, false);
		
		for (int i=0;i<10;i++) {
			sim1.simulateNextStep(maxCurrentAllowedStatic);
		}

		assertTrue(sim1.getSoC() > initialSoC);
		assertTrue(sim1.getChargedAh() > 0);
		
		for (int i=0;i<10;i++) {
			sim1.simulatePreviousStep(maxCurrentAllowedStatic);
		}
		
		//System.out.println(sim1.getSoC());
		//System.out.println(sim1.getChargedAh());
		
		assertEquals(0, sim1.getChargedAh(), 1e-4); // Will be a bit imprecise because we 
		
	}
	
	
	

	
	
	
}
