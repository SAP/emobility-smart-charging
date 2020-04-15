package com.sap.charging.sim.eval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.common.DefaultDataGenerator;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.model.Fuse;
import com.sap.charging.model.FuseTree;
import com.sap.charging.model.FuseTreeNode;
import com.sap.charging.realTime.State;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.model.CarAssignment;
import com.sap.charging.realTime.model.PowerAssignment;
import com.sap.charging.sim.Simulation;
import com.sap.charging.sim.common.SimulationUnitTest;
import com.sap.charging.sim.eval.exception.CarAssignmentException;
import com.sap.charging.sim.eval.exception.FuseTreeException;
import com.sap.charging.sim.eval.exception.PowerAssignmentException;
import com.sap.charging.sim.eval.exception.ValidationException;
import com.sap.charging.util.TimeUtil;


public class ValidationTest extends SimulationUnitTest {

    @Test
    public void testDummy() {
        new Validation();
    }

    State state; 

    @BeforeEach
    public void setup() {
    	Simulation.verbosity = 0; 
    	sim = new Simulation(dataSim, new StrategyAlgorithmic()); 
    	sim.init(); 
    	state = sim.getState(); 
        for (Car car : dataSim.getCars()) {
            car.setCurrentPlan(new double[dataSim.getEnergyPriceHistory().getNTimeslots()]);
        }
    }

    /***************************************
     * Car assignment tests
     ***************************************/

    @Test
    public void testValidCarAssignments() throws ValidationException {
        List<CarAssignment> carAssignments = new ArrayList<>();
        
        for (int i = 0; i < dataSim.getChargingStations().size(); i++) {
            if (i < dataSim.getCars().size()) {
                carAssignments.add(new CarAssignment(dataSim.getCars().get(i), dataSim.getChargingStations().get(i)));
            }
        }
        
        Validation.validateCarAssignments(carAssignments);
    }

    @Test
    public void testInvalidCarAssignments_AssignChargingStationTwice() {
        List<CarAssignment> carAssignments = new ArrayList<>();
        carAssignments.add(new CarAssignment(dataSim.getCars().get(0), dataSim.getChargingStations().get(0)));
        carAssignments.add(new CarAssignment(dataSim.getCars().get(1), dataSim.getChargingStations().get(0)));

        try {
            Validation.validateCarAssignments(carAssignments);
            fail("Assigning a charging station twice should have failed.");
        } catch (CarAssignmentException e) {
            assertTrue(e.getMessage().contains("i=0 is already assigned."));
        }
    }

    @Test
    public void testInvalidCarAssignments_AssignCarTwice() {
        List<CarAssignment> carAssignments = new ArrayList<>();
        carAssignments.add(new CarAssignment(dataSim.getCars().get(0), dataSim.getChargingStations().get(0)));
        carAssignments.add(new CarAssignment(dataSim.getCars().get(0), dataSim.getChargingStations().get(1)));

        try {
            Validation.validateCarAssignments(carAssignments);
            fail("Assigning a car twice should have failed.");
        } catch (CarAssignmentException e) {
            assertTrue(e.getMessage().contains("n=0 is already assigned."));
        }
    }

    /***************************************
     * Power assignment tests
     ***************************************/

    @Test
    public void testValidPowerAssignments() throws PowerAssignmentException {
    	List<PowerAssignment> powerAssignments = new ArrayList<>();
    	Car car1 = dataSim.getCar(0); 
    	Car car2 = dataSim.getCar(1); 
    	ChargingStation chargingStation1 = dataSim.getChargingStation(0); 
    	ChargingStation chargingStation2 = dataSim.getChargingStation(1); 
    	powerAssignments.add(new PowerAssignment(chargingStation1, car1, 10.0, 10.0*car1.canLoadPhase2, 10.0*car1.canLoadPhase3));
    	powerAssignments.add(new PowerAssignment(chargingStation2, car2, 10.0, 10.0*car2.canLoadPhase2, 10.0*car2.canLoadPhase3));

    	Validation.validateCarPowerAssignments(powerAssignments);
    }

    @Test
    public void testValidPowerAssignment_ThreePhaseEV_SinglePhaseStation() throws PowerAssignmentException {
    	DataGenerator data = DefaultDataGenerator.getDataGenerator_ThreePhaseEV(); 
    	Car car = data.getCar(0); 
    	
    	assertEquals(3, car.sumUsedPhases, 0); 

    	ChargingStation station = dataSim.getChargingStation(0); 
    	station.fusePhase2 = 0; 
    	station.fusePhase3 = 0; 
    	station.setPhase2Connected(false);
    	station.setPhase3Connected(false);
    	
    	assertEquals(true, station.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_1)); 
    	assertEquals(false, station.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_2)); 
    	assertEquals(false, station.isPhaseAtGridConnectedInFuseTree(Phase.PHASE_3)); 
    	
    	
    	List<PowerAssignment> powerAssignments = new ArrayList<>();
    	powerAssignments.add(new PowerAssignment(station, car, 10.0, 0, 0));
    	
    	Validation.validateCarPowerAssignments(powerAssignments);
    }
    
    @Test
    public void testInvalidPowerAssignments_OverloadFuse() {
    	List<PowerAssignment> powerAssignments = new ArrayList<>();
    	powerAssignments.add(new PowerAssignment(dataSim.getChargingStations().get(0), dataSim.getCars().get(0), 10.0, 10.0, 10.0));
    	
        Car car = dataSim.getCars().get(0); // Uses three phases
        car.getCurrentPlan()[0] = car.minCurrentPerPhase;

        ChargingStation chargingStation = dataSim.getChargingStations().get(0);

        // Fail on phase 1
        chargingStation.fusePhase1 = 0.5;
        chargingStation.fusePhase2 = 100;
        chargingStation.fusePhase3 = 100;

        try {
            Validation.validateCarPowerAssignments(powerAssignments);
            fail("Assigning a plan with more than a charging station's fuse phase 1 should have failed.");
        } catch (PowerAssignmentException e) {
            assertTrue(e.getMessage().contains("chargingStation.fusePhase1=0.5"));
        }

        // Fail on phase 2
        chargingStation.fusePhase1 = 100;
        chargingStation.fusePhase2 = 0.5;
        chargingStation.fusePhase3 = 100;

        try {
            Validation.validateCarPowerAssignments(powerAssignments);
            fail("Assigning a plan with more than a charging station's fuse phase 2 should have failed.");
        } catch (PowerAssignmentException e) {
            assertTrue(e.getMessage().contains("chargingStation.fusePhase2=0.5"));
        }

        // Fail on phase 3
        chargingStation.fusePhase1 = 100;
        chargingStation.fusePhase2 = 100;
        chargingStation.fusePhase3 = 0.5;

        try {
            Validation.validateCarPowerAssignments(powerAssignments);
            fail("Assigning a plan with more than a charging station's fuse phase 3 should have failed.");
        } catch (PowerAssignmentException e) {
            assertTrue(e.getMessage().contains("chargingStation.fusePhase3=0.5"));
        }

    }

    @Test
    public void testInvalidPowerAssignments_BreakCarMaxPower() {
        int index = 0;
        final Car car = dataSim.getCars().get(index);
        ChargingStation chargingStation = dataSim.getChargingStations().get(index);
        chargingStation.fusePhase1 = 100;
        chargingStation.fusePhase2 = 100;
        chargingStation.fusePhase3 = 100; 

        // Phase 1
        List<PowerAssignment> powerAssignments = new ArrayList<>();
    	powerAssignments.add(new PowerAssignment(dataSim.getChargingStations().get(0), dataSim.getCars().get(0), car.maxCurrentPerPhase+1, car.maxCurrentPerPhase+1, car.maxCurrentPerPhase+1));
    	
        try {
            Validation.validateCarPowerAssignments(powerAssignments);
            fail("Assigning a plan with more than a car's  max power should have failed (phase 1).");
        } catch (PowerAssignmentException e) {
            assertTrue(e.getMessage().contains("car=n0 car.maxCurrentPerPhase"));
        }
    }

    @Test
    public void testInvalidPowerAssignments_BreakCarMinPower() {
        int index = 0;
        final Car car = dataSim.getCars().get(index);
        ChargingStation chargingStation = dataSim.getChargingStations().get(index);
        chargingStation.fusePhase1 = 100;
        chargingStation.fusePhase2 = 100;
        chargingStation.fusePhase3 = 100; 

        // Phase 1
        List<PowerAssignment> powerAssignments = new ArrayList<>();
    	powerAssignments.add(new PowerAssignment(chargingStation, car, car.minCurrentPerPhase-1, car.minCurrentPerPhase-1, car.minCurrentPerPhase-1));
    	
    	try {
            Validation.validateCarPowerAssignments(powerAssignments);
            fail("Assigning a plan with less than a car's  min power should have failed (phase 1).");
        } catch (PowerAssignmentException e) {
            assertTrue(e.getMessage().contains("car=n" + index + " car.minCurrentPerPhase"));
        }

    }

    @Test
    public void testInvalidPowerAssignments_BreakCarPowerRatios() {
        int index = 0;
        final Car car = dataSim.getCars().get(index);
        ChargingStation chargingStation = dataSim.getChargingStations().get(index);
        chargingStation.fusePhase1 = 100;
        chargingStation.fusePhase2 = 100;
        chargingStation.fusePhase3 = 100; 

        // Phase 1 to phase 2
        List<PowerAssignment> powerAssignments = new ArrayList<>();
    	powerAssignments.add(new PowerAssignment(chargingStation, car, car.minCurrentPerPhase, car.minCurrentPerPhase, 0));
    	
        try {
            Validation.validateCarPowerAssignments(powerAssignments);
            fail("Assigning a plan with wrong phase ratios should have failed (phase 1 to phase 2).");
        } catch (PowerAssignmentException e) {
            assertTrue(e.getMessage().contains("R6/R7 (phase ratios) has been broken."));
        }

        // Phase 1 to phase 3
        powerAssignments = new ArrayList<>();
    	powerAssignments.add(new PowerAssignment(chargingStation, car, car.minCurrentPerPhase, 0, car.minCurrentPerPhase));
    	try {
            Validation.validateCarPowerAssignments(powerAssignments);
            fail("Assigning a plan with wrong phase ratios should have failed (phase 1 to phase 3).");
        } catch (PowerAssignmentException e) {
            assertTrue(e.getMessage().contains("R6/R7 (phase ratios) has been broken."));
        }

    }
    
    
    @Test
    public void testInvalidPowerAssignments_NotConnected() {
    	int index = 0;
        final Car car = dataSim.getCars().get(index);
        ChargingStation chargingStation = dataSim.getChargingStations().get(index);
        
        List<PowerAssignment> powerAssignments = new ArrayList<>();
    	powerAssignments.add(new PowerAssignment(chargingStation, car, car.maxCurrentPerPhase, car.maxCurrentPerPhase, car.maxCurrentPerPhase));
    	
    	// Power assignment invalid because charging station is not connected on phase 1
        chargingStation.fusePhase1 = 0;
        chargingStation.fusePhase2 = 32; 
        chargingStation.fusePhase3 = 32;  
        chargingStation.setPhase1Connected(false);
        chargingStation.setPhase2Connected(true);
        chargingStation.setPhase3Connected(true);
    	
        try {
            Validation.validateCarPowerAssignments(powerAssignments);
            fail("Invalid PowerAssignment with unconnected charging stations should have failed");
        } catch (PowerAssignmentException e) {
            assertTrue(e.getMessage().contains("R3 (don't overload fuses) has been broken"));
        }
        

    }
    

    /***************************************
     * Fuse tree tests
     * @throws FuseTreeException
     ***************************************/

    @Test
    public void testFuseUsage() throws FuseTreeException {
        Fuse fuse1 = new Fuse(0, 100);
        Fuse fuse2 = new Fuse(1, 100);

        double[] valid = new double[]{50, 50, 50};
        double[] invalidP1 = new double[]{200, 50, 50};
        double[] invalidP2 = new double[]{50, 200, 50};
        double[] invalidP3 = new double[]{50, 50, 200};

        // Valid
        assertTrue(Validation.isFuseUsageValid(fuse1, valid));

        // Invalid
        assertTrue(Validation.isFuseUsageValid(fuse1, invalidP1) == false);
        assertTrue(Validation.isFuseUsageValid(fuse1, invalidP2) == false);
        assertTrue(Validation.isFuseUsageValid(fuse1, invalidP3) == false);

        HashMap<FuseTreeNode, double[]> fuseConsumptionMap = new HashMap<>();

        // Valid
        fuseConsumptionMap.put(fuse1, valid);
        fuseConsumptionMap.put(fuse2, null);
        Validation.validateFuseUsage(fuseConsumptionMap, 0);

        // Invalid
        fuseConsumptionMap.clear();
        fuseConsumptionMap.put(fuse1, invalidP1);
        try {
            Validation.validateFuseUsage(fuseConsumptionMap, 0);
            fail("Invalid fuse usage should have failed.");
        } catch (FuseTreeException e) {
            assertTrue(e.getMessage().contains("Fuse index=0 broken"));
        }
    }
    

    @Test
    public void testParentNodeConsumption() {
        Fuse fuseRoot = new Fuse(0, 100);
        Fuse fuseParent = new Fuse(1, 100);
        fuseRoot.addChild(fuseParent);
        Fuse fuseChild = new Fuse(2, 100);
        fuseParent.addChild(fuseChild);

        double[] consumption = new double[]{20, 20, 20};

        HashMap<FuseTreeNode, double[]> fuseConsumptionMap = new HashMap<>();
        fuseConsumptionMap.put(fuseRoot,
                new double[]{50, 50, 50}); // Only fuseRoot has had a consumption array so far. fuseParent has not

        Validation.updateFuseParentsConsumption(fuseChild, fuseConsumptionMap, null);
        Validation.updateFuseParentsConsumption(fuseChild, fuseConsumptionMap, consumption);

        assertEquals(70, fuseConsumptionMap.get(fuseRoot)[0], 1e-8);
        assertEquals(20, fuseConsumptionMap.get(fuseParent)[0], 1e-8);
    }

    @Test
    public void testCurrentPerGridPhase() {
        // With assignment
    	Car car = dataSim.getCar(0); 
    	ChargingStation station = dataSim.getChargingStation(0); 
    	state.currentTimeSeconds = car.timestampArrival.toSecondOfDay();
    	state.addCarAssignment(car, station); 
        double[] result1 = Validation.getCurrentPerGridPhase(station, state, 0);
        assertNotNull(result1);

        // Without assignment
        double[] result2 = Validation.getCurrentPerGridPhase(new ChargingStation(), state, 0);
        assertNull(result2);

    }

    @Test
    public void testCheckSummedChildConsumption() throws FuseTreeException {
    	Car car = dataSim.getCar(0); 
    	ChargingStation chargingStation = dataSim.getChargingStation(0); 
    	state.currentTimeSeconds = car.timestampArrival.toSecondOfDay();
    	state.currentTimeslot = TimeUtil.getTimeslotFromSeconds(state.currentTimeSeconds); 
    	state.addCarAssignment(car, chargingStation); 
    	state.addPowerAssignment(car, chargingStation, car.maxCurrentPerPhase, car.maxCurrentPerPhase, car.maxCurrentPerPhase); 
        
    	car.getCurrentPlan()[state.currentTimeslot] = 200; 
    	
    	Fuse fuseRoot = new Fuse(0, 100);
        Fuse fuseParent = new Fuse(1, 100);
        fuseRoot.addChild(fuseParent);
        Fuse fuseChild = new Fuse(2, 100);
        fuseChild.addChild(chargingStation);
        fuseParent.addChild(fuseChild);

        FuseTree fuseTree = new FuseTree(fuseRoot, 100);
        state.fuseTree = fuseTree; 
        
        // Check for root (before car arrival)
        Validation.checkSummedChildConsumptionAtTimeslot(fuseRoot, state, 0);
       
        // Check for non root (before car arrival)
        Validation.checkSummedChildConsumptionAtTimeslot(fuseParent, state, 0);

        chargingStation.fusePhase1 = 200; 
        chargingStation.fusePhase2 = 200; 
        chargingStation.fusePhase3 = 200; 
        
        state.getCurrentPowerAssignment(car).setPhase1(200); 
        //Simulation.verbosity = 3; 
        // Invalid root (at car arrival)
        try {
            Validation.checkSummedChildConsumptionAtTimeslot(fuseRoot, state, state.currentTimeslot);
            fail("Invalid fuse usage (root) at current timeslot (=use car plan) should have failed.");
        } catch (FuseTreeException e) {
        	assertEquals(200, e.getSumConsumedByPhase(Phase.PHASE_1), 1e-8); 
        }
        try {
            Validation.checkSummedChildConsumptionAtTimeslot(fuseRoot, state, -1);
            fail("Invalid fuse usage (root) at timeslot -1 (=use power assignment) should have failed.");
        } catch (FuseTreeException e) {
        	assertEquals(200, e.getSumConsumedByPhase(Phase.PHASE_1), 1e-8); 
        }
        
        
        // Invalid non root
        try {
            Validation.checkSummedChildConsumptionAtTimeslot(fuseParent, state, state.currentTimeslot);
            fail("Invalid fuse usage (non root) at current timeslot (=use car plan) should have failed.");
        } catch (FuseTreeException e) {
        	assertEquals(200, e.getSumConsumedByPhase(Phase.PHASE_1), 1e-8); 
        }
        try {
            Validation.checkSummedChildConsumptionAtTimeslot(fuseParent, state, -1);
            fail("Invalid fuse usage (non root) at timeslot -1 (=use power assignment) should have failed.");
        } catch (FuseTreeException e) {
        	assertEquals(200, e.getSumConsumedByPhase(Phase.PHASE_1), 1e-8); 
        }

    }
    
    @Test
    public void testCheckSummedChildConsumption_ThreePhaseEV_SinglePhaseStation() throws FuseTreeException {

    	ChargingStation chargingStation = dataSim.getChargingStation(0); 
    	chargingStation.fusePhase2 = 0; 
    	chargingStation.fusePhase3 = 0; 
    	chargingStation.setPhase2Connected(false);
    	chargingStation.setPhase3Connected(false);
    	
    	state.currentTimeSeconds = threePhaseCar.timestampArrival.toSecondOfDay();
    	state.currentTimeslot = TimeUtil.getTimeslotFromSeconds(state.currentTimeSeconds); 
    	state.addCarAssignment(threePhaseCar, chargingStation); 
    	state.addPowerAssignment(threePhaseCar, chargingStation, 
    			threePhaseCar.maxCurrentPerPhase, 0, 0); 
        
    	threePhaseCar.getCurrentPlan()[state.currentTimeslot] = 200; 
    	
    	Fuse fuseRoot = new Fuse(0, 100);
        Fuse fuseParent = new Fuse(1, 100);
        fuseRoot.addChild(fuseParent);
        Fuse fuseChild = new Fuse(2, 100);
        fuseChild.addChild(chargingStation);
        fuseParent.addChild(fuseChild);

        FuseTree fuseTree = new FuseTree(fuseRoot, 100);
        state.fuseTree = fuseTree; 
        
        // Check for root
        Validation.checkSummedChildConsumptionAtTimeslot(fuseRoot, state, 0);

        // Check for non root
        Validation.checkSummedChildConsumptionAtTimeslot(fuseParent, state, 0);

        chargingStation.fusePhase1 = 200; 
        
        state.getCurrentPowerAssignment(threePhaseCar).setPhase1(200); 
        //Simulation.verbosity = 3; 
        // Invalid root
        try {
            Validation.checkSummedChildConsumptionAtTimeslot(fuseRoot, state, state.currentTimeslot);
            fail("Invalid fuse usage (root) at current timeslot (=use car plan) should have failed.");
        } catch (FuseTreeException e) {
        	assertEquals(200, e.getSumConsumedByPhase(Phase.PHASE_1), 1e-8); 
        }
        try {
            Validation.checkSummedChildConsumptionAtTimeslot(fuseRoot, state, -1);
            fail("Invalid fuse usage (root) at timeslot -1 (=use power assignment) should have failed.");
        } catch (FuseTreeException e) {
        	assertEquals(200, e.getSumConsumedByPhase(Phase.PHASE_1), 1e-8); 
        }
        
        // Invalid non root
        try {
            Validation.checkSummedChildConsumptionAtTimeslot(fuseParent, state, state.currentTimeslot);
            fail("Invalid fuse usage (non root) at current timeslot (=use car plan) should have failed.");
        } catch (FuseTreeException e) {
        	assertEquals(200, e.getSumConsumedByPhase(Phase.PHASE_1), 1e-8); 
        }
        try {
            Validation.checkSummedChildConsumptionAtTimeslot(fuseParent, state, -1);
            fail("Invalid fuse usage (non root) at timeslot -1 (=use power assignment) should have failed.");
        } catch (FuseTreeException e) {
        	assertEquals(200, e.getSumConsumedByPhase(Phase.PHASE_1), 1e-8); 
        }

    }
    
    
    
    @Test
    public void testCheckSummedChildConsumption_ThreePhaseEV_SinglePhaseStation_WithPhaseRotation() throws FuseTreeException {

    	ChargingStation chargingStation = dataSim.getChargingStation(0); 
    	chargingStation.fusePhase2 = 0; 
    	chargingStation.fusePhase3 = 0; 
    	chargingStation.setPhase2Connected(false);
    	chargingStation.setPhase3Connected(false);
    	
    	state.currentTimeSeconds = threePhaseCar.timestampArrival.toSecondOfDay();
    	state.currentTimeslot = TimeUtil.getTimeslotFromSeconds(state.currentTimeSeconds); 
    	state.addCarAssignment(threePhaseCar, chargingStation); 
    	state.addPowerAssignment(threePhaseCar, chargingStation, 
    			threePhaseCar.maxCurrentPerPhase, 0, 0); 
        
    	threePhaseCar.getCurrentPlan()[state.currentTimeslot] = 200; 
    	
    	Fuse fuseRoot = new Fuse(0, 100);
        Fuse fuseParent = new Fuse(1, 100);
        fuseRoot.addChild(fuseParent);
        Fuse fuseChild = new Fuse(2, 100);
        fuseChild.addChild(chargingStation);
        fuseParent.addChild(fuseChild);

        FuseTree fuseTree = new FuseTree(fuseRoot, 100);
        state.fuseTree = fuseTree; 
        
        // Check for root
        Validation.checkSummedChildConsumptionAtTimeslot(fuseRoot, state, 0);

        // Check for non root
        Validation.checkSummedChildConsumptionAtTimeslot(fuseParent, state, 0);

        chargingStation.fusePhase1 = 200; 
        
        state.getCurrentPowerAssignment(threePhaseCar).setPhase1(200); 
        
        chargingStation.setPhaseMatching(Phase.PHASE_2, Phase.PHASE_3, Phase.PHASE_1);
        // Invalid root
        try {
            Validation.checkSummedChildConsumptionAtTimeslot(fuseRoot, state, state.currentTimeslot);
            fail("Invalid fuse usage (root) at current timeslot (=use car plan) should have failed.");
        } catch (FuseTreeException e) {
        	assertEquals(200, e.getSumConsumedByPhase(Phase.PHASE_2), 1e-8); 
        }
        try {
            Validation.checkSummedChildConsumptionAtTimeslot(fuseRoot, state, -1);
            fail("Invalid fuse usage (root) at timeslot -1 (=use power assignment) should have failed.");
        } catch (FuseTreeException e) {
        	assertEquals(200, e.getSumConsumedByPhase(Phase.PHASE_2), 1e-8); 
        }
        
        chargingStation.setPhaseMatching(Phase.PHASE_3, Phase.PHASE_1, Phase.PHASE_2);
        try {
            Validation.checkSummedChildConsumptionAtTimeslot(fuseRoot, state, state.currentTimeslot);
            fail("Invalid fuse usage (root) at current timeslot (=use car plan) should have failed.");
        } catch (FuseTreeException e) {
        	assertEquals(200, e.getSumConsumedByPhase(Phase.PHASE_3), 1e-8); 
        }
        try {
            Validation.checkSummedChildConsumptionAtTimeslot(fuseRoot, state, -1);
            fail("Invalid fuse usage (root) at timeslot -1 (=use power assignment) should have failed.");
        } catch (FuseTreeException e) {
        	assertEquals(200, e.getSumConsumedByPhase(Phase.PHASE_3), 1e-8); 
        }
        

    }
    

    @Test
    public void testValidateFuseAtTimeslot_valid() throws FuseTreeException {
        Car car = dataSim.getCar(0); 
        ChargingStation chargingStation = dataSim.getChargingStation(0); 
        
        state.currentTimeSeconds = car.timestampArrival.toSecondOfDay();
        state.currentTimeslot = TimeUtil.getTimeslotFromSeconds(state.currentTimeSeconds); 
    	state.addCarAssignment(car, chargingStation); 
    	
    	double currentPerPhase = Math.min(car.maxCurrentPerPhase, chargingStation.fusePhase1); 
    	state.addPowerAssignment(car, chargingStation, currentPerPhase, currentPerPhase, currentPerPhase); 
        
        Fuse fuseRoot = new Fuse(0, 100);
        Fuse fuseParent = new Fuse(1, 100);
        fuseRoot.addChild(fuseParent);
        Fuse fuseChild = new Fuse(2, 100);
        fuseChild.addChild(chargingStation);
        fuseParent.addChild(fuseChild);

        FuseTree fuseTree = new FuseTree(fuseRoot, 100);
        state.fuseTree = fuseTree; 

        // VALID
        // Validates at a given timeslot
        Validation.validateFuseAtTimeslot(fuseRoot, state, state.currentTimeslot);
        Validation.validateFuseTreeAtTimeslot(state, state.currentTimeslot);
        assertTrue(Validation.isFuseValidAtTimeslot(fuseRoot, state, state.currentTimeslot));

        // Validates at current timeslot
        Validation.validateFuse(fuseRoot, state);
        Validation.validateFuseTree(state);
        assertTrue(Validation.isFuseTreeValid(state));

    }

    @Test
    public void testValidateFuseAtTimeslot_invalid() throws FuseTreeException {
    	Car car = dataSim.getCars().get(0);
        ChargingStation chargingStation = dataSim.getChargingStations().get(0);
        
        state.currentTimeSeconds = car.timestampArrival.toSecondOfDay();
        state.currentTimeslot = TimeUtil.getTimeslotFromSeconds(state.currentTimeSeconds); 
    	state.addCarAssignment(car, chargingStation); 
    	state.addPowerAssignment(car, chargingStation, 200, 200, 200); 
        
        car.setCurrentPlan(new double[96]);
        car.getCurrentPlan()[state.currentTimeslot+1] = 0; // valid 
        car.getCurrentPlan()[state.currentTimeslot+2] = 200; 
        
        
        Fuse fuseRoot = new Fuse(0, 100);
        Fuse fuseParent = new Fuse(1, 100);
        fuseRoot.addChild(fuseParent);
        Fuse fuseChild = new Fuse(2, 100);
        fuseChild.addChild(chargingStation);
        fuseParent.addChild(fuseChild);

        FuseTree fuseTree = new FuseTree(fuseRoot, 100);
        state.fuseTree = fuseTree; 
        
        
        assertFalse(Validation.isFuseTreeValid(state)); // INVALID (current timeslot) 
        assertTrue(Validation.isFuseValidAtTimeslot(fuseRoot, state, state.currentTimeslot+1)); // Valid (timeslot+1)
        assertFalse(Validation.isFuseValidAtTimeslot(fuseRoot, state, state.currentTimeslot+2)); // INVALID (timeslot+2)
    }
    
    
    
    @Test
    public void testValidateFuseAtTimeslot_SinglePhaseChargingStation_NoPhaseRotation() throws FuseTreeException {
    	Car car = dataSim.getCars().get(0);
        ChargingStation station = dataSim.getChargingStations().get(0);
        station.fusePhase2 = 0; 
        station.fusePhase3 = 0; 
        station.setPhase2Connected(false); 
        station.setPhase3Connected(false); 
        
        state.currentTimeSeconds = car.timestampArrival.toSecondOfDay();
        state.currentTimeslot = TimeUtil.getTimeslotFromSeconds(state.currentTimeSeconds); 
    	state.addCarAssignment(car, station); 
    	state.addPowerAssignment(car, station, 32, 0, 0); 
        
    	car.setCurrentPlan(new double[96]);
    	car.getCurrentPlan()[state.currentTimeslot+1] = 32; // valid 
    	car.getCurrentPlan()[state.currentTimeslot+2] = 200; // invalid 
         
    	// Uses power assignment
    	Validation.validateFuseAtTimeslot(station, state, -1); 
    	// Uses car assignment
    	Validation.validateFuseAtTimeslot(station, state, state.currentTimeslot+1); 
        
        try {
        	Validation.validateFuseAtTimeslot(station, state, state.currentTimeslot+2); 
        	fail("should have failed"); 
        }
        catch (FuseTreeException e) {
        	assertEquals(200, e.getSumConsumedByPhase(Phase.PHASE_1), 1e-8);
        	assertEquals(200-32, e.getDeltaByPhase(Phase.PHASE_1), 1e-8);
        	assertEquals(0, e.getSumConsumedByPhase(Phase.PHASE_2), 1e-8);
        	assertEquals(0, e.getSumConsumedByPhase(Phase.PHASE_3), 1e-8);
        }
    }
    
    @Test
    public void testValidateFuseAtTimeslot_SinglePhaseChargingStation_WithPhaseRotation() throws FuseTreeException {
    	Car car = dataSim.getCars().get(0);
        ChargingStation station = dataSim.getChargingStations().get(0);
        station.fusePhase2 = 0; 
        station.fusePhase3 = 0; 
        station.setPhase2Connected(false); 
        station.setPhase3Connected(false); 
        
        state.currentTimeSeconds = car.timestampArrival.toSecondOfDay();
        state.currentTimeslot = TimeUtil.getTimeslotFromSeconds(state.currentTimeSeconds); 
    	state.addCarAssignment(car, station); 
    	state.addPowerAssignment(car, station, 32, 0, 0); // phases at charging station
        
    	car.setCurrentPlan(new double[96]);
    	car.getCurrentPlan()[state.currentTimeslot+1] = 32; // valid 
    	car.getCurrentPlan()[state.currentTimeslot+2] = 200; // invalid 
         
    	// Uses power assignment
    	Validation.validateFuseAtTimeslot(station, state, -1); 
    	// Uses car assignment
    	Validation.validateFuseAtTimeslot(station, state, state.currentTimeslot+1); 
        
    	// 2,3,1 matching
    	station.setPhaseMatching(Phase.PHASE_2, Phase.PHASE_3, Phase.PHASE_1);
        try {
        	Validation.validateFuseAtTimeslot(station, state, state.currentTimeslot+2); 
        	fail("should have failed"); 
        }
        catch (FuseTreeException e) {
        	assertEquals(200, e.getSumConsumedByPhase(Phase.PHASE_1), 1e-8);
        	assertEquals(200-32, e.getDeltaByPhase(Phase.PHASE_1), 1e-8);
        	assertEquals(0, e.getSumConsumedByPhase(Phase.PHASE_2), 1e-8);
        	assertEquals(0, e.getSumConsumedByPhase(Phase.PHASE_3), 1e-8);
        }
        
        // 3,1,2 matching
    	station.setPhaseMatching(Phase.PHASE_3, Phase.PHASE_1, Phase.PHASE_2);
        try {
        	Validation.validateFuseAtTimeslot(station, state, state.currentTimeslot+2); 
        	fail("should have failed"); 
        }
        catch (FuseTreeException e) {
        	assertEquals(200, e.getSumConsumedByPhase(Phase.PHASE_1), 1e-8);
        	assertEquals(200-32, e.getDeltaByPhase(Phase.PHASE_1), 1e-8);
        	assertEquals(0, e.getSumConsumedByPhase(Phase.PHASE_2), 1e-8);
        	assertEquals(0, e.getSumConsumedByPhase(Phase.PHASE_3), 1e-8);
        }
    }
    
    
    
    @Test
    public void testValidateFuseAtTimeslot_ThreePhaseEV_SinglePhaseFuse() throws FuseTreeException {
    	
    	Car car = dataSim.getCars().get(0);
    	assertEquals(3, car.sumUsedPhases, 0); 
        ChargingStation station = dataSim.getChargingStations().get(0);
        station.fusePhase1 = 200; // force exception to result from fuse
        station.fusePhase2 = 200; 
        station.fusePhase3 = 200; 
        
        
        Fuse fuseRoot = new Fuse(0, 32, 0, 0, true, false, false, new ArrayList<>());
        fuseRoot.addChild(station);

        FuseTree fuseTree = new FuseTree(fuseRoot, 100);
        state.fuseTree = fuseTree; 
        
        state.currentTimeSeconds = car.timestampArrival.toSecondOfDay();
        state.currentTimeslot = TimeUtil.getTimeslotFromSeconds(state.currentTimeSeconds); 
    	state.addCarAssignment(car, station); 
    	state.addPowerAssignment(car, station, 32, 0, 0); // phases at charging station
        
    	car.setCurrentPlan(new double[96]);
    	car.getCurrentPlan()[state.currentTimeslot+1] = 32; // valid 
    	car.getCurrentPlan()[state.currentTimeslot+2] = 200; // invalid 
         
    	// Uses power assignment
    	Validation.validateFuseAtTimeslot(fuseRoot, state, -1); 
    	// Uses car assignment (valid charge plan)
    	Validation.validateFuseAtTimeslot(fuseRoot, state, state.currentTimeslot+1); 
        
    	// 1,2,3 matching
    	try {
        	Validation.validateFuseAtTimeslot(fuseRoot, state, state.currentTimeslot+2); 
        	fail("should have failed"); 
        }
        catch (FuseTreeException e) {
        	assertEquals(200, e.getSumConsumedByPhase(Phase.PHASE_1), 1e-8);
        	assertEquals(200-32, e.getDeltaByPhase(Phase.PHASE_1), 1e-8);
        	assertEquals(0, e.getSumConsumedByPhase(Phase.PHASE_2), 1e-8);
        	assertEquals(0, e.getSumConsumedByPhase(Phase.PHASE_3), 1e-8);
        }
    	
    	
    	// 2,3,1 matching
    	station.setPhaseMatching(Phase.PHASE_2, Phase.PHASE_3, Phase.PHASE_1);
        try {
        	Validation.validateFuseAtTimeslot(fuseRoot, state, state.currentTimeslot+2); 
        	fail("should have failed"); 
        }
        catch (FuseTreeException e) {
        	assertEquals(200, e.getSumConsumedByPhase(Phase.PHASE_1), 1e-8);
        	assertEquals(200-32, e.getDeltaByPhase(Phase.PHASE_1), 1e-8);
        	assertEquals(0, e.getSumConsumedByPhase(Phase.PHASE_2), 1e-8);
        	assertEquals(0, e.getSumConsumedByPhase(Phase.PHASE_3), 1e-8);
        }
        
        // 3,1,2 matching
    	station.setPhaseMatching(Phase.PHASE_3, Phase.PHASE_1, Phase.PHASE_2);
        try {
        	Validation.validateFuseAtTimeslot(station, state, state.currentTimeslot+2); 
        	fail("should have failed"); 
        }
        catch (FuseTreeException e) {
        	assertEquals(200, e.getSumConsumedByPhase(Phase.PHASE_1), 1e-8);
        	assertEquals(200-32, e.getDeltaByPhase(Phase.PHASE_1), 1e-8);
        	assertEquals(0, e.getSumConsumedByPhase(Phase.PHASE_2), 1e-8);
        	assertEquals(0, e.getSumConsumedByPhase(Phase.PHASE_3), 1e-8);
        	
        }
    	
    }
    
}

