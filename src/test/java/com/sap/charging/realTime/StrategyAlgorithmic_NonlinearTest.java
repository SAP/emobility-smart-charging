package com.sap.charging.realTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.model.Car;
import com.sap.charging.model.CarFactory.CarModel;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.model.Fuse;
import com.sap.charging.model.battery.BatteryData_Sample;
import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.realTime.model.CarAssignment;
import com.sap.charging.realTime.util.TimeslotSorter;
import com.sap.charging.realTime.util.TimeslotSortingCriteria;
import com.sap.charging.sim.Simulation;
import com.sap.charging.sim.eval.exception.FuseTreeException;
import com.sap.charging.sim.util.SimulationListenerCSV;
import com.sap.charging.util.TimeUtil;
import com.sap.charging.util.random.ConstantDistribution;

public class StrategyAlgorithmic_NonlinearTest {

	private StrategyAlgorithmic strategyLinear;
	private Simulation simulationLinear;
	
	private StrategyAlgorithmic strategyNonlinear;
	private Simulation simulationNonlinear;
	
	private DataGeneratorRandom data;
	
	ChargingStation chargingStation;
	ChargingStation chargingStation2;
	Car car;
	Car car2;
	CarAssignment carAssignment;
	CarAssignment carAssignment2;
	State stateNonlinear;
	
	@BeforeEach
	public void setup() {
		
		Simulation.verbosity = 0;
		
		data = new DataGeneratorRandom(5, false);
		data.setCarModels(new CarModel[] {CarModel.RENAULT_ZOE_ZE40});
		data.setBatteryData(new BatteryData_Sample() {
			@Override
			public double getResistanceFromSOC(double soc) {
				return 0.006;
			}
			
		});
		
		data.setIdealCars(true);
		data.setIdealChargingStations(true);
		data.setCurCapacityDistribution(new ConstantDistribution(null, 0));
		data.setNonlinearCharging(true);
		
		int nCars = 2;
		
		CONSTANTS.FUSE_LEVEL_0_SIZE = 100;
		
		data.generateEnergyPriceHistory(96)
			.generateChargingStations(nCars)
			.generateCars(nCars)
			.generateSimpleFuseTree();
			//.generateFuseTree(nCars, true);
		
		for (int k=0;k<data.getEnergyPriceHistory().getNTimeslots();k++) {
			data.getEnergyPriceHistory().getPrices()[k] = k/2.0;
		}
		
		
		chargingStation = data.getChargingStation(0);
		chargingStation2 = data.getChargingStation(1);
		car = data.getCar(0);
		car.timestampArrival = TimeUtil.getTimestampFromSeconds(0);
		car2 = data.getCar(1);
		car2.timestampArrival = TimeUtil.getTimestampFromSeconds(0);
		carAssignment = new CarAssignment(car, chargingStation);
		carAssignment.setExpectedDepartureTimeSeconds(car.timestampDeparture.toSecondOfDay());
		
		strategyLinear = new StrategyAlgorithmic();
		strategyLinear.setRecognizeNonlinearCharging(false);
		simulationLinear = new Simulation(data, strategyLinear);
		simulationLinear.init();
		
		strategyNonlinear = new StrategyAlgorithmic();
		strategyNonlinear.setRecognizeNonlinearCharging(true);
		simulationNonlinear = new Simulation(data, strategyNonlinear);
		simulationNonlinear.setIntervalReoptimizationEvents(300);
		simulationNonlinear.init();
		
		stateNonlinear = simulationNonlinear.getState();
		stateNonlinear.addCarAssignment(car, chargingStation);
		stateNonlinear.getCurrentCarAssignment(car).setExpectedDepartureTimeSeconds(car.timestampDeparture.toSecondOfDay());
		stateNonlinear.addCarAssignment(car2, chargingStation2);
		stateNonlinear.getCurrentCarAssignment(car2).setExpectedDepartureTimeSeconds(car2.timestampDeparture.toSecondOfDay());
		
	}
	
	
	

	@Test
	public void rescheduleCarTest_Nonlinear_RescheduleCarWith0A_ToLaterTimeslotWith0A() {
		// [32, 0, 31, 0, 0] (31 is violated)
		// should result in
		// [32, 0, 0, 31, 0]
		
		int violatingK = 2;
		boolean[] blockedTimeslots = new boolean[96];
		blockedTimeslots[violatingK] = true;

		car.setCurrentPlan(new double[96]);
		car.getCurrentPlan()[0] = 32;
		blockedTimeslots[1] = true; 
		car.getCurrentPlan()[2] = 32;
		car.getCurrentPlan()[3] = 0; // 32A should be pushed here
		
		//System.out.println(Arrays.toString(car.getCurrentPlan()));
		
		double startPlannedCapacity = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation, car, 0);
		
		strategyNonlinear.rescheduleCar(simulationNonlinear.getState(), carAssignment, 
				blockedTimeslots, violatingK, null);
		
		double endPlannedCapacity = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation, car, 0);
		
		assertEquals(32, car.getCurrentPlan()[0], 1e-8);
		assertEquals(0, car.getCurrentPlan()[1], 1e-8);
		assertEquals(0, car.getCurrentPlan()[2], 1e-8);
		assertTrue(car.getCurrentPlan()[3] > 0);
		assertEquals(0, car.getCurrentPlan()[4], 1e-8);
		
		assertEquals(startPlannedCapacity, endPlannedCapacity, 1e-8);
		//System.out.println(Arrays.toString(car.getCurrentPlan()));
		//System.out.println("startPlannedCapacity=" + startPlannedCapacity + "Ah, endPlannedCapacity=" + endPlannedCapacity + "Ah");
		
	}
	
	
	@Test
	public void rescheduleCarTest_Nonlinear_RescheduleCarWith0A_ToLaterTimeslotWith0A_WithLaterPlan() {

		// [32, 0, 31, 0, 23] (31 is violated)
		// should result in
		// [32, 0, 0, 31, 23]
		
		int violatingK = 2;
		boolean[] blockedTimeslots = new boolean[96];
		blockedTimeslots[1] = true; 
		blockedTimeslots[violatingK] = true;

		car.setCurrentPlan(new double[96]);
		// Fill at 0, 2 and 4 with full current possible
		strategyNonlinear.getScheduler().fillChargingPlan(car, chargingStation, car.getMaxCapacity(), 
				TimeslotSorter.getSortedTimeslots(null, 0, 5, TimeslotSortingCriteria.INDEX, new boolean[] {false, true, false, true, false}), 
				0);
		
		double currentAtTimeslot2 = car.getCurrentPlan()[2]; // Should be 31.628 with R0 = 0.006 this amount should be at k=3 after rescheduling
		double currentAtTimeslot4 = car.getCurrentPlan()[4]; // Should be 23.621 with R0 = 0.006. This amount should be 
		
		double startPlannedCapacity = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation, car, 0);
		
		strategyNonlinear.rescheduleCar(simulationNonlinear.getState(), carAssignment, 
				blockedTimeslots, violatingK, null);
		
		double endPlannedCapacity = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation, car, 0);
		
		//System.out.println("startPlannedCapacity=" + startPlannedCapacity + "Ah, endPlannedCapacity=" + endPlannedCapacity + "Ah");
		
		assertEquals(32, car.getCurrentPlan()[0], 1e-8);
		assertEquals(0, car.getCurrentPlan()[1], 1e-8); // Was blocked
		assertEquals(0, car.getCurrentPlan()[2], 1e-8); // Was violated
		assertEquals(currentAtTimeslot2, car.getCurrentPlan()[3], 1e-8); // current from k=2 should be reassigned here
		assertEquals(currentAtTimeslot4, car.getCurrentPlan()[4], 1e-8); // Should be unchanged
		assertEquals(0, car.getCurrentPlan()[5], 1e-8); // Should be unchanged
		
		assertEquals(startPlannedCapacity, endPlannedCapacity, 1e-8);
		
	}
	
	
	
	@Test
	public void rescheduleCarTest_Nonlinear_RescheduleCarWith0A_ToEarlierTimeslotWith0A() {
		
		// [32, 0, 0, 31, 0] (31 is violated)
		// should result in
		// [32, 0, 31, 0, 0]
		
		int violatingK = 3;
		boolean[] blockedTimeslots = new boolean[96];
		blockedTimeslots[1] = true; 
		blockedTimeslots[violatingK] = true;

		car.setCurrentPlan(new double[96]);
		// Fill at 0, 2 and 4 with full power
		strategyNonlinear.getScheduler().fillChargingPlan(car, chargingStation, car.getMaxCapacity(), 
				TimeslotSorter.getSortedTimeslots(null, 0, 4, TimeslotSortingCriteria.INDEX, new boolean[] {false, true, true, false}), 
				0);
		

		
		//System.out.println(Arrays.toString(car.getCurrentPlan()));
		
		double startPlannedCapacity = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation, car, 0);
		
		strategyNonlinear.rescheduleCar(simulationNonlinear.getState(), carAssignment, 
				blockedTimeslots, violatingK, null);
		
		double endPlannedCapacity = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation, car, 0);
		
		//System.out.println(Arrays.toString(car.getCurrentPlan()));
		//System.out.println("startPlannedCapacity=" + startPlannedCapacity + "Ah, endPlannedCapacity=" + endPlannedCapacity + "Ah");
		
		
		assertEquals(32, car.getCurrentPlan()[0], 1e-8);
		assertEquals(0, car.getCurrentPlan()[1], 1e-8); // Was blocked
		assertTrue(car.getCurrentPlan()[2] > 0); 		// Was reassigned
		assertEquals(0, car.getCurrentPlan()[3], 1e-8); // Was violated 
		
		assertEquals(startPlannedCapacity, endPlannedCapacity, 1e-8);
		
	}
	

	
	@Test
	public void rescheduleCarTest_Nonlinear_RescheduleCarWith0A_ToEarlierTimeslotWith0A_WithLaterPlan() {

		// [32, 0, 0, 31, 23] (31 is violated)
		// should result in
		// [32, 0, 31, 0, 23]
		
		int violatingK = 3;
		boolean[] blockedTimeslots = new boolean[96];
		blockedTimeslots[1] = true; 
		blockedTimeslots[violatingK] = true;

		car.setCurrentPlan(new double[96]);
		
		// Fill at 0, 3 and 4 with full current possible
		strategyNonlinear.getScheduler().fillChargingPlan(car, chargingStation, car.getMaxCapacity(), 
				TimeslotSorter.getSortedTimeslots(null, 0, 5, TimeslotSortingCriteria.INDEX, new boolean[] {false, true, true, false, false}), 
				0);
		
		double currentAtTimeslot3 = car.getCurrentPlan()[3]; // Should be 31.628 with R0 = 0.006 this amount should be at k=3 after rescheduling
		double currentAtTimeslot4 = car.getCurrentPlan()[4]; // Should be 23.621 with R0 = 0.006. This amount should be 
		
		double startPlannedCapacity = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation, car, 0);
		
		strategyNonlinear.rescheduleCar(simulationNonlinear.getState(), carAssignment, 
				blockedTimeslots, violatingK, null);
		
		double endPlannedCapacity = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation, car, 0);
		
		
		// 0 is already planned (32A)
		// 1 is blocked
		// 2 is violated (and should be reassigned to 3)
		// 3 is unplanned (current=0)
		// 4 was already planned (< 32A) and should be unchanged
		
		//System.out.println("startPlannedCapacity=" + startPlannedCapacity + "Ah, endPlannedCapacity=" + endPlannedCapacity + "Ah");
		
		assertEquals(32, car.getCurrentPlan()[0], 1e-8);
		assertEquals(0, car.getCurrentPlan()[1], 1e-8); // Was blocked
		assertEquals(currentAtTimeslot3, car.getCurrentPlan()[2], 1e-8); // current from k=3 should be reassigned here
		assertEquals(0, car.getCurrentPlan()[3], 1e-8); // Violated
		assertEquals(currentAtTimeslot4, car.getCurrentPlan()[4], 1e-8); // Should be unchanged
		assertEquals(0, car.getCurrentPlan()[5], 1e-8); // Should be unchanged
		
		assertEquals(startPlannedCapacity, endPlannedCapacity, 1e-8);
		
	}
	
	
	
	@Test
	public void rescheduleCarTest_Nonlinear_RescheduleCarWith0A_ToEarlierTimeslot_AroundLaterPlan() {

		// [32, 0, 0, 31, 23] (23 is violated)
		// should result in
		// [32, 0, 31, 23, 0]
		
		int violatingK = 4;
		boolean[] blockedTimeslots = new boolean[96];
		blockedTimeslots[1] = true; 
		blockedTimeslots[violatingK] = true;

		car.setCurrentPlan(new double[96]);
		
		// Fill at 0, 3 and 4 with full current possible
		strategyNonlinear.getScheduler().fillChargingPlan(car, chargingStation, car.getMaxCapacity(), 
				TimeslotSorter.getSortedTimeslots(null, 0, 5, TimeslotSortingCriteria.INDEX, new boolean[] {false, true, true, false, false}), 
				0);
		
		double currentAtTimeslot3 = car.getCurrentPlan()[3]; // Should be 31.628 with R0 = 0.006 this amount should be at k=3 after rescheduling
		double currentAtTimeslot4 = car.getCurrentPlan()[4]; // Should be 23.621 with R0 = 0.006. This amount should be 
		
		double startPlannedCapacity = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation, car, 0);
		
		strategyNonlinear.rescheduleCar(simulationNonlinear.getState(), carAssignment, 
				blockedTimeslots, violatingK, null);
		
		double endPlannedCapacity = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation, car, 0);
		
		//System.out.println(Arrays.toString(car.getCurrentPlan()));
		
		// 0 is already planned (32A)
		// 1 is blocked
		// 2 is unplanned (current=0)
		// 3 is already planned (31A). SHOULD BE UPDATED TO 23A
		// 4 is planned (23A) and violated (and should be reassigned to 2). SHOULD BE UPDATED TO 31A
		
		//System.out.println("startPlannedCapacity=" + startPlannedCapacity + "Ah, endPlannedCapacity=" + endPlannedCapacity + "Ah");
		
		assertEquals(32, car.getCurrentPlan()[0], 1e-8);
		assertEquals(0, car.getCurrentPlan()[1], 1e-8); // Was blocked
		assertEquals(currentAtTimeslot3, car.getCurrentPlan()[2], 1e-8); // current from k=4 should be reassigned here AND UPDATED TO 31A
		assertEquals(currentAtTimeslot4, car.getCurrentPlan()[3], 1e-8); // current at k=3 should be UPDATED TO 23A
		assertEquals(0, car.getCurrentPlan()[4], 1e-8); // Violated
		assertEquals(0, car.getCurrentPlan()[5], 1e-8); // Should be unchanged
		
		assertEquals(startPlannedCapacity, endPlannedCapacity, 1e-8);
		
	}
	
	
	
	
	
	
	@Test
	public void rescheduleCarTest_Nonlinear_RescheduleCarWithGreater0A_ToLaterTimeslotWith0A() {
		// Reassign current from one timeslot to another timeslot with 0A

		// [32, 0, 31, 23.621, 0] (23 is violated by 14A)
		// should result in
		// [32, 0, 31, 9.621, 8.5]
		
		int violatingK = 3;
		// Should be rescheduled to 3rd timeslot
		boolean[] blockedTimeslots = new boolean[96];
		blockedTimeslots[1] = true; 
		blockedTimeslots[violatingK] = true;
		
		// 0 already has 32A
		// 1 is blocked
		// 2 is violated, so current from 2 should be reassigned to 3
		// 3 is unplanned (current=0)
				
		
		// 4 was already planned (< 32A) BUT SHOULD BE UPDATED. Then we don't need linear approximation! We are free to REDUCE current
		
		car.setCurrentPlan(new double[96]);
		strategyNonlinear.getScheduler().fillChargingPlan(car, chargingStation, car.getMaxCapacity(), 
				TimeslotSorter.getSortedTimeslots(null, 0, 5, TimeslotSortingCriteria.INDEX, new boolean[] {false, true, false, false, true}), 
				0);
		
		double currentAtTimeslot2 = car.getCurrentPlan()[2];
		double currentAtTimeslot3 = car.getCurrentPlan()[3];
		
		
		Fuse fuse = new Fuse(0, 50);
		FuseTreeException fuseTreeException = new FuseTreeException(fuse, new double[] {64, 64, 64}, violatingK);
		strategyNonlinear.setRescheduleCarsWith0A(false);
		
		
		double startPlannedCapacity = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation, car, 0);
		strategyNonlinear.rescheduleCar(simulationNonlinear.getState(), carAssignment, 
				blockedTimeslots, violatingK, fuseTreeException);
		double endPlannedCapacity = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation, car, 0);
		
		assertEquals(32, car.getCurrentPlan()[0], 1e-8);
		assertEquals(0, car.getCurrentPlan()[1], 1e-8); // 1 was blocked
		assertEquals(currentAtTimeslot2, car.getCurrentPlan()[2], 1e-8); // Should not be changed
		assertEquals(currentAtTimeslot3 - 14, car.getCurrentPlan()[3], 1e-8); // Should be reduced by 14A
		assertTrue(car.getCurrentPlan()[4] > 0);
		
		assertEquals(startPlannedCapacity, endPlannedCapacity, 1e-8);
	}
	
	
	
	@Test
	public void rescheduleCarTest_Nonlinear_RescheduleCarWithGreater0A_ToLaterTimeslotGreater0A() {

		// [32, 0, 31.621, 23, 0] (31 is violated by 14A)
		// should result in
		// [32, 0, 17.621, >23.621, >0]
		
		int violatingK = 2;
		// Should be rescheduled to 3rd timeslot
		boolean[] blockedTimeslots = new boolean[96];
		blockedTimeslots[1] = true; 
		blockedTimeslots[violatingK] = true;
		
		car.setCurrentPlan(new double[96]);
		strategyNonlinear.getScheduler().fillChargingPlan(car, chargingStation, car.getMaxCapacity(), 
				TimeslotSorter.getSortedTimeslots(null, 0, 5, TimeslotSortingCriteria.INDEX, new boolean[] {false, true, false, false, true}), 
				0);
		
		double currentAtTimeslot2 = car.getCurrentPlan()[2];
		double currentAtTimeslot3 = car.getCurrentPlan()[3];
		
		Fuse fuse = new Fuse(0, 50);
		FuseTreeException fuseTreeException = new FuseTreeException(fuse, new double[] {64, 64, 64}, violatingK);
		strategyNonlinear.setRescheduleCarsWith0A(false);
		
		
		double startPlannedCapacity = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation, car, 0);
		strategyNonlinear.rescheduleCar(simulationNonlinear.getState(), carAssignment, 
				blockedTimeslots, violatingK, fuseTreeException);
		double endPlannedCapacity = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation, car, 0);
		
		assertEquals(32, car.getCurrentPlan()[0], 1e-8);
		assertEquals(0, car.getCurrentPlan()[1], 1e-8); // 1 was blocked
		assertEquals(currentAtTimeslot2 - 14, car.getCurrentPlan()[2], 1e-8); // Should be reduced by 14A
		assertTrue(car.getCurrentPlan()[3] > currentAtTimeslot3);
		assertTrue(car.getCurrentPlan()[4] > 0);
		
		assertEquals(startPlannedCapacity, endPlannedCapacity, 1e-8);
	}
	
	
	
	
	
	@Test
	public void rescheduleCarTest_Nonlinear_RescheduleCarWithGreater0A_ToEarlierTimeslotWithTo0A() {
		
		// [32, 0, 31, 0, 23] (23 is violated by 14A)
		// should result in
		// [32, 0, 31, >0, 9]
		
		int violatingK = 4;
		boolean[] blockedTimeslots = new boolean[96];
		blockedTimeslots[1] = true; 
		blockedTimeslots[violatingK] = true;
		
		car.setCurrentPlan(new double[96]);
		strategyNonlinear.getScheduler().fillChargingPlan(car, chargingStation, car.getMaxCapacity(), 
				TimeslotSorter.getSortedTimeslots(null, 0, 5, TimeslotSortingCriteria.INDEX, new boolean[] {false, true, false, true, false}), 
				0);
		
		double currentAtTimeslot2 = car.getCurrentPlan()[2];
		double currentAtTimeslot4 = car.getCurrentPlan()[4];
		
		Fuse fuse = new Fuse(0, 50);
		FuseTreeException fuseTreeException = new FuseTreeException(fuse, new double[] {64, 64, 64}, violatingK);
		strategyNonlinear.setRescheduleCarsWith0A(false);
		
		
		double startPlannedCapacity = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation, car, 0);
		strategyNonlinear.rescheduleCar(simulationNonlinear.getState(), carAssignment, 
				blockedTimeslots, violatingK, fuseTreeException);
		double endPlannedCapacity = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation, car, 0);
		
		assertEquals(32, car.getCurrentPlan()[0], 1e-8);
		assertEquals(0, car.getCurrentPlan()[1], 1e-8); // 1 was blocked
		assertEquals(currentAtTimeslot2, car.getCurrentPlan()[2], 1e-8); // 2 had 32A originally
		assertTrue(car.getCurrentPlan()[3] > 0); 
		assertEquals(currentAtTimeslot4 - 14, car.getCurrentPlan()[4], 1e-8);
		
		assertEquals(startPlannedCapacity, endPlannedCapacity, 1e-8);
		
	}
	
	
	@Test
	public void handleViolation_Nonlinear_RescheduleWithGreater0A() {
		
		int violatingK = 0;
		
		boolean[] blockedTimeslots = new boolean[96];
		blockedTimeslots[violatingK] = true;
		blockedTimeslots[1] = true;
		
		car.setCurrentPlan(new double[96]);
		car.getCurrentPlan()[0] = 32;
		car.carBattery.addChargedCapacity(1, 32); // Car1 should be rescheduled since it has lower priority
		
		car2.setCurrentPlan(new double[96]);
		car2.getCurrentPlan()[0] = 32;
		
		
		Map<Integer, FuseTreeException> violatingTimeslots = new HashMap<>();
		Fuse rootFuse = data.getFuseTree().getRootFuse();
		double exceptionValue = rootFuse.getFusePhase(Phase.PHASE_1) + 64;
		FuseTreeException fuseTreeException = new FuseTreeException(rootFuse, new double[] {exceptionValue, exceptionValue, exceptionValue}, violatingK);
		violatingTimeslots.put(violatingK, fuseTreeException);
		
		double startPlannedCapacity1 = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation, car, 0);
		double startPlannedCapacity2 = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation2, car2, 0);
		
		strategyNonlinear.setRescheduleCarsWith0A(false);
		strategyNonlinear.handleViolation(stateNonlinear, violatingK, blockedTimeslots, violatingTimeslots);
		
		double endPlannedCapacity1 = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation, car, 0);
		double endPlannedCapacity2 = strategyNonlinear.getScheduler().getPlannedCapacity(chargingStation2, car2, 0);
		
		// Planned capacity should still be the same for both cars
		assertEquals(startPlannedCapacity1, endPlannedCapacity1, 1e-8);
		assertEquals(startPlannedCapacity2, endPlannedCapacity2, 1e-8);
		
	}
	
	
	@Test
	public void nonlinearStrategy_NonlinearBattery() {

		SimulationListenerCSV csvListener = new SimulationListenerCSV();
		
		for (Car car : data.getCars()) {
			car.setCurrentPlan(new double[96]);
		}
		
		strategyNonlinear.getScheduler().setEnablePlannedCapacityCache(true);
		simulationNonlinear.addStateListener(csvListener);
		simulationNonlinear.simulate();
		
		/*System.out.println("Charged capacity: " + car.getChargedCapacity() + "/" + car.getMaxCapacity() + " (soc=" + car.carBattery.getSoC() + ")");
		
		System.out.println("nSimulations=" + BatterySim.nSimulations +  ", nSimulationSteps=" + BatterySim.nSimulationSteps + 
				", average steps: " + Math.ceil(1.0*BatterySim.nSimulationSteps/BatterySim.nSimulations) + 
				", nCacheEntries: " + strategyNonlinear.getScheduler().getPlannedCapacityHashMap().size() + 
				", nCacheHits: " + strategyNonlinear.getScheduler().plannedCapacityHashMapHits);*/
		
		/*for (String key : strategyNonlinear.getScheduler().getPlannedCapacityHashMap().keySet()) {
			System.out.println(key + " : " + strategyNonlinear.getScheduler().getPlannedCapacityHashMap().get(key));
		}*/
		/*for (int i=0;i<strategyNonlinear.getScheduler().getPlannedCapacityHashMap().keySet().size();i++) {
			System.out.println(key + " : " + strategyNonlinear.getScheduler().getPlannedCapacityHashMap().get(key));
		}*/
		//System.out.println(strategyNonlinear.getScheduler().getPlannedCapacityHashMap().keySet().stream().findFirst().get());
		
		
		//FileIO.writeFile("gen/data/NonlinearSimulationWithNonlinearBattery.json", simulationNonlinear.getSimulationResult().getSolvedProblemInstanceJSON());
		//FileIO.writeFile("gen/data/NonlinearSimulationWithNonlinearBattery.csv", csvListener.getCSVString());
		
	}
	
	
	
	@Test
	public void linearStrategy_NonlinearBattery() {
		
		SimulationListenerCSV csvListener = new SimulationListenerCSV();
		
		simulationLinear.addStateListener(csvListener);
		simulationLinear.simulate();
		
		//System.out.println("Charged capacity: " + car.getChargedCapacity() + "/" + car.getMaxCapacity() + " (soc=" + car.carBattery.getSoC() + "), efficiency=" + CONSTANTS.CHARGING_EFFICIENCY);
		
		//FileIO.writeFile("gen/data/LinearSimulationWithNonlinearBattery.json", simulationLinear.getSimulationResult().getSolvedProblemInstanceJSON());
		//FileIO.writeFile("gen/data/LinearSimulationWithNonlinearBattery.csv", csvListener.getCSVString());
		
	}
	
	
	
	
	
	
}
