package com.sap.charging.realTime.model; 

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.model.Fuse;
import com.sap.charging.sim.common.SimulationUnitTest;
import com.sap.charging.util.TimeUtil;



public class CarAssignmentTest extends SimulationUnitTest {

    
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
    public void testCurrentCalculation_NoPhaseRotation() {
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
    public void testCurrentCalculation_WithPhaseRotation() {
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
        
        double[] currentPerStationPhase = carAssignment.getCurrentPerStationPhase(1); 
        assertEquals(5, currentPerStationPhase[0], 1e-8);
        assertEquals(0, currentPerStationPhase[1], 1e-8);
        assertEquals(0, currentPerStationPhase[2], 1e-8);
        
        
        // Matching is 1->2, 2->3, 3->1 (1st phase of charging station is connected to 3rd phase of grid)
        chargingStation.setPhaseMatching(Phase.PHASE_3, Phase.PHASE_1, Phase.PHASE_2);
        currentPerGridPhase = carAssignment.getCurrentPerGridPhase(1);
        assertEquals(0, currentPerGridPhase[0], 1e-8);
        assertEquals(0, currentPerGridPhase[1], 1e-8);
        assertEquals(5, currentPerGridPhase[2], 1e-8);
        
        currentPerStationPhase = carAssignment.getCurrentPerStationPhase(1); 
        assertEquals(5, currentPerStationPhase[0], 1e-8);
        assertEquals(0, currentPerStationPhase[1], 1e-8);
        assertEquals(0, currentPerStationPhase[2], 1e-8);
        
    }
    
	@Test
	public void testCurrentCalculation_ThreePhaseEV_SinglePhaseStation_Phase1_NoPhaseRotation() {
		double[] currentPlan = new double[96]; 
		currentPlan[1] = 32; 
		threePhaseCar.setCurrentPlan(currentPlan);
		
		ChargingStation station = dataSim.getChargingStation(0); 
		station.fusePhase1 = 32; 
		station.fusePhase2 = 0; 
		station.fusePhase3 = 0; 
		station.setPhase2Connected(false);
		station.setPhase3Connected(false);
		
		CarAssignment carAssignment = new CarAssignment(threePhaseCar, station);
		double[] currentPerGridPhase = carAssignment.getCurrentPerGridPhase(1);
		assertEquals(32, currentPerGridPhase[0], 1e-8);
		assertEquals(0, currentPerGridPhase[1], 1e-8);
		assertEquals(0, currentPerGridPhase[2], 1e-8);
	        
		double[] currentPerStationPhase = carAssignment.getCurrentPerStationPhase(1); 
        assertEquals(32, currentPerStationPhase[0], 1e-8);
        assertEquals(0, currentPerStationPhase[1], 1e-8);
        assertEquals(0, currentPerStationPhase[2], 1e-8);
       
		
	}
	
	@Test
	public void testCurrentCalculation_ThreePhaseEV_SinglePhaseStation_Phase2_NoPhaseRotation() {
		double[] currentPlan = new double[96]; 
		currentPlan[1] = 32; 
		threePhaseCar.setCurrentPlan(currentPlan);
		
		ChargingStation station = dataSim.getChargingStation(0); 
		station.fusePhase1 = 0; 
		station.fusePhase2 = 32; 
		station.fusePhase3 = 0; 
		station.setPhase1Connected(false);
		station.setPhase3Connected(false);
		
		CarAssignment carAssignment = new CarAssignment(threePhaseCar, station);
		double[] currentPerGridPhase = carAssignment.getCurrentPerGridPhase(1);
		assertEquals(0, currentPerGridPhase[0], 1e-8);
		assertEquals(32, currentPerGridPhase[1], 1e-8);
		assertEquals(0, currentPerGridPhase[2], 1e-8);
	   
		double[] currentPerStationPhase = carAssignment.getCurrentPerStationPhase(1); 
        assertEquals(0, currentPerStationPhase[0], 1e-8);
        assertEquals(32, currentPerStationPhase[1], 1e-8);
        assertEquals(0, currentPerStationPhase[2], 1e-8);
       
	}
	
	@Test
	public void testCurrentCalculation_ThreePhaseEV_SinglePhaseStation_Phase3_NoPhaseRotation() {
		double[] currentPlan = new double[96]; 
		currentPlan[1] = 32; 
		threePhaseCar.setCurrentPlan(currentPlan);
		
		ChargingStation station = dataSim.getChargingStation(0); 
		station.fusePhase1 = 0; 
		station.fusePhase2 = 0; 
		station.fusePhase3 = 32; 
		station.setPhase1Connected(false);
		station.setPhase2Connected(false);
		
		CarAssignment carAssignment = new CarAssignment(threePhaseCar, station);
		double[] currentPerGridPhase = carAssignment.getCurrentPerGridPhase(1);
		assertEquals(0, currentPerGridPhase[0], 1e-8);
		assertEquals(0, currentPerGridPhase[1], 1e-8);
		assertEquals(32, currentPerGridPhase[2], 1e-8);
	   
		double[] currentPerStationPhase = carAssignment.getCurrentPerStationPhase(1); 
        assertEquals(0, currentPerStationPhase[0], 1e-8);
        assertEquals(0, currentPerStationPhase[1], 1e-8);
        assertEquals(32, currentPerStationPhase[2], 1e-8);
       
	}
	
	@Test
	public void testCurrentCalculation_ThreePhaseEV_SinglePhaseStation_WithPhaseRotation() {
		double[] currentPlan = new double[96]; 
		currentPlan[1] = 32; 
		threePhaseCar.setCurrentPlan(currentPlan);
		
		ChargingStation station = dataSim.getChargingStation(0); 
		station.fusePhase1 = 32; 
		station.fusePhase2 = 0; 
		station.fusePhase3 = 0; 
		station.setPhase2Connected(false);
		station.setPhase3Connected(false);
		
		CarAssignment carAssignment = new CarAssignment(threePhaseCar, station);

		// 2,3,1 rotation
		station.setPhaseMatching(Phase.PHASE_2, Phase.PHASE_3, Phase.PHASE_1);;
		double[] currentPerGridPhase = carAssignment.getCurrentPerGridPhase(1);
		assertEquals(0, currentPerGridPhase[0], 1e-8);
		assertEquals(32, currentPerGridPhase[1], 1e-8);
		assertEquals(0, currentPerGridPhase[2], 1e-8);
		
		double[] currentPerStationPhase = carAssignment.getCurrentPerStationPhase(1); 
        assertEquals(32, currentPerStationPhase[0], 1e-8);
        assertEquals(0, currentPerStationPhase[1], 1e-8);
        assertEquals(0, currentPerStationPhase[2], 1e-8);
       
		
		// 3,1,2 rotation
		station.setPhaseMatching(Phase.PHASE_3, Phase.PHASE_1, Phase.PHASE_2);;
		currentPerGridPhase = carAssignment.getCurrentPerGridPhase(1);
		assertEquals(0, currentPerGridPhase[0], 1e-8);
		assertEquals(0, currentPerGridPhase[1], 1e-8);
		assertEquals(32, currentPerGridPhase[2], 1e-8);
				
		currentPerStationPhase = carAssignment.getCurrentPerStationPhase(1); 
        assertEquals(32, currentPerStationPhase[0], 1e-8);
        assertEquals(0, currentPerStationPhase[1], 1e-8);
        assertEquals(0, currentPerStationPhase[2], 1e-8);
       
		
	}
	
	@Test
	public void testCurrentCalculation_ThreePhaseEV_SinglePhaseFuse_WithPhaseRotation() {
	
		double[] currentPlan = new double[96]; 
		currentPlan[1] = 32; 
		threePhaseCar.setCurrentPlan(currentPlan);
		
		ChargingStation station = dataSim.getChargingStation(0); 

		Fuse fuse = new Fuse(0, 32, 0, 0, true, false, false, new ArrayList<>()); 
		fuse.addChild(station);
		
		CarAssignment carAssignment = new CarAssignment(threePhaseCar, station);

		// 3 phase EV will always try to charge on all phases
		
		// 1,2,3 rotation
		double[] currentPerGridPhase = carAssignment.getCurrentPerGridPhase(1);
		assertEquals(32, currentPerGridPhase[0], 1e-8);
		assertEquals(0, currentPerGridPhase[1], 1e-8);
		assertEquals(0, currentPerGridPhase[2], 1e-8);
		
		double[] currentPerStationPhase = carAssignment.getCurrentPerStationPhase(1); 
        assertEquals(32, currentPerStationPhase[0], 1e-8);
        assertEquals(0, currentPerStationPhase[1], 1e-8);
        assertEquals(0, currentPerStationPhase[2], 1e-8);
		
		// 2,3,1 rotation
		station.setPhaseMatching(Phase.PHASE_2, Phase.PHASE_3, Phase.PHASE_1);;
		currentPerGridPhase = carAssignment.getCurrentPerGridPhase(1);
		assertEquals(32, currentPerGridPhase[0], 1e-8);
		assertEquals(0, currentPerGridPhase[1], 1e-8);
		assertEquals(0, currentPerGridPhase[2], 1e-8);
		
		currentPerStationPhase = carAssignment.getCurrentPerStationPhase(1); 
        assertEquals(0, currentPerStationPhase[0], 1e-8);
        assertEquals(0, currentPerStationPhase[1], 1e-8);
        assertEquals(32, currentPerStationPhase[2], 1e-8); // EV has the opportunity to charge on 3rd phase seen from charging station
		
		// 3,1,2 rotation
		station.setPhaseMatching(Phase.PHASE_3, Phase.PHASE_1, Phase.PHASE_2);;
		currentPerGridPhase = carAssignment.getCurrentPerGridPhase(1);
		assertEquals(32, currentPerGridPhase[0], 1e-8);
		assertEquals(0, currentPerGridPhase[1], 1e-8);
		assertEquals(0, currentPerGridPhase[2], 1e-8);
		
		currentPerStationPhase = carAssignment.getCurrentPerStationPhase(1); 
        assertEquals(0, currentPerStationPhase[0], 1e-8);
        assertEquals(32, currentPerStationPhase[1], 1e-8);
        assertEquals(0, currentPerStationPhase[2], 1e-8);
		
	}
	
	@Test
	public void testCurrentCalculation_SinglePhaseEV_SinglePhaseFuse_WithPhaseRotation() {
	
		double[] currentPlan = new double[96]; 
		currentPlan[1] = 32; 
		onePhaseCar.setCurrentPlan(currentPlan);
		
		ChargingStation station = dataSim.getChargingStation(0); 

		Fuse fuse = new Fuse(0, 32, 0, 0, true, false, false, new ArrayList<>()); 
		fuse.addChild(station);
		
		CarAssignment carAssignment = new CarAssignment(onePhaseCar, station);

		
		// 1,2,3 rotation
		double[] currentPerGridPhase = carAssignment.getCurrentPerGridPhase(1);
		assertEquals(32, currentPerGridPhase[0], 1e-8);
		assertEquals(0, currentPerGridPhase[1], 1e-8);
		assertEquals(0, currentPerGridPhase[2], 1e-8);
		
		double[] currentPerStationPhase = carAssignment.getCurrentPerStationPhase(1); 
        assertEquals(32, currentPerStationPhase[0], 1e-8);
        assertEquals(0, currentPerStationPhase[1], 1e-8);
        assertEquals(0, currentPerStationPhase[2], 1e-8);
		
        
        // 1 phase can't charge with 2,3,1 or 3,1,2 rotation
		// 2,3,1 rotation
		station.setPhaseMatching(Phase.PHASE_2, Phase.PHASE_3, Phase.PHASE_1);;
		currentPerGridPhase = carAssignment.getCurrentPerGridPhase(1);
		assertEquals(0, currentPerGridPhase[0], 1e-8);
		assertEquals(0, currentPerGridPhase[1], 1e-8);
		assertEquals(0, currentPerGridPhase[2], 1e-8);
		
		currentPerStationPhase = carAssignment.getCurrentPerStationPhase(1); 
        assertEquals(0, currentPerStationPhase[0], 1e-8);
        assertEquals(0, currentPerStationPhase[1], 1e-8);
        assertEquals(0, currentPerStationPhase[2], 1e-8);
		
		
		// 3,1,2 rotation
		station.setPhaseMatching(Phase.PHASE_3, Phase.PHASE_1, Phase.PHASE_2);;
		currentPerGridPhase = carAssignment.getCurrentPerGridPhase(1);
		assertEquals(0, currentPerGridPhase[0], 1e-8);
		assertEquals(0, currentPerGridPhase[1], 1e-8);
		assertEquals(0, currentPerGridPhase[2], 1e-8);
		
		currentPerStationPhase = carAssignment.getCurrentPerStationPhase(1); 
        assertEquals(0, currentPerStationPhase[0], 1e-8);
        assertEquals(0, currentPerStationPhase[1], 1e-8);
        assertEquals(0, currentPerStationPhase[2], 1e-8);
		
	}
	

    
    
    
    
}












