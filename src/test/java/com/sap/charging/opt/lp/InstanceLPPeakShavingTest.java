package com.sap.charging.opt.lp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.common.DefaultDataGenerator;
import com.sap.charging.opt.heuristics.InstanceHeuristicAbsSoCLP;

public class InstanceLPPeakShavingTest {
	
	
	@BeforeEach
	public void setup() {
		InstanceLP.verbosity = 0; 
	}
	@Disabled
	@Test
	public void testCompleteExampleToyWithHeuristic() {
		DataGenerator data = DefaultDataGenerator.getToyDataGenerator(); 

		InstanceHeuristicAbsSoCLP instance = new InstanceHeuristicAbsSoCLP(data);
		instance.getInstanceLP().objectiveEnergyCosts.setWeight(0);
		instance.getInstanceLP().objectiveLoadImbalance.setWeight(0);
		instance.getInstanceLP().objectivePeakShaving.setWeight(1e10);
		
		instance.constructProblem();
		instance.getSolvedProblemInstanceJSON(); 
		
		
		//FileIO.writeFile("vis/data/solution_absSoCLP.json", instance.getSolvedProblemInstanceJSON());
		//InstanceHeuristicAbsSoCLP instance = new InstanceHeuristicAbsSoCLP(data);
	}
	
	
}
