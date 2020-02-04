package com.sap.charging.realTime.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.SortableElement;

public class TimeslotSorterTest {
	
	DataGenerator data; 
	StrategyAlgorithmic strategy; 
	Simulation sim; 

	@BeforeEach
	public void setup() {
		Simulation.verbosity = 0;
		
		data = new DataGeneratorRandom(0, false);
		data.generateEnergyPriceHistory(96)
			.generateCars(20)
			.generateChargingStations(20)
			.generateFuseTree(20, true);
			
		strategy = new StrategyAlgorithmic();
		sim = new Simulation(data, strategy);
		sim.init();
		sim.simulateNextStep();
	}
	
	@Test
	public void sortByPeakDemand() {
		
		data.getCar(0).setCurrentPlan(new double[96]);
		data.getCar(0).getCurrentPlan()[11] = 32;
		

		data.getCar(1).setCurrentPlan(new double[96]);
		data.getCar(1).getCurrentPlan()[9] = 32;
		data.getCar(1).getCurrentPlan()[11] = 32;
		data.getCar(1).getCurrentPlan()[15] = 32;
		
		List<SortableElement<Integer>> result = TimeslotSorter.getSortedTimeslots(sim.getState(), 10, 20, TimeslotSortingCriteria.PEAK_DEMAND);
		assertEquals(10, result.get(0).index, 1e-8);
		assertEquals(15, result.get(8).index, 1e-8);
		assertEquals(11, result.get(9).index, 1e-8);
		
	}
	
	
	
	
	@Test
	public void sortByPrice()  {
		List<SortableElement<Integer>> result = TimeslotSorter.getSortedTimeslots(sim.getState(), 10, 20, TimeslotSortingCriteria.PRICE);
		
		assertTrue(result.get(0).value <= result.get(1).value);
		assertTrue(result.get(1).value <= result.get(2).value);
	}
	
}








