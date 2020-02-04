package com.sap.charging.model;

import static org.junit.Assert.fail;

import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.charging.model.CarFactory.CarModel;
import com.sap.charging.model.CarFactory.CarType;
import com.sap.charging.sim.Simulation;

public class CarFactoryTest {
	
	
	@BeforeEach
	public void setup() {
		Simulation.verbosity = 0; 
	}
	
	@Test
	public void testEnums() {
		CarFactory.CarType.values();
		CarFactory.CarType.valueOf("BEV");
		CarFactory.CarModel.values();
		CarFactory.CarModel.valueOf("NISSAN_LEAF_2016");
	}

	@Test
	public void testBuildInvalidCar() {
		int nTimeslots = 10;
		double curCapacity = 50;
		double maxCapacity = 100;
		double minCurrent = 10;
		double maxCurrent = 20;
		
		try {
			CarFactory.builder()
					.set(CarModel.NISSAN_LEAF_2016)
					.availableTimeslots(16, 11, nTimeslots)
					.availableTimestamps(LocalTime.of(4, 0), LocalTime.of(4, 45))
					.immediateStart(true)
					.suspendable(true)
					.canUseVariablePower(false)
					.carType(CarType.BEV)
					.currentCapacity(curCapacity)
					.maxCapacity(maxCapacity)
					.minCurrent(minCurrent)
					.maxCurrent(maxCurrent)
					.phases(true, true, true)
					.phases(false, false, false)
					.phases(1.0, 0.5, 0) // this is used		
					.build();
			fail("Sum used phases * minCurrent does not equal minCurrent"); 
		}
		catch (Exception e) { }
		
	}

}
