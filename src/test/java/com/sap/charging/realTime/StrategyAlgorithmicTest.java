package com.sap.charging.realTime; 


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.charging.model.Car;
import com.sap.charging.model.CarFactory;
import com.sap.charging.model.CarFactory.CarModel;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.model.Fuse;
import com.sap.charging.model.FuseTree;
import com.sap.charging.model.FuseTreeNode;
import com.sap.charging.realTime.model.CarAssignment;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureForecast;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureForecastMedianTimestamp;
import com.sap.charging.realTime.util.TimeslotSorter;
import com.sap.charging.realTime.util.TimeslotSortingCriteria;
import com.sap.charging.sim.Simulation;
import com.sap.charging.sim.common.SimulationUnitTest;
import com.sap.charging.sim.eval.exception.FuseTreeException;
import com.sap.charging.util.SortableElement;
import com.sap.charging.util.TimeUtil;

public class StrategyAlgorithmicTest extends SimulationUnitTest {

    private StrategyAlgorithmic strategy;
    private StrategyAlgorithmicChargeScheduler scheduler; 
    private State state; 
    
    private Car car1;
    private Car car2;
    private Car car3;
    private Car car4;
    private ChargingStation chargingStation1;
    private ChargingStation chargingStation2;
    private ChargingStation chargingStation3;
    private ChargingStation chargingStation4;

    private FuseTree fuseTree;

    @BeforeEach
    public void setup() {
        for (Car car : dataSim.getCars()) {
            car.setCurrentPlan(new double[dataSim.getEnergyPriceHistory().getNTimeslots()]);
        }

        strategy = new StrategyAlgorithmic();
        strategy.setRescheduleCarsWith0A(false);
        strategy.objectiveEnergyCosts.setWeight(0);
        strategy.objectiveFairShare.setWeight(1);
        scheduler = strategy.getScheduler(); 
        
        Simulation.verbosity = 0; 
        sim = new Simulation(dataSim, strategy); 
        sim.init(); 
        state = sim.getState(); 

        car1 = CarFactory.builder()
                .set(CarModel.TESLA_MODEL_S)
                .availableTimeslots(32, 68, 96)
                .availableTimestamps(LocalTime.ofSecondOfDay(32 * 15 * 60), LocalTime.ofSecondOfDay(68 * 15 * 60))
                .id(0)
                .build();
        car1.setCurrentPlan(new double[96]);
        car2 = CarFactory.builder()
                .set(CarModel.TESLA_MODEL_S)
                .availableTimeslots(32, 68, 96)
                .availableTimestamps(LocalTime.ofSecondOfDay(32 * 15 * 60), LocalTime.ofSecondOfDay(68 * 15 * 60))
                .id(1)
                .build();
        car2.setCurrentPlan(new double[96]);
        car3 = CarFactory.builder()
                .set(CarModel.TESLA_MODEL_S)
                .availableTimeslots(32, 68, 96)
                .availableTimestamps(LocalTime.ofSecondOfDay(32 * 15 * 60), LocalTime.ofSecondOfDay(68 * 15 * 60))
                .id(2)
                .build();
        car3.setCurrentPlan(new double[96]);
        car4 = CarFactory.builder()
                .set(CarModel.TESLA_MODEL_S)
                .availableTimeslots(32, 68, 96)
                .availableTimestamps(LocalTime.ofSecondOfDay(32 * 15 * 60), LocalTime.ofSecondOfDay(68 * 15 * 60))
                .id(3)
                .build();
        car4.setCurrentPlan(new double[96]);
        
        car1.setIdealCar(true);
        car2.setIdealCar(true);
        car3.setIdealCar(true);
        car4.setIdealCar(true);

        chargingStation1 = dataSim.getChargingStation(0); 
        chargingStation2 = dataSim.getChargingStation(1); 
        chargingStation3 = dataSim.getChargingStation(2); 
        chargingStation4 = dataSim.getChargingStation(3); 


        Fuse fuseRoot = new Fuse(0, 100);
        // 1st branch
        Fuse fuseParent = new Fuse(1, 100);
        fuseRoot.addChild(fuseParent);
        Fuse fuseChild = new Fuse(2, 50);
        fuseChild.addChild(chargingStation1);
        fuseChild.addChild(chargingStation2);
        fuseParent.addChild(fuseChild);

        // 2nd branch
        Fuse fuseParent2 = new Fuse(3, 100);
        fuseRoot.addChild(fuseParent2);
        Fuse fuseChild2 = new Fuse(4, 50);
        fuseChild2.addChild(chargingStation3);
        fuseChild2.addChild(chargingStation4);
        fuseParent2.addChild(fuseChild2);

        fuseTree = new FuseTree(fuseRoot, 999);
        state.fuseTree = fuseTree; 

    }
    

    @Test
    public void testConstructor() {
        strategy = new StrategyAlgorithmic(CarDepartureForecast.getDefaultCarDepartureForecast());
        assertTrue(strategy.getCarDepartureForecast() instanceof CarDepartureForecastMedianTimestamp);
    }


    @Test
    public void testGetMethod() {
        assertEquals("realTimeAlgorithmic", strategy.getMethod());
    }

    /*****************************
     * Single car charging plans
     *****************************/

    @Test
    public void testChargingPriority() {
    	state.currentTimeSeconds = Math.max(car1.timestampArrival.toSecondOfDay(), car2.timestampArrival.toSecondOfDay()); 
    	state.currentTimeslot = TimeUtil.getTimeslotFromSeconds(state.currentTimeSeconds); 
    	
    	CarAssignment carAssignment1 = state.addCarAssignment(car1, chargingStation1); 
    	CarAssignment carAssignment2 = state.addCarAssignment(car2, chargingStation2); 
    	
    	
        int departureCar1 = 100;
        int departureCar2 = 200;
        double priority1 = strategy.getChargingPriority(carAssignment1, 50, departureCar1);
        double priority2 = strategy.getChargingPriority(carAssignment2, 50, departureCar2);

        // Priority1 should be higher since car needs to leave earlier
        assertTrue(priority1 > priority2);

        // Car that is already over minSoC should be lower in priority
        car2.setCurrentCapacity(car2.minLoadingState + 1);
        double priority3 = strategy.getChargingPriority(carAssignment1, 50, departureCar1);
        double priority4 = strategy.getChargingPriority(carAssignment2, 50, departureCar2);

        assertTrue(priority3 > priority4);

    }

    @Test
    public void testFillChargingPlan() {
        // Test whether charging plans are correctly filled
        car1.getCurrentPlan()[1] = 32;
        double firstPlanned = scheduler.getPlannedCapacity(chargingStation1, car1, car1.timestampArrival.toSecondOfDay());

        double desiredCapacity = 50;

        scheduler.fillChargingPlan(car1, chargingStation1, desiredCapacity, TimeslotSorter.getSortedTimeslots(state, 0, 95, TimeslotSortingCriteria.INDEX), car1.timestampArrival.toSecondOfDay()); 

        double secondPlanned = scheduler.getPlannedCapacity(chargingStation1, car1, car1.timestampArrival.toSecondOfDay());
        // Difference in planned should be desiredCapacity
        double diff = secondPlanned - firstPlanned;
        assertEquals(desiredCapacity, diff, 1e-8);
    }
    
    /*@Test
    public void testFillChargingPlanImmediateStartNeeded_atArrival() {
        // Test whether charging plans are correctly filled with car that needs an immediate start
        car1 = CarFactory.builder()
                .set(CarModel.TESLA_MODEL_S)
                .availableTimeslots(32, 68, 96)
                .availableTimestamps(LocalTime.ofSecondOfDay(32 * 15 * 60), // Make sure car arrives on chargeScheduleSet date
                                     LocalTime.ofSecondOfDay(68 * 15 * 60))
                .id(0)
                .immediateStart(true)
                .suspendable(true)
                .canUseVariablePower(true)
                .build();
        car1.setCurrentPlan(new double[96]);
        
        double desiredCapacity = 100;
        
        List<SortableElement<Integer>> sortedTimeslotsReverse = TimeslotSorter.getSortedTimeslotsByIndex(32, 52); 
        Collections.reverse(sortedTimeslotsReverse);
        
        scheduler.fillChargingPlan(car1, chargingStation1, desiredCapacity, sortedTimeslotsReverse, car1.timestampArrival.toSecondOfDay()); 
        
        // Check whether plan[0] was filled even though plan[19] was prioritized via reverse
        assertEquals(32, car1.getCurrentPlan()[0], 1e-8);  
        assertEquals(0, car1.getCurrentPlan()[1], 1e-8);  
        assertEquals(32, car1.getCurrentPlan()[19], 1e-8);  
        assertEquals(32, car1.getCurrentPlan()[18], 1e-8);  
        
    }
    
    @Test
    public void testFillChargingPlanImmediateStartNeeded_chargingStartedtooLate() {        
        
        // car needs immediateStart but did not receive during first arrival
        // Should not receive a plan at all
        car1 = CarFactory.builder()
                .set(CarModel.TESLA_MODEL_S)
                .availableTimeslots(32, 68, 96)
                .availableTimestamps(LocalTime.ofSecondOfDay(32*15*60), // Make sure car arrives on chargeScheduleSet date
                                     LocalTime.ofSecondOfDay(68 * 15 * 60))
                .id(0)
                .immediateStart(true)
                .suspendable(true)
                .canUseVariablePower(true)
                .build();
        car1.setCurrentPlan(new double[96]);
        
        assertEquals(0, car1.getChargedCapacity(), 1e-8); // requirement for validity of test
        
        List<SortableElement<Integer>> sortedTimeslots = TimeslotSorter.getSortedTimeslotsByIndex(40, 50); 
        double desiredCapacity = 100;
        scheduler.fillChargingPlan(car1, chargingStation1, desiredCapacity, sortedTimeslots, 40*15*60); 
        
        for (int i=0;i<car1.getCurrentPlan().length;i++) {
            assertEquals(0, car1.getCurrentPlan()[i], 1e-8);  
        }
           
    }
    
    @Test
    public void testFillChargingPlanImmediateStartNeeded_chargingStarted() {
        
        // Charging had already started at some point, should receive a plan as usual
        car1 = CarFactory.builder()
                .set(CarModel.TESLA_MODEL_S)
                .availableTimeslots(32, 68, 96)
                .availableTimestamps(LocalTime.ofSecondOfDay(32*15*60), // Make sure car arrives on chargeScheduleSet date
                                     LocalTime.ofSecondOfDay(68 * 15 * 60))
                .id(0)
                .immediateStart(true)
                .suspendable(true)
                .canUseVariablePower(true)
                .build();
        car1.setCurrentPlan(new double[96]);
        car1.addChargedCapacity(3600, 10);
        
        double desiredCapacity = 100;
        
        List<SortableElement<Integer>> sortedTimeslotsReverse = TimeslotSorter.getSortedTimeslotsByIndex(32, 52);
        Collections.reverse(sortedTimeslotsReverse);
        
        scheduler.fillChargingPlan(car1, chargingStation1, desiredCapacity, sortedTimeslotsReverse, 40*15*60); 
        
        
        assertEquals(0, car1.getCurrentPlan()[0], 1e-8);  
        assertEquals(32, car1.getCurrentPlan()[18], 1e-8);  
        assertEquals(32, car1.getCurrentPlan()[19], 1e-8);  
        
    }
    
    @Test
    public void testFillChargingPlanImmediateStartNeeded_timeslotBlocked() {
        
        // First timeslot is blocked, "tough luck" (car will not be charged at all)
        car1 = CarFactory.builder()
                .set(CarModel.TESLA_MODEL_S)
                .availableTimeslots(32, 68, 96)
                .availableTimestamps(LocalTime.ofSecondOfDay(32*15*60), // Make sure car arrives on chargeScheduleSet date
                                     LocalTime.ofSecondOfDay(68 * 15 * 60))
                .id(0)
                .immediateStart(true)
                .suspendable(true)
                .canUseVariablePower(true)
                .build();
        car1.setCurrentPlan(new double[96]);
       
        double desiredCapacity = 100;
        
        List<SortableElement<Integer>> sortedTimeslotsReverse = TimeslotSorter.getSortedTimeslotsByIndex(33, 52);
        Collections.reverse(sortedTimeslotsReverse);
        
        scheduler.fillChargingPlan(car1, chargingStation1, desiredCapacity, sortedTimeslotsReverse, car1.timestampArrival.toSecondOfDay()+900); 
        
        for (int i=0;i<car1.getCurrentPlan().length;i++) {
            assertEquals(0, car1.getCurrentPlan()[i], 1e-8);  
        }
    }
    
    
    
    @Test
    public void testFillChargingPlanNotSuspendable_onArrival() {
        // Car is not charging yet (current = 0) but is on arrival so should receive a plan as usual
        car1 = CarFactory.builder()
                .set(CarModel.TESLA_MODEL_S)
                .availableTimeslots(32, 68, 96)
                .availableTimestamps(LocalTime.ofSecondOfDay(32*15*60), // Make sure car arrives on chargeScheduleSet date
                                     LocalTime.ofSecondOfDay(68 * 15 * 60))
                .id(0)
                .immediateStart(false)
                .suspendable(false)
                .canUseVariablePower(true)
                .build();
        car1.setCurrentPlan(new double[96]);

        double desiredCapacity = 100;
        List<SortableElement<Integer>> sortedTimeslotsReverse = TimeslotSorter.getSortedTimeslotsByIndex(32, 52);
        Collections.reverse(sortedTimeslotsReverse);
        
        scheduler.fillChargingPlan(car1, chargingStation1, desiredCapacity, sortedTimeslotsReverse, car1.timestampArrival.toSecondOfDay()); 
        
        assertEquals(32, car1.getCurrentPlan()[0], 1e-8);
        assertEquals(32, car1.getCurrentPlan()[1], 1e-8);
        assertEquals(0, car1.getCurrentPlan()[19], 1e-8);          
        
    }

    @Test
    public void testFillChargingPlanNotSuspendable_isCharging() {
        // Car is charging (current > 0), should receive a plan as usual since plan has never been interrupted
        car1 = CarFactory.builder()
                .set(CarModel.TESLA_MODEL_S)
                .availableTimeslots(32, 68, 96)
                .availableTimestamps(LocalTime.ofSecondOfDay(32*15*60), // Make sure car arrives on chargeScheduleSet date
                                     LocalTime.ofSecondOfDay(68 * 15 * 60))
                .id(0)
                .immediateStart(false)
                .suspendable(false)
                .canUseVariablePower(true)
                .build();
        car1.addChargedCapacity(3600, 10);
        car1.setCurrentPlan(new double[96]);
        double[] currentPlan = car1.getCurrentPlan();
        currentPlan[32] = 20;
        
        double desiredCapacity = 100;
        
        List<SortableElement<Integer>> sortedTimeslots = TimeslotSorter.getSortedTimeslotsByIndex(33, 52);
        
        scheduler.fillChargingPlan(car1, chargingStation1, desiredCapacity, sortedTimeslots, car1.timestampArrival.toSecondOfDay()+900); 
        
        assertEquals(32, car1.getCurrentPlan()[0], 1e-8);
        assertEquals(32, car1.getCurrentPlan()[1], 1e-8);
        assertEquals(0, car1.getCurrentPlan()[19], 1e-8);          
        
    }
    
    @Test
    public void testFillChargingPlanNotSuspendable_hasBeenInterrupted() {
        // Car was charged in the past (current > 0) but has been interrupted (current = 0 now)
        // Should never receive a plan with current > 0
        car1 = CarFactory.builder()
                .set(CarModel.TESLA_MODEL_S)
                .availableTimeslots(32, 68, 96)
                .availableTimestamps(chargeScheduleSet.getScheduleStartDate().toLocalTime(), // Make sure car arrives on chargeScheduleSet date
                                     LocalTime.ofSecondOfDay(68 * 15 * 60))
                .id(0)
                .immediateStart(false)
                .suspendable(false)
                .canUseVariablePower(true)
                .build();
        car1.addChargedCapacity(10);
        car1.setCurrentPlan(new double[96]);
        
        double desiredCapacity = 100;
        
        List<SortableElement<Integer>> sortedTimeslotsReverse = strategy.getSortedTimeslotsByIndex(0, 20);
        Collections.reverse(sortedTimeslotsReverse);
        
        strategy.fillChargingPlan(car1, chargingStation1, desiredCapacity, sortedTimeslotsReverse,
                                  chargeScheduleSet);
        
        for (int i=0;i<car1.getCurrentPlan().length;i++) {
            //assertEquals(0, car1.getCurrentPlan()[i], 1e-8);
        }
        // This test has been disabled because suspendable is not 100% observed in practice. Sometimes EVs will still pick up new plans
    }
    
    
    @Test
    public void testFillChargingPlanNotSuspendable_blockedTimeslots() {
        // Car is currently charging (current > 0) but cannot charge after timeslot = 2 since further timeslots are blocked
        
        car1 = CarFactory.builder()
                .set(CarModel.TESLA_MODEL_S)
                .availableTimeslots(32, 68, 96)
                .availableTimestamps(chargeScheduleSet.getScheduleStartDate().toLocalTime(), // Make sure car arrives on chargeScheduleSet date
                                     LocalTime.ofSecondOfDay(68 * 15 * 60))
                .id(0)
                .immediateStart(false)
                .suspendable(false)
                .canUseVariablePower(true)
                .build();
        car1.addChargedCapacity(10);
        car1.setCurrentPlan(new double[96]);
        car1.getCurrentPlan()[0] = 32;
        
        double desiredCapacity = 100;
        
        // simulates timeslot=2 is blocked
        List<SortableElement<Integer>> sortedTimeslotsReverse = strategy.getSortedTimeslotsByIndex(0, 20);
        Collections.reverse(sortedTimeslotsReverse);
        sortedTimeslotsReverse.removeIf(e -> e.index == 2);
        
        strategy.fillChargingPlan(car1, chargingStation1, desiredCapacity, sortedTimeslotsReverse,
                                  chargeScheduleSet);
        
        assertEquals(32, car1.getCurrentPlan()[0], 1e-8);
        assertEquals(32, car1.getCurrentPlan()[1], 1e-8);
        assertEquals(0, car1.getCurrentPlan()[2], 1e-8);
        assertEquals(0, car1.getCurrentPlan()[3], 1e-8);
        assertEquals(0, car1.getCurrentPlan()[4], 1e-8);
        assertEquals(0, car1.getCurrentPlan()[19], 1e-8);     
        
    }
    
    
    
    
    @Test
    public void testFillChargingPlanNoVariablePower() {
        car1 = CarFactory.builder()
                .set(CarModel.TESLA_MODEL_S)
                .availableTimeslots(32, 68, 96)
                .availableTimestamps(chargeScheduleSet.getScheduleStartDate().toLocalTime(), // Make sure car arrives on chargeScheduleSet date
                                     LocalTime.ofSecondOfDay(68 * 15 * 60))
                .id(0)
                .immediateStart(false)
                .suspendable(true)
                .canUseVariablePower(false)
                .build();
        car1.setCurrentPlan(new double[96]);
        
        double desiredCapacity = 100;
        
        strategy.fillChargingPlan(car1, chargingStation1, desiredCapacity, strategy.getSortedTimeslotsByIndex(0, 10),
                                  chargeScheduleSet);
        
        assertEquals(32, car1.getCurrentPlan()[0], 1e-8);
        assertEquals(32, car1.getCurrentPlan()[1], 1e-8);
        assertEquals(32, car1.getCurrentPlan()[2], 1e-8);
        assertEquals(32, car1.getCurrentPlan()[5], 1e-8);
        
        
        // Should be more than originally planned because last timeslot is not filled exactly but with max current
        double planned = strategy.getPlannedCapacity(car1, chargeScheduleSet);
        assertEquals(true, planned > desiredCapacity);
    }
    */
    
    @Test
    public void testFillChargingPlanWithMinCurrent() {
    	
        // A timeslot is hit that would be below min current
        car1.getCurrentPlan()[car1.getFirstAvailableTimeslot()] = 32;
        double firstPlanned = scheduler.getPlannedCapacity(chargingStation1, car1, car1.timestampArrival.toSecondOfDay()); 

        double desiredCapacity = 0.1;

        List<SortableElement<Integer>> sortedTimeslots = TimeslotSorter.getSortedTimeslotsByIndex(car1.getFirstAvailableTimeslot(), 95);
        scheduler.fillChargingPlan(car1, chargingStation2, desiredCapacity, sortedTimeslots, car1.timestampArrival.toSecondOfDay()); 

        double secondPlanned = scheduler.getPlannedCapacity(chargingStation1, car1, car1.timestampArrival.toSecondOfDay()); 

        // Difference in planned should be higher than desiredCapacity
        double diff = secondPlanned - firstPlanned;

        assertTrue(diff > desiredCapacity);
        assertEquals(32, car1.getCurrentPlan()[car1.getFirstAvailableTimeslot()], 1e-8); // Check that first slot receives min current
        assertEquals(car1.minCurrentPerPhase, car1.getCurrentPlan()[car1.getFirstAvailableTimeslot()+1], 1e-8); // Check that first slot receives min current
    }
    
    @Test
    public void fillChargingPlan_givenAChargingStationWithSmallerFuseThanMinCurrentPerPhaseOfCar_setsSlotsTo0Amps() {
        ChargingStation station = new ChargingStation();
        station.fusePhase1 = car1.minCurrentPerPhase - 1;
        station.fusePhase2 = car1.minCurrentPerPhase - 1;
        station.fusePhase3 = car1.minCurrentPerPhase - 1;
        
        scheduler.fillChargingPlan(car1, station, 1.0, TimeslotSorter.getSortedTimeslotsByIndex(car1.getFirstAvailableTimeslot(), 95), car1.timestampArrival.toSecondOfDay());

        assertEquals(car1.getCurrentPlan()[0], 0.0, 1e-8);
    }

    @Test
    public void fillChargingPlan_givenAChargingStationWithSmallerFuseThanOldPlanValue_correctsOldValue() {
        car1.getCurrentPlan()[car1.getFirstAvailableTimeslot()] = 32;

        ChargingStation station = new ChargingStation();
        station.fusePhase1 = 16;
        station.fusePhase2 = 16;
        station.fusePhase3 = 16;
        
        scheduler.fillChargingPlan(car1, station, 1.0, TimeslotSorter.getSortedTimeslotsByIndex(car1.getFirstAvailableTimeslot(), 95), car1.timestampArrival.toSecondOfDay());

        assertEquals(car1.getCurrentPlan()[car1.getFirstAvailableTimeslot()], 16.0, 1e-8);
    }
    
    @Test
    public void fillChargingPlan_ParentFuseLowerThanChargingStation() {
    	
        Fuse fuseRoot = new Fuse(0, 30);
        fuseRoot.addChild(chargingStation1);

        fuseTree = new FuseTree(fuseRoot, 999);
        state.fuseTree = fuseTree; 
        state.currentTimeSeconds = car1.timestampArrival.toSecondOfDay();
        state.currentTimeslot = TimeUtil.getTimeslotFromSeconds(state.currentTimeSeconds); 
        state.addCarAssignment(car1, chargingStation1); 
        
        
        scheduler.fillChargingPlan(car1, chargingStation1, 50.0, TimeslotSorter.getSortedTimeslotsByIndex(car1.getFirstAvailableTimeslot(), 95), car1.timestampArrival.toSecondOfDay());
        
        int violatingK = car1.getFirstAvailableTimeslot();
        Map<Integer, FuseTreeException> violatingTimeslots = strategy.getInitialFuseViolations(state, car1); 
        
        boolean[] blockedTimeslots = new boolean[96];
        blockedTimeslots[violatingK] = true; 
        
        strategy.handleViolation(state, violatingK, blockedTimeslots, violatingTimeslots);
        
        assertEquals(30, car1.getCurrentPlan()[violatingK], 1e-8);
    }

    @Test
    public void testFillChargingPlanNotEnoughTimeslots() {
        double firstPlanned = scheduler.getPlannedCapacity(chargingStation1, car1, car1.timestampArrival.toSecondOfDay()); 

        double desiredCapacity = car1.getMaxCapacity();

        scheduler.fillChargingPlan(car1, chargingStation1, desiredCapacity, TimeslotSorter.getSortedTimeslotsByIndex(car1.getFirstAvailableTimeslot(), 
        		car1.getFirstAvailableTimeslot()+2), car1.timestampArrival.toSecondOfDay()); 

        double secondPlanned = scheduler.getPlannedCapacity(chargingStation1, car1, car1.timestampArrival.toSecondOfDay()); 
        // 2 timeslots should not be enough to get the desired full capacity
        double diff = secondPlanned - firstPlanned;
        assertTrue(diff < desiredCapacity);
    }

    @Test
    public void testFillChargingPlanToMinSoC() {
        // Make it more difficult by having spots in the plan filled already
        car1.getCurrentPlan()[car1.getFirstAvailableTimeslot()] = 32;
        car1.getCurrentPlan()[car1.getFirstAvailableTimeslot()+1] = 20;

        scheduler.fillChargingPlanToMinSoC(state, car1.getFirstAvailableTimeslot(), 95, car1, chargingStation1, car1.timestampArrival.toSecondOfDay());
        double planned = scheduler.getPlannedCapacity(chargingStation1, car1, car1.timestampArrival.toSecondOfDay()); 

        assertEquals(32.0, car1.getCurrentPlan()[car1.getFirstAvailableTimeslot()+1], 1e-8); 
        assertTrue(planned >= car1.minLoadingState);
    }

    @Test
    public void testFillCarAtMinSoC() {
    	car1.setCurrentCapacity(car1.minLoadingState + 1); 
        scheduler.fillChargingPlanToMinSoC(state, car1.getFirstAvailableTimeslot(), 95, car1, chargingStation1, car1.timestampArrival.toSecondOfDay());
        
        double planned = scheduler.getPlannedCapacity(chargingStation1, car1, car1.timestampArrival.toSecondOfDay()); 

        assertEquals(0, planned, 1e-8);
    }

    @Test
    public void testFillChargingPlanByCost() {
        car1.getCurrentPlan()[car1.getFirstAvailableTimeslot()] = 32;
        car1.getCurrentPlan()[car1.getFirstAvailableTimeslot()+1] = 20;
        
        scheduler.fillChargingPlanByCost(state, car1.getFirstAvailableTimeslot(), 95, car1, chargingStation1, car1.timestampArrival.toSecondOfDay());
        double planned = scheduler.getPlannedCapacity(chargingStation1, car1, car1.timestampArrival.toSecondOfDay()); 

        assertTrue(planned >= car1.getMaxCapacity()); 
        assertEquals(car1.getMaxCapacity(), planned, 1);
    }

    @Test
    public void testFillFullCarByCost() {
        car1.setCurrentCapacity(car1.getMaxCapacity());
        scheduler.fillChargingPlanByCost(state, car1.getFirstAvailableTimeslot(), 95, car1, chargingStation1, car1.timestampArrival.toSecondOfDay());
        
        double planned = scheduler.getPlannedCapacity(chargingStation1, car1, car1.timestampArrival.toSecondOfDay()); 
        assertEquals(0, planned, 1e-8);
    }

    /*********************************
     * Resolving violations that stem from filling charging plans
     *********************************/
    @Test
    public void testRescheduleCar() {
    	
    	state.currentTimeSeconds = car1.timestampArrival.toSecondOfDay(); 
    	state.currentTimeslot = TimeUtil.getTimeslotFromSeconds(state.currentTimeSeconds); 
    	CarAssignment carAssignment1 = state.addCarAssignment(car1, chargingStation1); 
    	
        Fuse smallFuse = new Fuse(0, 50);
        int violatingTimeslot = car1.getFirstAvailableTimeslot();
        FuseTreeException fuseTreeException = new FuseTreeException(smallFuse, new double[] {64, 64, 64}, violatingTimeslot);
        
        car1.getCurrentPlan()[violatingTimeslot] = 32;

        boolean[] blockedTimeslots = new boolean[96];
        blockedTimeslots[violatingTimeslot] = true; 
        blockedTimeslots[violatingTimeslot+1] = true; 

        double plannedCapacity1 = scheduler.getPlannedCapacity(chargingStation1, car1, car1.timestampArrival.toSecondOfDay()); 
        double[] firstPlan = car1.getCurrentPlan().clone();
        
        strategy.rescheduleCar(state, carAssignment1, blockedTimeslots, violatingTimeslot, fuseTreeException); 
        
        double plannedCapacity2 = scheduler.getPlannedCapacity(chargingStation1, car1, car1.timestampArrival.toSecondOfDay()); 
        double[] secondPlan = car1.getCurrentPlan().clone();

        // Plan should be changed
        assertFalse(Arrays.equals(firstPlan, secondPlan));
        // plannedCapacity should be the same
        assertEquals(plannedCapacity1, plannedCapacity2, 1e-8);
        assertEquals(18, car1.getCurrentPlan()[car1.getFirstAvailableTimeslot()], 1e-8); 
        assertEquals(0, car1.getCurrentPlan()[car1.getFirstAvailableTimeslot()+1], 1e-8); 
        assertEquals(14, car1.getCurrentPlan()[car1.getFirstAvailableTimeslot()+2], 1e-8); 
    }

    
    
    @Test
    public void testHandleSingleViolation() {
    	state.currentTimeSeconds = Math.max(car1.timestampArrival.toSecondOfDay(), car2.timestampArrival.toSecondOfDay()); 
    	state.currentTimeslot = TimeUtil.getTimeslotFromSeconds(state.currentTimeSeconds); 
    	state.addCarAssignment(car1, chargingStation1); 
    	state.addCarAssignment(car2, chargingStation2);
    	
        // Create violation at timeslot=1
    	int violatingK = Math.max(car1.getFirstAvailableTimeslot(), car2.getFirstAvailableTimeslot()); 
    	
        car1.getCurrentPlan()[violatingK] = 32;
        car2.getCurrentPlan()[violatingK] = 32;
        car2.setCurrentCapacity(car2.minLoadingState + 1); // Add this so that car 2 has less priority

        HashMap<Integer, FuseTreeException> violatingTimeslots = new HashMap<>();
        violatingTimeslots.put(violatingK, new FuseTreeException((Fuse) fuseTree.getRootFuse().getChildren().get(0).getChildren().get(0),
                new double[]{64, 64, 64}, violatingK));

        boolean[] blockedTimeslots = new boolean[96];
        blockedTimeslots[violatingK] = true; // block first timeslot
        blockedTimeslots[violatingK+1] = true;
        
        strategy.handleViolation(state, violatingK, blockedTimeslots, violatingTimeslots);

        // Check that car1 keeps original plan and that car2 is rescheduled to timeslot=2
        assertEquals(32, car1.getCurrentPlan()[violatingK], 1e-8);
        assertEquals(0, car1.getCurrentPlan()[violatingK+1], 1e-8);

        assertEquals(18, car2.getCurrentPlan()[violatingK], 1e-8); // Lowest fuse has 50A, 32-(64-50)=18
        assertEquals(0, car2.getCurrentPlan()[violatingK+1], 1e-8);
        assertEquals(14, car2.getCurrentPlan()[violatingK+2], 1e-8);
        

    }

    @Test
    public void testHandleTwoViolationsSameTimeslot() {
    	int violatingTimeslot = Arrays.stream(new int[] {car1.getFirstAvailableTimeslot(), car2.getFirstAvailableTimeslot(), car3.getFirstAvailableTimeslot(), car4.getFirstAvailableTimeslot()})
        	  .max().getAsInt();
        
        car1.getCurrentPlan()[violatingTimeslot] = 32;
        car2.getCurrentPlan()[violatingTimeslot] = 32;
        car1.setCurrentCapacity(car1.minLoadingState + 1); // Make car 1 have lower priority than car 2

        car3.getCurrentPlan()[violatingTimeslot] = 32;
        car4.getCurrentPlan()[violatingTimeslot] = 32;
        car4.setCurrentCapacity(car4.minLoadingState + 1); // Make car 4 have lower priority than car 3

        boolean[] blockedTimeslots = new boolean[96];
        blockedTimeslots[violatingTimeslot] = true;
        blockedTimeslots[violatingTimeslot+1] = true;
        
        state.currentTimeSeconds = TimeUtil.getSecondsFromTimeslot(violatingTimeslot);
        state.currentTimeslot = violatingTimeslot; 
        state.addCarAssignment(car1, chargingStation1); 
        state.addCarAssignment(car2, chargingStation2);
        state.addCarAssignment(car3, chargingStation3);
        state.addCarAssignment(car4, chargingStation4);
        
        //Map<Integer, FuseTreeException> violatingTimeslots = strategy.getInitialFuseViolations(state, null); 
        //assertEquals(1, violatingTimeslots.size()); // Can only store 1 violation for this timeslot
        
        // Add exception for car 1 and 2 manually
        Map<Integer, FuseTreeException> violatingTimeslots = new HashMap<>();
        FuseTreeNode fuseWithCars1And2 = fuseTree.getRootFuse().getChildren().get(0).getChildren().get(0); 
        violatingTimeslots.put(violatingTimeslot, new FuseTreeException((Fuse) fuseWithCars1And2, new double[]{64, 64, 64}, violatingTimeslot));
        

        // Car 3 and 4 should cause another exception but this should be fixed within handleViolation
        strategy.handleViolation(state, violatingTimeslot, blockedTimeslots, violatingTimeslots);

        // Car 1 should be rescheduled: 18 is left over for timeslot=1, 14 for timeslot 2
        assertEquals(18, car1.getCurrentPlan()[violatingTimeslot], 1e-8);  // 32 - (64-50) = 18
        assertEquals(0, car1.getCurrentPlan()[violatingTimeslot+1], 1e-8); // blocked
        assertEquals(14, car1.getCurrentPlan()[violatingTimeslot+2], 1e-8);

        // Car 2 should be unchanged since it has higher priority
        assertEquals(32, car2.getCurrentPlan()[violatingTimeslot], 1e-8);
        assertEquals(0, car2.getCurrentPlan()[violatingTimeslot+1], 1e-8);
        assertEquals(0, car2.getCurrentPlan()[violatingTimeslot+2], 1e-8);

        // Only a single violation is handled! Car 3 and car 4 should be unchanged
        assertEquals(32, car3.getCurrentPlan()[violatingTimeslot], 1e-8);
        assertEquals(0, car3.getCurrentPlan()[violatingTimeslot+1], 1e-8);
        assertEquals(0, car3.getCurrentPlan()[violatingTimeslot+2], 1e-8);
        assertEquals(32, car4.getCurrentPlan()[violatingTimeslot], 1e-8);  
        assertEquals(0, car4.getCurrentPlan()[violatingTimeslot+1], 1e-8);
        assertEquals(0, car4.getCurrentPlan()[violatingTimeslot+2], 1e-8);
        assertEquals(1, violatingTimeslots.size()); // The new exception should be in the hashmap
        
    }

    @Test
    public void testHandleSingleViolationNoPower() {
        int violatingTimeslot = Math.max(car1.getFirstAvailableTimeslot(), car2.getFirstAvailableTimeslot()); 

        state.currentTimeSeconds = Math.max(car1.timestampArrival.toSecondOfDay(), car2.timestampArrival.toSecondOfDay()); 
        state.currentTimeslot = TimeUtil.getTimeslotFromSeconds(state.currentTimeSeconds); 
        state.addCarAssignment(car1, chargingStation1); 
        state.addCarAssignment(car2, chargingStation2); 
        
        boolean[] blockedTimeslots = new boolean[96];
        blockedTimeslots[violatingTimeslot] = true;

        car1.getCurrentPlan()[violatingTimeslot] = 32;
        car2.getCurrentPlan()[violatingTimeslot+1] = 0;
        car2.setCurrentCapacity(car1.minLoadingState + 1); // Make car 1 have higher priority

        Map<Integer, FuseTreeException> violatingTimeslots = new HashMap<>();
        violatingTimeslots.put(violatingTimeslot, new FuseTreeException((Fuse) fuseTree.getRootFuse().getChildren().get(0).getChildren().get(0),
                new double[]{64, 64, 64}, violatingTimeslot));
        
        strategy.handleViolation(state, violatingTimeslot, blockedTimeslots, violatingTimeslots);

        // Car 1 should be rescheduled even though it has lower priority since car 2 has nothing to reschedule
        assertEquals(18, car1.getCurrentPlan()[violatingTimeslot], 1e-8);
        assertEquals(14, car1.getCurrentPlan()[violatingTimeslot+1], 1e-8);

    }

    @Test
    public void testHandleViolation_UpdateFuseTreeException() {
    	strategy.setRescheduleCarsWith0A(false);
    	
    	int violatingTimeslot = Math.max(car1.getFirstAvailableTimeslot(), car2.getFirstAvailableTimeslot()); 
    	boolean[] blockedTimeslots = new boolean[96]; 
    	blockedTimeslots[violatingTimeslot] = true; 
    	
    	state.currentTimeslot = violatingTimeslot;
    	state.currentTimeSeconds = Math.max(car1.timestampArrival.toSecondOfDay(), car2.timestampArrival.toSecondOfDay()); 
    	state.addCarAssignment(car1, chargingStation1);
    	state.addCarAssignment(car2, chargingStation2); 
    	
    	
    	car1.getCurrentPlan()[violatingTimeslot] = 32;
        car1.setCurrentCapacity(car1.minLoadingState + 1); // Make car 2 have higher priority
        car2.getCurrentPlan()[violatingTimeslot] = 32;

        // Exception: 50A over the top -> reduce by 32A and 18A 
        // FuseTreeException must be updated internally for this
        double deltaA = 50; 
        
        Map<Integer, FuseTreeException> violatingTimeslots = new HashMap<>(); 
        Fuse parent = (Fuse) chargingStation1.getParent(); 
        parent.fusePhase1 = 14; 
        parent.fusePhase2 = 14; 
        parent.fusePhase3 = 14; 
        
        double fuseTreeExceptionValuePerPhase = parent.getFusePhase(Phase.PHASE_1) + deltaA; 
        
        violatingTimeslots.put(violatingTimeslot, new FuseTreeException(chargingStation1.getParent(), 
        		new double[] {fuseTreeExceptionValuePerPhase, fuseTreeExceptionValuePerPhase, fuseTreeExceptionValuePerPhase}, violatingTimeslot)); 
        
        strategy.handleViolation(state, violatingTimeslot, blockedTimeslots, violatingTimeslots);
        
        
        assertEquals(14, car2.getCurrentPlan()[violatingTimeslot], 1e-8); 
        assertEquals(0, car1.getCurrentPlan()[violatingTimeslot], 1e-8); 
    }
    
    
    
    @Test
    public void testGetInitialFuseViolations() {
    	int violatingTimeslot = Math.max(car1.getFirstAvailableTimeslot(), car2.getFirstAvailableTimeslot()); 
    	
    	state.currentTimeSeconds = Math.max(car1.timestampArrival.toSecondOfDay(), car2.timestampArrival.toSecondOfDay()); 
    	state.currentTimeslot = violatingTimeslot; 
    	state.addCarAssignment(car1, chargingStation1); 
    	state.addCarAssignment(car2, chargingStation2); 
    	
        car1.getCurrentPlan()[violatingTimeslot] = 32;
        car1.setCurrentCapacity(car1.minLoadingState + 1);
        car2.getCurrentPlan()[violatingTimeslot] = 32;

        Map<Integer, FuseTreeException> violatingTimeslots = strategy.getInitialFuseViolations(state, car1);

        assertEquals(1, violatingTimeslots.size());
        assertEquals(violatingTimeslot, violatingTimeslots.get(violatingTimeslot).timeslot);

    }

    @Test
    public void testResolveViolations() {
    	strategy.setRescheduleCarsWith0A(false);
    	
    	int violatingTimeslot = Math.max(car1.getFirstAvailableTimeslot(), car2.getFirstAvailableTimeslot()); 
    	
    	state.currentTimeslot = violatingTimeslot;
    	state.currentTimeSeconds = Math.max(car1.timestampArrival.toSecondOfDay(), car2.timestampArrival.toSecondOfDay()); 
    	state.addCarAssignment(car1, chargingStation1);
    	state.addCarAssignment(car2, chargingStation2); 
    	
    	
        car1.getCurrentPlan()[violatingTimeslot] = 32;
        car1.setCurrentCapacity(car1.minLoadingState + 1); // Make car 2 have higher priority
        car2.getCurrentPlan()[violatingTimeslot] = 32;

        Map<Integer, FuseTreeException> violatingTimeslots = strategy.getInitialFuseViolations(state, null);
        strategy.resolveViolations(state, violatingTimeslots); 
        
        // Car 1 should be rescheduled (divided between timeslot=1 (the original timeslot) and timeslot=0)
        assertEquals(18, car1.getCurrentPlan()[violatingTimeslot], 1e-8); // 50 (fuse value) - 32 (other car plan value) = 18, we were 14 over the top
        assertEquals(14, car1.getCurrentPlan()[violatingTimeslot+1], 1e-8); // 14A should be rescheduled to somewhere else

        // Car 2 should be unchanged since it has higher priority
        assertEquals(32, car2.getCurrentPlan()[violatingTimeslot], 1e-8);
        assertEquals(0, car2.getCurrentPlan()[violatingTimeslot+1], 1e-8);

        // Violations should be gone
        assertEquals(0, violatingTimeslots.size());

    }
    
    /******************************
     * React to events
     ******************************/

    @Test
    public void testReactArrival() {
    	
    	state.currentTimeSeconds = car1.timestampArrival.toSecondOfDay(); 
    	state.currentTimeslot = TimeUtil.getTimeslotFromSeconds(state.currentTimeSeconds); 
        strategy.reactCarArrival(state, car1);

        assertEquals(32, car1.getCurrentPlan()[car1.getFirstAvailableTimeslot()], 1e-8);
        assertTrue(scheduler.getPlannedCapacity(chargingStation1, car1, car1.timestampArrival.toSecondOfDay()) >= car1.getMaxCapacity());
        assertEquals(car1.getMaxCapacity(), scheduler.getPlannedCapacity(chargingStation1, car1, car1.timestampArrival.toSecondOfDay()), 1);
        
    }

    @Test
    public void testReactOptimize() {

    	// 2 cars, one arrived in the morning and one arrived now
    	int currentTimeSeconds = 16 * 3600; 
    	int currentTimeslot = TimeUtil.getTimeslotFromSeconds(currentTimeSeconds); 
    	car1.timestampArrival = TimeUtil.getTimestampFromSeconds(8*3600); 
    	car1.setCurrentCapacity(car1.getMaxCapacity()*0.9);
    	car2.timestampArrival = TimeUtil.getTimestampFromSeconds(currentTimeSeconds); 
    	car2.setCurrentCapacity(car2.getMaxCapacity()*0.7);
    	
    	state.currentTimeSeconds = currentTimeSeconds; 
    	state.currentTimeslot = TimeUtil.getTimeslotFromSeconds(state.currentTimeSeconds); 
        
    	
    	state.addCarAssignment(car1, chargingStation1); 
    	state.addCarAssignment(car2, chargingStation2); 
    	
    	Fuse fuseRoot = new Fuse(0, 32);
    	fuseRoot.addChild(chargingStation1);
    	fuseRoot.addChild(chargingStation2);
    	
        fuseTree = new FuseTree(fuseRoot, 999);
        state.fuseTree = fuseTree; 
    	
    	strategy.reactReoptimize(state);
    	
    	// Car1 should have lower priority and be rescheduled
    	assertEquals(0, car1.getCurrentPlan()[currentTimeslot], 0); 
    	assertEquals(32, car2.getCurrentPlan()[currentTimeslot], 0); 
    	
    	// Plan should still full both cars
    	double desiredCapacityCar2 = car2.getMissingCapacity(); 
    	assertEquals(desiredCapacityCar2, scheduler.getPlannedCapacity(chargingStation2, car2, currentTimeSeconds), 1); 
    	
    	double desiredCapacityCar1 = car1.getMissingCapacity(); 
    	assertEquals(desiredCapacityCar1, scheduler.getPlannedCapacity(chargingStation1, car1, currentTimeSeconds), 1e-8); 
    }
    
    

    @Test
    public void testReactCarFinished() {
        // Placeholder
        strategy.reactCarFinished(state, car1);
    }

    
    @Test
    public void testReactCarDeparture() {
        strategy.reactCarDeparture(state, car1);
        strategy.reactCarDeparture(state, car2);
    }

    @Test
    public void testReactExternalTrigger() {
    	strategy.reactEnergyPriceChange(state, state.energyPriceHistory);
    }
    
    
    

    @Test
    public void reactExternalTriggerWrapper_givenADecreaseOfTheChargingStationFuseValues_decreasesValuesInChargePlan() {


        state.currentTimeSeconds = car1.timestampArrival.toSecondOfDay(); 
        state.currentTimeslot = TimeUtil.getTimeslotFromSeconds(state.currentTimeSeconds); 
        
        strategy.reactCarArrivalWrapper(state, car1);

        //System.out.println(Arrays.toString(car1.getCurrentPlan()));
        assertEquals(32, car1.getCurrentPlan()[car1.getFirstAvailableTimeslot()], 1e-8);

        ChargingStation station = state.getCurrentCarAssignment(car1).chargingStation; 
        station.fusePhase1 = 16; 
        station.fusePhase2 = 16; 
        station.fusePhase3 = 16; 

        strategy.reactReoptimize(state);

        double[] currentPlan = car1.getCurrentPlan();
        for (int i = 0; i < currentPlan.length; i++) {
            assertThat("Expected value<=16.0 but was " + currentPlan[i], currentPlan[i] <= 16.0, is(true));
        }
    }
}













