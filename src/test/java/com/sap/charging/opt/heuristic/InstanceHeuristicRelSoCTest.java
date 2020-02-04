package com.sap.charging.opt.heuristic;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.model.Car;
import com.sap.charging.opt.heuristics.InstanceHeuristicRelSoCLP;
import com.sap.charging.opt.heuristics.util.CarAssignmentPriority;
import com.sap.charging.opt.lp.InstanceLP;

public class InstanceHeuristicRelSoCTest {
	
	@BeforeEach
	public void setup() {
		InstanceLP.verbosity = 0; 
	}
	
	
	@Test
	public void testOrdering() {
		DataGeneratorRandom data = new DataGeneratorRandom(0, false);
		
		data.setIdealCars(true);
		data.setIdealChargingStations(true);
		
		data.generateEnergyPriceHistory(96)
			.generateCars(10)
			.generateChargingStations(5)
			.generateFuseTree(100, true);
		
		List<Integer> sortedCarIDs = CarAssignmentPriority.sortCarIdByRelSoC(data.getCars());
		
		double previousSoC = 0;
		for (int indexList=0;indexList<sortedCarIDs.size();indexList++) {
			int indexN = sortedCarIDs.get(indexList);
			Car car = data.getCars().get(indexN);
			double soc = car.getCurrentCapacity() / car.getMaxCapacity();

			if (indexList>0) {
				assertTrue(previousSoC <= soc);
			}
			
			previousSoC = soc;
		}
	}
	@Disabled
	@Test
	public void testCompleteExample() {
		DataGeneratorRandom data = new DataGeneratorRandom(0, false);
		
		data.setIdealCars(true);
		data.setIdealChargingStations(true);
		
		data.generateEnergyPriceHistory(96)
			.generateCars(10)
			.generateChargingStations(5)
			.generateFuseTree(100, true);
		
		InstanceHeuristicRelSoCLP instance = new InstanceHeuristicRelSoCLP(data);
		instance.constructProblem();
		instance.getSolvedProblemInstanceJSON(); 
		
	}
	
}
