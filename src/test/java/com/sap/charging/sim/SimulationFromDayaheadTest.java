package com.sap.charging.sim;

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.charging.model.Car;
import com.sap.charging.opt.heuristics.InstanceHeuristicAbsSoCLP;
import com.sap.charging.opt.lp.InstanceLP;
import com.sap.charging.opt.lp.Variable;
import com.sap.charging.opt.solution.model.DayaheadSchedule;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.StrategyFromDayahead;
import com.sap.charging.sim.common.SimulationUnitTest;

@Disabled 
// Disabled until SCIP installed
public class SimulationFromDayaheadTest extends SimulationUnitTest {

	private DayaheadSchedule schedule;
	private InstanceHeuristicAbsSoCLP instance;
	
	@BeforeEach
	public void setup() {
		Simulation.verbosity = 0; 
		InstanceLP.verbosity = 0; 
		
		instance = new InstanceHeuristicAbsSoCLP(dataSimToy);
		instance.constructProblem();
		JSONObject result = instance.getSolvedProblemInstanceJSON();
		//FileIO.writeFile("vis/data/solution_" + instance.getMethod() + ".json", result);
		
		schedule = new DayaheadSchedule(result);

	}

	
	@Test
	public void testStrategyFromDayahead() {
		this.strategy = new StrategyFromDayahead(schedule);
		this.sim = new Simulation(dataSimToy, strategy);
		this.sim.init();
		this.sim.simulate();

		// Check that realtime is somewhat the same to original schedule
		// Check same assignments as originally planned
		Collection<Variable> originalVariablesX = schedule.getVariablesX().values();
		for (Variable originalVariable : originalVariablesX) {
			
			// Check that the sim assignment strings contains 
			// original assigment strings
			assertTrue(sim.getState().getAllCarAssignments().stream()
					.map(a -> a.toVariableX().getNameWithIndices())
					.collect(Collectors.toList())
					.contains(originalVariable.getNameWithIndices()));
		}
		
		sim.getSimulationResult().getSolvedProblemInstanceJSON();
		
		//String filePath = "vis/data/realTime_solution_" + instance.getMethod() + ".json";
		//FileIO.writeFile(filePath, result);
	}
	
	@Test
	public void testStrategyAlgorithmicFromDayahead() {
		this.strategy = new StrategyAlgorithmic(schedule);
		this.sim = new Simulation(dataSimToy, strategy);
		this.sim.init();
		this.sim.simulate();


		// Check that realtime is somewhat the same to original schedule
		// Check same assignments as originally planned
		Collection<Variable> originalVariablesX = schedule.getVariablesX().values();
		
		
		/*System.out.println(originalVariablesX);
		System.out.println(sim.getState().getAllCarAssignments().stream()
					.map(a -> a.toVariableX().getNameWithIndices())
					.collect(Collectors.toList()));
		
		for (Variable originalVariable : originalVariablesX) {
			
			// Check that the sim assignment strings contains 
			// original assigment strings
			assertTrue(sim.getState().getAllCarAssignments().stream()
					.map(a -> a.toVariableX().getNameWithIndices())
					.collect(Collectors.toList())
					.contains(originalVariable.getNameWithIndices()));
			In strategyAlgorithmic first free charging station is used!
		}*/
		
		// Check that those cars originally assigned in day ahead get to charge
		for (Variable originalVariable : originalVariablesX) {
			Car car = dataSimToy.getCar(originalVariable.getIndex("n"));
			assertTrue(car.getChargedCapacity() > 0);
		}
		
		sim.getSimulationResult().getSolvedProblemInstanceJSON();
		
		//String filePath = "vis/data/realTime_solution_" + instance.getMethod() + ".json";
		//FileIO.writeFile(filePath, result);
	}	

}
