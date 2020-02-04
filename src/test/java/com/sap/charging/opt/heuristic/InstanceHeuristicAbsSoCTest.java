package com.sap.charging.opt.heuristic;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.common.DefaultDataGenerator;
import com.sap.charging.opt.heuristics.InstanceHeuristicAbsSoCLP;
import com.sap.charging.opt.lp.InstanceLP;

public class InstanceHeuristicAbsSoCTest {
	
	@BeforeEach
	public void setup() {
		InstanceLP.verbosity = 0; 
	}
	
	@Disabled
	@Test
	public void testCompleteExampleReal() {
		//DataGeneratorReal data = new DataGeneratorReal("2017-11-16", 0, true, true);
		DataGenerator data = DefaultDataGenerator.getToyDataGenerator();
		
		InstanceHeuristicAbsSoCLP instance = new InstanceHeuristicAbsSoCLP(data);
		instance.constructProblem();
		instance.getSolvedProblemInstanceJSON();
		
	}
}
