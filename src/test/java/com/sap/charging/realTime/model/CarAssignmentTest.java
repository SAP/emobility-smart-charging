package com.sap.charging.realTime.model; 

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.charging.model.Car;
import com.sap.charging.model.CarFactory;
import com.sap.charging.model.CarFactory.CarModel;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.sim.common.SimulationUnitTest;
import com.sap.charging.util.TimeUtil;



public class CarAssignmentTest extends SimulationUnitTest {

	Car onePhaseCar; 
    
    @BeforeEach
    public void setup() {
    	this.onePhaseCar = CarFactory.builder().set(CarModel.MERCEDES_GLC_350e)
    			.availableTimeslots(0, 95, 96)
    			.build();
    }
    
    @Test
    public void testToString() {
        Car car = dataSim.getCar(0); 
        ChargingStation chargingStation = dataSim.getChargingStation(0); 
        
        CarAssignment carAssignment = new CarAssignment(car, chargingStation);
        
        assertEquals("CarAssignment carId=0, chargingStationId=0", carAssignment.toString());
    }
    
    
    @Test
    public void testForecasting() {
        Car car = dataSim.getCar(0); 
        ChargingStation chargingStation = dataSim.getChargingStation(0); 
        
        CarAssignment carAssignment = new CarAssignment(car, chargingStation);
        carAssignment.setExpectedDepartureTimeSeconds(1000);
        
        int expectedDepartureTimeSeconds = carAssignment.getExpectedDepartureTimeSeconds(); 
        assertEquals(1000, expectedDepartureTimeSeconds); 
        
        int expectedDepartureTimeslot = carAssignment.getExpectedDepartureTimeslot();
        assertEquals(TimeUtil.getTimeslotFromSeconds(1000), expectedDepartureTimeslot);
    }
    
    @Test
    public void testCurrentCalculationNoPhaseRotation() {
        double[] currentPlan = new double[dataSim.getEnergyPriceHistory().getNTimeslots()]; // Ampere per 15 mins timeslot
        currentPlan[1] = 5;
        onePhaseCar.setCurrentPlan(currentPlan);
        ChargingStation chargingStation = dataSim.getChargingStation(0); 
        
        CarAssignment carAssignment = new CarAssignment(onePhaseCar, chargingStation);
        
        // First timeslot should have no current in all cases
        double[] currentPerGridPhase0 = carAssignment.getCurrentPerGridPhase(0);
        assertEquals(0, currentPerGridPhase0[0], 1e-8);
        assertEquals(0, currentPerGridPhase0[1], 1e-8);
        assertEquals(0, currentPerGridPhase0[2], 1e-8);

        double[] currentPerGridPhase1 = carAssignment.getCurrentPerGridPhase(1);
        assertEquals(5, currentPerGridPhase1[0], 1e-8);
        assertEquals(0, currentPerGridPhase1[1], 1e-8);
        assertEquals(0, currentPerGridPhase1[2], 1e-8);
    }
    
    @Test
    public void testCurrentCalculationWithPhaseRotation() {
        double[] currentPlan = new double[dataSim.getEnergyPriceHistory().getNTimeslots()]; // Ampere per 15 mins timeslot
        currentPlan[1] = 5;
        onePhaseCar.setCurrentPlan(currentPlan);
        
        ChargingStation chargingStation = dataSim.getChargingStation(1); // Use one that has a phase rotation, i.e. not the first one
        CarAssignment carAssignment = new CarAssignment(onePhaseCar, chargingStation);
        
        // Matching is 1->2, 2->3, 3->1 (1st phase of charging station is connected to 2nd phase of grid)
        chargingStation.setPhaseMatching(Phase.PHASE_2, Phase.PHASE_3, Phase.PHASE_1);
        
        double[] currentPerGridPhase = carAssignment.getCurrentPerGridPhase(1);
        assertEquals(0, currentPerGridPhase[0], 1e-8);
        assertEquals(5, currentPerGridPhase[1], 1e-8);
        assertEquals(0, currentPerGridPhase[2], 1e-8);
        
        // Matching is 1->2, 2->3, 3->1 (1st phase of charging station is connected to 3rd phase of grid)
        chargingStation.setPhaseMatching(Phase.PHASE_3, Phase.PHASE_1, Phase.PHASE_2);
        currentPerGridPhase = carAssignment.getCurrentPerGridPhase(1);
        assertEquals(0, currentPerGridPhase[0], 1e-8);
        assertEquals(0, currentPerGridPhase[1], 1e-8);
        assertEquals(5, currentPerGridPhase[2], 1e-8);
        
    }
    
    
    
    
}












