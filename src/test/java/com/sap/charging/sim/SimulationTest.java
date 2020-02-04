package com.sap.charging.sim;

import static com.sap.charging.util.JSONKeys.JSON_KEY_PROBLEM_INSTANCE;
import static com.sap.charging.util.JSONKeys.JSON_KEY_SOLUTION;
import static com.sap.charging.util.JSONKeys.JSON_KEY_VARIABLES;
import static com.sap.charging.util.JSONKeys.JSON_KEY_VARIABLE_NAME;
import static com.sap.charging.util.JSONKeys.JSON_KEY_VARIABLE_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.dataGeneration.common.DefaultDataGenerator;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.opt.heuristics.InstanceHeuristicAbsSoCLP;
import com.sap.charging.opt.lp.InstanceLP;
import com.sap.charging.opt.solution.model.DayaheadSchedule;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.StrategyFromDayahead;
import com.sap.charging.realTime.StrategyGreedy;
import com.sap.charging.realTime.model.CarAssignment;
import com.sap.charging.sim.common.SimulationUnitTest;
import com.sap.charging.sim.common.StrategyMock;
import com.sap.charging.sim.event.Event;
import com.sap.charging.sim.event.EventCarArrival;
import com.sap.charging.sim.event.EventCarDeparture;
import com.sap.charging.sim.event.EventCarFinished;
import com.sap.charging.sim.util.SimulationListenerCSV;

public class SimulationTest extends SimulationUnitTest {

	@BeforeEach
	public void setup() {
		Simulation.verbosity = 0; 
		InstanceLP.verbosity = 0; 
		
		this.strategy = new StrategyGreedy();
		this.sim = new Simulation(dataSim, strategy);
		this.sim.init();
	}
	
	@Test
	public void testAddCarAndEvents() {
		for (int timeSeconds=0;timeSeconds<1000;timeSeconds++) {
			sim.simulateNextStep();
		}
		
		DataGenerator temp = new DataGeneratorRandom(0, false);
		temp.setMinCarId(dataSim.getCars().size());
		temp.generateEnergyPriceHistory(96).generateCars(1);
		
		sim.addCar(temp.getCars().get(0), true, true);
		
		assertEquals(sim.getDataSim().getCars().size(), DefaultDataGenerator.nCars+1);
		
		int nArrivalEvents = (int) sim.getSimulationEvents().stream().filter(e -> e instanceof EventCarArrival).count();
		// Check if car arrival event was added, i.e. each car has one arrival event
		assertEquals(nArrivalEvents, sim.getDataSim().getCars().size());
		
		int nDepartureEvents  = (int) sim.getSimulationEvents().stream().filter(e -> e instanceof EventCarDeparture).count();
		// Check if car arrival event was added, i.e. each car has one departure event
		assertEquals(nDepartureEvents, sim.getDataSim().getCars().size());
	}

	@Test
	public void testAddCarWithoutEvents() {
		for (int timeSeconds=0;timeSeconds<1000;timeSeconds++) {
			sim.simulateNextStep();
		}
		
		DataGenerator temp = new DataGeneratorRandom(0, false);
		temp.setMinCarId(dataSim.getCars().size());
		temp.generateEnergyPriceHistory(96).generateCars(1);
		
		sim.addCar(temp.getCars().get(0), false, false);
		
		assertEquals(sim.getDataSim().getCars().size(), DefaultDataGenerator.nCars+1);
		
		int nArrivalEvents = (int) sim.getSimulationEvents().stream().filter(e -> e instanceof EventCarArrival).count();
		// Check if car arrival event was not added, i.e. each car has one arrival event
		assertEquals(nArrivalEvents, sim.getDataSim().getCars().size()-1);
		
		int nDepartureEvents  = (int) sim.getSimulationEvents().stream().filter(e -> e instanceof EventCarDeparture).count();
		// Check if car arrival event was not added, i.e. each car has one departure event
		assertEquals(nDepartureEvents, sim.getDataSim().getCars().size()-1);
	}
	
	@Test
	public void testAddChargingStation() {
		for (int timeSeconds=0;timeSeconds<1000;timeSeconds++) {
			sim.simulateNextStep();
		}
		
		DataGenerator temp = new DataGeneratorRandom(0, false);
		temp.setMinChargingStationId(dataSim.getChargingStations().size());
		temp.generateChargingStations(1);
		
		ChargingStation chargingStation = temp.getChargingStations().get(0);
		sim.addChargingStation(chargingStation);
		
		// Check if added to list
		assertEquals(true, sim.getDataSim().getChargingStations().contains(chargingStation));
		
		// Check if added to fuse tree (first and lowest fuse that has a charging station as a child)
		assertEquals(true, sim.getDataSim().getFuseTree()
				.getRootFuse().getChildren().get(0)
				.getChildren().get(0)
				.getChildren().contains(chargingStation));
		
		
		// Check if added to state as free charging station
		assertEquals(true, sim.getState().getChargingStationsFree().contains(chargingStation));
	}
	
	
	@Test
	public void testInitEvents() {
		this.sim = new Simulation(dataSim, strategy);
		this.sim.init();
		
		// Check total length of events
		Assert.assertEquals(
				dataSim.getCars().size()*2 + // arrival + departure per car
				dataSim.getEnergyPriceHistory().getNTimeslots(),  // one per timeslot
				sim.getSimulationEvents().size()
				);
		
		// Check that events are properly ordered (ascending by timestamp)
		for (int i=0;i<sim.getSimulationEvents().size()-1;i++) {
			Event e1 = sim.getSimulationEvents().get(i);
			Event e2 = sim.getSimulationEvents().get(i+1);
			Assert.assertTrue(e1.timestamp.isBefore(e2.timestamp));
		}
		
	}
	

	@Test
	public void testInitTimedEvents() {
		this.sim = new Simulation(dataSim, strategy);
		this.sim.init();
		
		List<Event> allEvents = sim.getSimulationEvents();
		HashMap<Integer, ArrayList<Event>> timedEvents = sim.getTimedSimulationEvents();
		
		// Check that all events are also in timed events at correct key (time)
		for (Event e : allEvents) {
			int t = e.getSecondsOfDay();
			ArrayList<Event> localTimedEvents = timedEvents.get(t);
			
			assertNotNull(localTimedEvents);
			assertTrue(localTimedEvents.contains(e));
		}
		
	}
	
	@Test
	public void testCSVStrings() {
		
		getCSVGreedy();
		//FileIO.writeFile("gen/performance/simGreedy.csv", csvGreedy);
		
		//getCSVFromDayahead();
		//FileIO.writeFile("gen/performance/simFromDayAhead.csv", csvFromDayahead);
		
	}
	
	private String getCSVGreedy() {
		SimulationListenerCSV listener = new SimulationListenerCSV();
		
		Simulation simGreedy = new Simulation(this.dataSim, new StrategyGreedy());
		simGreedy.addStateListener(listener);
		simGreedy.init();
		simGreedy.simulate();
		
		return listener.getCSVString();
	}
	
	public String getCSVFromDayahead() {
		SimulationListenerCSV listener = new SimulationListenerCSV();
		DataGenerator data = DefaultDataGenerator.getToyDataGenerator();
		
		InstanceHeuristicAbsSoCLP instance = new InstanceHeuristicAbsSoCLP(data);
		instance.constructProblem();
		JSONObject jsonResult = instance.getSolvedProblemInstanceJSON();
		DayaheadSchedule schedule = new DayaheadSchedule(jsonResult);
		StrategyFromDayahead strategy = new StrategyFromDayahead(schedule);
		Simulation simFromDayahead = new Simulation(data, strategy);
		simFromDayahead.addStateListener(listener);
		simFromDayahead.init();
		simFromDayahead.simulate();
		
		return listener.getCSVString();
	}
	
	@Test
	public void testSimulateNextStep() {
		//State state = sim.getState();
		//SimulationResult simulationResult = sim.getSimulationResult();
		
		Simulation sim = new Simulation(dataSimToy, new StrategyGreedy()); 
		sim.init(); 
		Car car = dataSimToy.getCar(0); 
		ChargingStation chargingStation = dataSim.getChargingStation(0); 
		
		// Loop to start of first car
		for (int t=0;t<car.timestampArrival.toSecondOfDay();t++) {
			sim.simulateNextStep();
		}
		
		//state.addCarAssignment(car, chargingStation);
		//state.addPowerAssignment(car, chargingStation, 10, 10, 0);
		
		// Manually perform loop steps - afterwards check how 
		// much the car was charged and whether variablesP were updated
		// accordingly
		//sim.updateState(sim.simStartSeconds);
		
		sim.simulateNextStep();
		
		// Car should be assigned
		CarAssignment carAssignment = sim.getState().getCurrentCarAssignment(car); 
		assertNotNull(carAssignment); 
		
		// 2 phases*10A*1s /3600 to get to Ah
		double ampere = Math.min(chargingStation.fusePhase1*car.sumUsedPhases, car.maxCurrent);
		double expectedAmountCharged1 = CONSTANTS.CHARGING_EFFICIENCY * ampere / 3600.0;
		assertEquals(expectedAmountCharged1, car.getChargedCapacity(), 1E-8);
		
		sim.simulateNextStep();
		double expectedAmountCharged2 = 2*expectedAmountCharged1;
		assertEquals(expectedAmountCharged2, car.getChargedCapacity(), 1E-8);
		
		
	}
	
	@Test
	public void testSimulationResultExportJSON() {
		this.sim.simulate();
		
		SimulationResult simulationResult = this.sim.getSimulationResult();
		JSONObject result = simulationResult.getSolvedProblemInstanceJSON();
		List<CarAssignment> allCarAssignments = sim.getState().getAllCarAssignments();
		
		// Check objects
		assertNotNull(result.get(JSON_KEY_PROBLEM_INSTANCE));
		assertNotNull(result.get(JSON_KEY_SOLUTION));
		
		//System.out.println(result);
		//FileIO.writeFile("vis/data/realTime_solution_greedy.json", result);
		
		// Check solution JSON
		JSONObject solution = (JSONObject) result.get(JSON_KEY_SOLUTION);
		assertNotNull(solution.get("variables"));
		JSONArray variables = (JSONArray) solution.get(JSON_KEY_VARIABLES);
		
		int numX = 0;
		int minNumP = 3*Math.min(dataSim.getCars().size(), dataSim.getChargingStations().size());
		int numP = 0;
		double sumP = 0; // Check whether sum loaded 
		
		for (int i=0;i<variables.size();i++) {
			JSONObject variable = (JSONObject) variables.get(i);
			String variableName = (String) variable.get(JSON_KEY_VARIABLE_NAME);
			double variableValue = (Double) variable.get(JSON_KEY_VARIABLE_VALUE);
			
			assertTrue(variableName.startsWith("X") || variableName.startsWith("P"));
			if (variableName.startsWith("X")) {
				numX++;
			}
			if (variableName.startsWith("P")) {
				numP++;
				sumP += CONSTANTS.CHARGING_EFFICIENCY * variableValue/4;
			}
		}
		
		// Check individual car charged sums
		double expectedCarChargedSum = 0;
		for (Car car : dataSim.getCars()) {
			double expectedCarCharged = car.getChargedCapacity();	
			assertEquals(expectedCarCharged, simulationResult.getSumCharged(car), 1e-8);
			//System.out.println("Expected: " + expectedCarCharged + ", simulation: " + simulationResult.getSumCharged(car));
			
			expectedCarChargedSum += car.getChargedCapacity();
		}		

		assertEquals(expectedCarChargedSum, simulationResult.getSumCharged(), 1e-8);
		assertEquals(expectedCarChargedSum, sumP, 1e-8);
		
		assertEquals(numX, allCarAssignments.size());
		assertTrue(numP >= minNumP);
		
		
	}
	
	
	@Test
	public void testSimStrategyMock() {
		StrategyMock strategyMock = new StrategyMock();
		Simulation sim = new Simulation(dataSim, strategyMock);
		sim.init();
		sim.simulate();
		
		assertEquals(dataSim.getCars().size(), strategyMock.counterCarArrival);
		// No cars will be finished as none will be assigned in mock strategy
		assertEquals(0, strategyMock.counterCarFinished);
		assertEquals(dataSim.getCars().size(), strategyMock.counterCarDeparture);
		assertEquals(dataSim.getEnergyPriceHistory().getNTimeslots(), strategyMock.counterEnergyPriceChange);
	}
	
	
	@Test
	public void testSimStrategyGreedy() {
		this.sim.simulate();
		
		// Check whether there are CarFinished events for all cars
		List<Event> allEvents = sim.getSimulationEvents();
		int numEventsCarFinished = (int) allEvents.stream().filter(e -> e instanceof EventCarFinished).count();
		
		int nCarsFullyCharged = 0; 
		for (Car car : this.sim.getState().cars) {
			if (car.isFullyCharged()) {
				nCarsFullyCharged++; 
			}
		}
		
		assertEquals(nCarsFullyCharged, numEventsCarFinished);
	}
	
	@Disabled
	@Test
	public void testStrategyFromDayAhead() throws ParseException {
		DataGenerator data = DefaultDataGenerator.getToyDataGenerator();
		
		InstanceHeuristicAbsSoCLP instance = new InstanceHeuristicAbsSoCLP(data);
		instance.constructProblem();
		JSONObject json = instance.getSolvedProblemInstanceJSON();
		
		// DayAhead schedule from 2016-12-02 (is filled)
		//String jsonPath = "gen/performance/cc1d9acb-15fb-4fb9-bf7c-8c34e6bbf9a0.json";
		//JSONObject json = (JSONObject) (new JSONParser()).parse(FileIO.readFile(jsonPath));
		DayaheadSchedule schedule = new DayaheadSchedule(json);
		StrategyFromDayahead strategy = new StrategyFromDayahead(schedule);
		Simulation sim = new Simulation(data, strategy);
		sim.init();
		sim.simulate();
		
		//FileIO.writeFile("vis/data/realTime_solution_absSoCLP.json", sim.getSimulationResult().getSolvedProblemInstanceJSON());
		
	}
	
	@Test
	public void testSimStrategyAlgorithmic() {
		/*DataGeneratorReal data = new DataGeneratorReal("2016-12-28", 0, true, true);
		data.setIdealCars(true);
		data.setIdealChargingStations(true);
		data.generateAll();
		
		InstanceHeuristicGreedyLP instance = new InstanceHeuristicGreedyLP(data);
		JSONObject solvedProblemInstance = instance.getSolvedProblemInstanceJSON();
		DayaheadSchedule schedule = new DayaheadSchedule(solvedProblemInstance);
		StrategyAlgorithmic strategy = new StrategyAlgorithmic(schedule);
		
		DataGeneratorRandom dataRandom = new DataGeneratorRandom(1, false);
		dataRandom.setIdealCars(true);
		dataRandom.setIdealChargingStations(true);
		dataRandom.setMinCarId(data.getCars().size());
		dataRandom.setMinChargingStationId(data.getChargingStations().size());
		dataRandom.generateEnergyPriceHistory(96).generateCars(8);
				  //.generateChargingStations(5);
		
		data.getCars().addAll(dataRandom.getCars());*/
		//data.getChargingStations().addAll(dataRandom.getChargingStations());
		
		//DataRandomizer dataRandomizer = new DataRandomizer(data, 0.2, 0);
		//dataRandomizer.generateAll();
		
		double fuseSize = 200;
		CONSTANTS.FUSE_LEVEL_2_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_1_SIZE = fuseSize;
		CONSTANTS.FUSE_LEVEL_0_SIZE = fuseSize;
		int nCars = 12;
		/*DataGeneratorRandom dataRandom1 = new DataGeneratorRandom(0, false);
		dataRandom1.generateEnergyPriceHistory(96)
				  .generateCars(nCars)
				  .generateChargingStations(nCars)
				  .generateFuseTree(5000, true); 
		
		Simulation sim1 = new Simulation(dataRandom1, new StrategyGreedy());
		sim1.init();
		sim1.simulate();
		
		FileIO.writeFile("vis/data/realTime_solution_greedy.json", sim1.getSimulationResult().getSolvedProblemInstanceJSON());
		*/
		
		DataGeneratorRandom dataRandom2 = new DataGeneratorRandom(0, false);
		dataRandom2.generateEnergyPriceHistory(96)
				  .generateCars(nCars)
				  .generateChargingStations(nCars)
				  .generateFuseTree(nCars, true); 
		
		/*int timesShortStay = 0;
		for (Car car : dataRandom2.getCars()) {
			if (car.getLastAvailableTimeslot() - car.getFirstAvailableTimeslot() <= 16) {
				timesShortStay++;
			}
		}
		System.out.println("timesShortStay=" + timesShortStay); */
		
		StrategyAlgorithmic strat = new StrategyAlgorithmic();
		Simulation sim2 = new Simulation(dataRandom2, strat);
		sim2.init();
		sim2.simulate();
		
		
		/*for (Car car : dataRandom2.getCars()) {
			System.out.println("Car n=" + car.getId() + ", curCapacity=" + car.getCurrentCapacity() + ", chargedCapacity=" + car.getChargedCapacity() + ", maxCapacity=" + car.maxCapacity+  ", planned=" + strat.getPlannedCapacity(car, car.timestampArrival.toSecondOfDay()));
			
		}*/
		
		//System.out.println("Validation.checkSummedChildConsumption called:" + Validation.checkSummedChildConsumption);
		//System.out.println("Validation.checkSummedChildConsumptionAtTimeslot called:" + Validation.checkSummedChildConsumptionAtTimeslot);
		
		
		//FileIO.writeFile("vis/data/realTime_solution_algorithmic.json", sim2.getSimulationResult().getSolvedProblemInstanceJSON());
	}
	
	
}
