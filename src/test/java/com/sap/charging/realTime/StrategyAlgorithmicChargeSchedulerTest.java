package com.sap.charging.realTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.model.Car;
import com.sap.charging.model.CarFactory.CarModel;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureOracle;
import com.sap.charging.realTime.util.TimeslotSorter;
import com.sap.charging.realTime.util.TimeslotSortingCriteria;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.random.ConstantDistribution;

public class StrategyAlgorithmicChargeSchedulerTest {

	private StrategyAlgorithmic strategyLinear;
	private StrategyAlgorithmicChargeScheduler schedulerLinear;
	
	private StrategyAlgorithmic strategyNonlinear;
	private StrategyAlgorithmicChargeScheduler schedulerNonlinear;

	ChargingStation chargingStation;
	Car car;
	double[] chargePlan;
	
	DataGeneratorRandom data;
	
	@BeforeEach
	public void setup() {
		Simulation.verbosity = 0;
		
		strategyLinear = new StrategyAlgorithmic();
		schedulerLinear = strategyLinear.getScheduler();
		strategyLinear.setRecognizeNonlinearCharging(false);
		
		strategyNonlinear = new StrategyAlgorithmic();
		schedulerNonlinear = strategyNonlinear.getScheduler();
		strategyNonlinear.setRecognizeNonlinearCharging(true);
		
		CONSTANTS.FUSE_LEVEL_0_SIZE = 32;
		CONSTANTS.FUSE_LEVEL_1_SIZE = 32;
		CONSTANTS.FUSE_LEVEL_2_SIZE = 32;
		
		
		int nCars = 1;
		data = new DataGeneratorRandom(5, false);
		data.setCarModels(new CarModel[] {CarModel.RENAULT_ZOE_ZE40});
		
		data.setIdealCars(true);
		data.setIdealChargingStations(true);
		data.setCurCapacityDistribution(new ConstantDistribution(null, 0));
		data.setNonlinearCharging(true);
		
		data.generateEnergyPriceHistory(96)
			.generateChargingStations(nCars)
			.generateCars(nCars)
			.generateSimpleFuseTree();
			//.generateFuseTree(100, true);
		
		
		
		chargingStation = data.getChargingStation(0);
		car = data.getCar(0);
		
		chargePlan = new double[] { 32, 32 };
		car.setCurrentPlan(chargePlan);
		
	}
	
	
	@Test
	public void testGetPlannedCapacity_Linear() {
		
		
		// Linear scheduler
		assertEquals(car.sumUsedPhases*900.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerLinear.getPlannedCapacityLinear(car, 0, 900), 1e-8);
		assertEquals(car.sumUsedPhases*600.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerLinear.getPlannedCapacityLinear(car, 300, 900), 1e-8);
		assertEquals(car.sumUsedPhases*600.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerLinear.getPlannedCapacityLinear(car,0, 600), 1e-8);
		assertEquals(car.sumUsedPhases*300.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerLinear.getPlannedCapacityLinear(car, 300, 600), 1e-8);
		assertEquals(car.sumUsedPhases*1200.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerLinear.getPlannedCapacityLinear(car, 300, 1500), 1e-8);
		
	
	}
	
	@Test
	public void testGetPlannedCapacity_Linear_Interval() {

		// Start of interval != currentTime
		assertEquals(car.sumUsedPhases*600.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerLinear.getPlannedCapacityNonlinear(chargingStation, car, 0, 300, 900), 1e-8);
		assertEquals(car.sumUsedPhases*150.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerLinear.getPlannedCapacityNonlinear(chargingStation, car, 0, 450, 600), 1e-8);
		assertEquals(car.sumUsedPhases*600.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerLinear.getPlannedCapacityNonlinear(chargingStation, car, 300, 900, 1500), 1e-8);
				
		
	}
	
	@Test
	public void testGetPlannedCapacity_Nonlinear() {
		
		// Nonlinear scheduler
		// Start of interval = currentTime
		assertEquals(car.sumUsedPhases*900.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerNonlinear.getPlannedCapacityNonlinear(chargingStation, car, 0, 0, 900), 1e-8);
		assertEquals(car.sumUsedPhases*600.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerNonlinear.getPlannedCapacityNonlinear(chargingStation, car, 300, 300, 900), 1e-8);
		assertEquals(car.sumUsedPhases*600.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerNonlinear.getPlannedCapacityNonlinear(chargingStation, car, 0, 0, 600), 1e-8);
		assertEquals(car.sumUsedPhases*300.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerNonlinear.getPlannedCapacityNonlinear(chargingStation, car, 300, 300, 600), 1e-8);
		assertEquals(car.sumUsedPhases*1200.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerNonlinear.getPlannedCapacityNonlinear(chargingStation, car, 300, 300, 1500), 1e-8);
		
	}
	
	@Test
	public void testGetPlannedCapacity_Nonlinear_Interval() {
		
		// Start of interval != currentTime
		assertEquals(0, schedulerNonlinear.getPlannedCapacityNonlinear(chargingStation, car, 0, 0, 0), 0);
		assertEquals(car.sumUsedPhases*600.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerNonlinear.getPlannedCapacityNonlinear(chargingStation, car, 0, 300, 900), 1e-8);
		assertEquals(car.sumUsedPhases*150.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerNonlinear.getPlannedCapacityNonlinear(chargingStation, car, 0, 450, 600), 1e-8);
		assertEquals(car.sumUsedPhases*600.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerNonlinear.getPlannedCapacityNonlinear(chargingStation, car, 300, 900, 1500), 1e-8);
		
		assertEquals(0, schedulerNonlinear.getPlannedCapacity(chargingStation, car, 0, 0, 0), 0);
		assertEquals(car.sumUsedPhases*600.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerNonlinear.getPlannedCapacity(chargingStation, car, 0, 300, 900), 1e-8);
		assertEquals(car.sumUsedPhases*150.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerNonlinear.getPlannedCapacity(chargingStation, car, 0, 450, 600), 1e-8);
		assertEquals(car.sumUsedPhases*600.0/3600*chargePlan[0]*CONSTANTS.CHARGING_EFFICIENCY, schedulerNonlinear.getPlannedCapacity(chargingStation, car, 300, 900, 1500), 1e-8);
		
		
	}
	
	@Test
	public void testGetPlannedCapacity_ChargePlanEmpty() {
		car.setCurrentPlan(new double[96]);
		
		assertEquals(0, schedulerLinear.getPlannedCapacityLinear(car, 0, 86400), 0);
		assertEquals(0, schedulerNonlinear.getPlannedCapacityNonlinear(chargingStation, car, 0, 0, 86400), 0);
		
	}
	
	
	@Test
	public void testGetCurrentToFillTimeslot_linear_() {
		double desiredCapacity = 5; 
		
		// 1 phase, complete timeslot, no original planned current
		double expectedAh = desiredCapacity * 4 / CONSTANTS.CHARGING_EFFICIENCY; 
		assertEquals(expectedAh, schedulerLinear.getCurrentToFillTimeslot_linear(desiredCapacity, 1, 0, 900, 0), 1e-8); 
		
		// 3 phase, complete timeslot, no original planned current
		expectedAh = desiredCapacity * 4 / CONSTANTS.CHARGING_EFFICIENCY / 3; 
		assertEquals(expectedAh, schedulerLinear.getCurrentToFillTimeslot_linear(desiredCapacity, 3, 0, 900, 0), 1e-8); 
		
		// 1 phase, half a timeslot, no original planned current
		expectedAh = desiredCapacity * 4 / CONSTANTS.CHARGING_EFFICIENCY * 2; 
		assertEquals(expectedAh, schedulerLinear.getCurrentToFillTimeslot_linear(desiredCapacity, 1, 0, 450, 0), 1e-8); 
		
		// 1 phase, complete timeslot, original planned current
		expectedAh = desiredCapacity * 4 / CONSTANTS.CHARGING_EFFICIENCY + 10; 
		assertEquals(expectedAh, schedulerLinear.getCurrentToFillTimeslot_linear(desiredCapacity, 1, 0, 900, 10), 1e-8); 
	}
	
	
	@Test
	public void testFillChargingPlan_Linear() {
		car.setCurrentPlan(new double[96]);
		double desiredCapacity = car.getMissingCapacity();
		
		schedulerLinear.fillChargingPlan(car, chargingStation, desiredCapacity, 
				TimeslotSorter.getSortedTimeslots(null, 0, 95, TimeslotSortingCriteria.INDEX), 
				300);
		
		assertEquals(32, car.getCurrentPlan()[0], 0);
		assertEquals(32, car.getCurrentPlan()[1], 0);
		assertEquals(32, car.getCurrentPlan()[2], 0);
		
		//System.out.println(Arrays.toString(car.getCurrentPlan()));
	}
	
	@Test
	public void testFillChargingPlan_Linear_ExistingPlan() {
		car.setCurrentPlan(new double[96]);
		car.getCurrentPlan()[1] = 16;
		
		double desiredCapacity = car.getMissingCapacity() - schedulerLinear.getPlannedCapacity(chargingStation, car, 300);
		
		schedulerLinear.fillChargingPlan(car, chargingStation, desiredCapacity, 
				TimeslotSorter.getSortedTimeslots(null, 0, 95, TimeslotSortingCriteria.INDEX), 
				300);
		
		assertEquals(32, car.getCurrentPlan()[0], 0);
		assertEquals(32, car.getCurrentPlan()[1], 0);
		assertEquals(32, car.getCurrentPlan()[2], 0);
		assertEquals(car.getMaxCapacity(), schedulerLinear.getPlannedCapacity(chargingStation, car, 300), 1e-8);
		
		//System.out.println(Arrays.toString(car.getCurrentPlan()));
	}
	
	
	@Test
	public void testFillChargingPlan_Nonlinear_ExistingPlan() {
		int startTimeSeconds = 300;
		car.setCurrentPlan(new double[96]);
		car.getCurrentPlan()[1] = 16;
		
		double desiredCapacity = car.getMissingCapacity() - schedulerNonlinear.getPlannedCapacity(chargingStation, car, startTimeSeconds);
		
		schedulerNonlinear.fillChargingPlan(car, chargingStation, desiredCapacity, 
				TimeslotSorter.getSortedTimeslots(null, 0, 95, TimeslotSortingCriteria.INDEX), 
				startTimeSeconds);
		
		assertEquals(32, car.getCurrentPlan()[0], 0);
		assertEquals(32, car.getCurrentPlan()[1], 0);
		assertEquals(32, car.getCurrentPlan()[2], 0);
		assertEquals(car.getMaxCapacity(), schedulerNonlinear.getPlannedCapacity(chargingStation, car, startTimeSeconds), 1e-8);
		
		//System.out.println(Arrays.toString(car.getCurrentPlan()));
	}
	
	
	
	
	@Test
	public void testFillChargingPlan_Nonlinear_EmptyCar() {
		
		double desiredCapacity = car.getMissingCapacity();
		
		int startTimeSeconds = 0; // Easy case: Start of timeslot
		car.setCurrentPlan(new double[96]);
		schedulerNonlinear.fillChargingPlan(car, chargingStation, desiredCapacity, 
				TimeslotSorter.getSortedTimeslots(null, 0, 95, TimeslotSortingCriteria.INDEX), 
				startTimeSeconds);
		
		assertEquals(32, car.getCurrentPlan()[0], 0);
		assertEquals(32, car.getCurrentPlan()[1], 0);
		assertEquals(32, car.getCurrentPlan()[2], 0);
		assertTrue(car.getCurrentPlan()[3] < 32);
		
		assertEquals(car.getMaxCapacity(), schedulerNonlinear.getPlannedCapacity(chargingStation, car, startTimeSeconds), 1e-8);
	}
	
	@Test
	public void testFillChargingNonlinear_EmptyCar_MiddleOfTimeslot() {	
		
		double desiredCapacity = car.getMissingCapacity();
		int startTimeSeconds = 600; // Harder: Middle of timeslot (only fraction of first timeslot can be used)
		car.setCurrentPlan(new double[96]);
		schedulerNonlinear.fillChargingPlan(car, chargingStation, desiredCapacity, 
				TimeslotSorter.getSortedTimeslots(null, 0, 95, TimeslotSortingCriteria.INDEX), 
				startTimeSeconds);
				
		
		// InitialSoC = 0
		assertEquals(32, car.getCurrentPlan()[0], 0);
		assertEquals(32, car.getCurrentPlan()[1], 0);
		assertEquals(32, car.getCurrentPlan()[2], 0);
		assertEquals(32, car.getCurrentPlan()[3], 0);
		assertTrue(car.getCurrentPlan()[4] < 32);
		
		assertEquals(car.getMaxCapacity(), schedulerNonlinear.getPlannedCapacity(chargingStation, car, startTimeSeconds), 1e-8);
		
		
	}
	
	@Test
	public void testFillChargingNonlinear_EmptyCar_SmallDesiredCapacity_LowInitialSoC() {
		
		double desiredCapacity = 0.1;
		int startTimeSeconds = 0;
		
		car.setCurrentPlan(new double[96]);
		schedulerNonlinear.fillChargingPlan(car, chargingStation, desiredCapacity, 
				TimeslotSorter.getSortedTimeslots(null, 0, 95, TimeslotSortingCriteria.INDEX), 
				startTimeSeconds);
		
		assertEquals(desiredCapacity, schedulerNonlinear.getPlannedCapacity(chargingStation, car, startTimeSeconds), 1e-8);
		assertTrue(car.getCurrentPlan()[0] > 0);
		assertTrue(car.getCurrentPlan()[0] < 32);
		
		
		
		
	}
	
	
	@Test
	public void testFillChargingNonlinear_EmptyCar_SmallDesiredCapacity_HighInitialSoC() {
		
		int startTimeSeconds = 0;
		double initialSoC = 0.99;
		while (car.carBattery.getSoC() < initialSoC) {
			car.addChargedCapacity(1, 32);
		}
		double desiredCapacity = 0.1;
		
		car.setCurrentPlan(new double[96]);
		schedulerNonlinear.fillChargingPlan(car, chargingStation, desiredCapacity, 
				TimeslotSorter.getSortedTimeslots(null, 0, 95, TimeslotSortingCriteria.INDEX), 
				startTimeSeconds);
		
		assertEquals(desiredCapacity, schedulerNonlinear.getPlannedCapacity(chargingStation, car, startTimeSeconds), 1e-8);
		assertTrue(car.getCurrentPlan()[0] > 0);
		assertTrue(car.getCurrentPlan()[0] < 32);
		//System.out.println(Arrays.toString(car.getCurrentPlan()));
		assertEquals(0, car.getCurrentPlan()[1], 1e-8);
	
	}

	@Test
	public void testFillChargingNonlinear_EmptyCar_ExhaustiveDesiredCapacity() {
	
		// Test exactness of nonlinear filling charge plans
		Simulation.verbosity = 0;
		int startTimeSeconds = 0;
		
		double desiredCapacityInterval = 1; 
		
		double desiredCapacity = desiredCapacityInterval;
		while (desiredCapacity <= car.getMaxCapacity()) {
			
			//System.out.println("desiredCapacity=" + desiredCapacity);
			car.setCurrentPlan(new double[96]);
			schedulerNonlinear.fillChargingPlan(car, chargingStation, desiredCapacity, 
					TimeslotSorter.getSortedTimeslots(null, 0, 96, TimeslotSortingCriteria.INDEX), 
					startTimeSeconds);
			
			assertEquals(desiredCapacity, schedulerNonlinear.getPlannedCapacity(chargingStation, car, startTimeSeconds), 1e-2);
			desiredCapacity += desiredCapacityInterval;
		}
		
		
	}
	
	@Test
	public void testFillChargingNonlinear_HighInitialSoC_ExhaustiveDesiredCapacity() {
		// Test exactness of nonlinear filling charge plans
		Simulation.verbosity = 0;
		int startTimeSeconds = 0;
		
		// Charge until soc=0.9
		double initialSoC = 0.9;
		while (car.carBattery.getSoC() < initialSoC) {
			car.addChargedCapacity(1, 32);
		}
		
		double desiredCapacity = 0.1;
		while (desiredCapacity <= car.getMissingCapacity()) {
			
			car.setCurrentPlan(new double[96]);
			schedulerNonlinear.fillChargingPlan(car, chargingStation, desiredCapacity, 
					TimeslotSorter.getSortedTimeslots(null, 0, 96, TimeslotSortingCriteria.INDEX), 
					startTimeSeconds);
			

			double plannedCapacity = schedulerNonlinear.getPlannedCapacity(chargingStation, car, startTimeSeconds);
			//System.out.println("desiredCapacity=" + desiredCapacity + "Ah, plannedCapacity=" + plannedCapacity);
			
			assertEquals(desiredCapacity, plannedCapacity, 1e-2);
			desiredCapacity+=0.1;
		}
		
	}
	
	
	
	@Test
	public void testFillChargingPlan_Linear_HighInitialSoC() {
		
		// InitialSoC = 0.9
		// Charge until soc=0.9
		double initialSoC = 0.9;
		int startTimeSeconds = 0;
		
		while (car.carBattery.getSoC() < initialSoC) {
			car.addChargedCapacity(1, 32);
		}
		double desiredCapacity = car.getMissingCapacity();
		
		// Linear
		car.setCurrentPlan(new double[96]);
		schedulerLinear.fillChargingPlan(car, chargingStation, desiredCapacity, 
				TimeslotSorter.getSortedTimeslots(null, 0, 95, TimeslotSortingCriteria.INDEX), 
				startTimeSeconds);
		
		
		//System.out.println("DesiredCapactiy: " + desiredCapacity);
		//System.out.println("Planned capacity: " + schedulerLinear.getPlannedCapacity(chargingStation, car, startTimeSeconds));
		//System.out.println(Arrays.toString(car.getCurrentPlan()));
		

		assertTrue(car.getCurrentPlan()[0] > 0);
		assertEquals(car.getMissingCapacity(), schedulerLinear.getPlannedCapacity(chargingStation, car, startTimeSeconds), 1e-6);
	}
		
	@Test
	public void testFillChargingPlan_Nonlinear_HighInitialSoC() {
		
		// InitialSoC = 0.9
		// Charge until soc=0.9
		double initialSoC = 0.99;
		int startTimeSeconds = 0;
		while (car.carBattery.getSoC() < initialSoC) {
			car.addChargedCapacity(1, 32);
		}
		double desiredCapacity = car.getMissingCapacity();
		
		
		// Nonlinear
		car.setCurrentPlan(new double[96]);
		schedulerNonlinear.fillChargingPlan(car, chargingStation, desiredCapacity, 
				TimeslotSorter.getSortedTimeslots(null, 0, 95, TimeslotSortingCriteria.INDEX), 
				startTimeSeconds);
		
		//System.out.println(Arrays.toString(car.getCurrentPlan()));
		//System.out.println(schedulerNonlinear.getPlannedCapacity(chargingStation, car, startTimeSeconds));
		
		assertEquals(car.getMissingCapacity(), schedulerNonlinear.getPlannedCapacity(chargingStation, car, startTimeSeconds), 1e-8);
		
	}
	
	
	@Test
	public void testFillChargingPlan_Nonlinear_BlockedTimeslots() {
		
		int startTimeSeconds = 0;
		car.setCurrentPlan(new double[96]);
		double desiredCapacity = car.getMissingCapacity();
		
		boolean[] blockedTimeslots = new boolean[96];
		blockedTimeslots[1] = true;
		blockedTimeslots[3] = true;
		
		schedulerNonlinear.fillChargingPlan(car, chargingStation, desiredCapacity, 
				TimeslotSorter.getSortedTimeslots(null, 0, 95, TimeslotSortingCriteria.INDEX, blockedTimeslots), 
				startTimeSeconds);
		
		//System.out.println(Arrays.toString(car.getCurrentPlan()));
		
		assertEquals(32, car.getCurrentPlan()[0], 1e-8);
		assertEquals(0, car.getCurrentPlan()[1], 1e-8);
		assertEquals(32, car.getCurrentPlan()[2], 1e-8);
		assertEquals(0, car.getCurrentPlan()[3], 1e-8);
		assertEquals(32, car.getCurrentPlan()[4], 1e-8);
		assertEquals(desiredCapacity, schedulerNonlinear.getPlannedCapacity(chargingStation, car, startTimeSeconds), 1e-8);
		
		
	}
	
	@Test
	public void testNonlinearCache() {
		
		StrategyAlgorithmic strategy1 = new StrategyAlgorithmic(new CarDepartureOracle());
		strategy1.setRecognizeNonlinearCharging(true);
		strategy1.getScheduler().setEnablePlannedCapacityCache(false);
		Simulation sim1 = new Simulation(data, strategy1);
		sim1.setEnableCSVStorage(true);
		sim1.init();
		sim1.simulate();
		
		
		DataGeneratorRandom data2 = data.clone();
		StrategyAlgorithmic strategy2 = new StrategyAlgorithmic(new CarDepartureOracle());
		strategy2.setRecognizeNonlinearCharging(true);
		strategy2.getScheduler().setEnablePlannedCapacityCache(true);
		Simulation sim2 = new Simulation(data2, strategy2);
		sim2.setEnableCSVStorage(true);
		sim2.init();
		sim2.simulate();
		
		//System.out.println(sim1.getSimulationResult().getSumCharged());
		//System.out.println(sim2.getSimulationResult().getSumCharged());
		
		//System.out.println("Cache hits: " + strategy2.getScheduler().plannedCapacityHashMapHits);
		
		assertEquals(sim1.getSimulationResult().getSumCharged(), sim2.getSimulationResult().getSumCharged(), 1e-8);
		
		
	}
	
	
}








