package com.sap.charging.sim;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyUtil;
import com.sap.charging.model.Fuse;
import com.sap.charging.model.FuseTreeNode;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.opt.util.MethodTimer;
import com.sap.charging.opt.util.MethodTimerState;
import com.sap.charging.realTime.State;
import com.sap.charging.realTime.Strategy;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.model.CarAssignment;
import com.sap.charging.realTime.model.PowerAssignment;
import com.sap.charging.sim.eval.Validation;
import com.sap.charging.sim.eval.exception.SimulationInvalidStateException;
import com.sap.charging.sim.eval.exception.ValidationException;
import com.sap.charging.sim.event.Event;
import com.sap.charging.sim.event.EventCarArrival;
import com.sap.charging.sim.event.EventCarDeparture;
import com.sap.charging.sim.event.EventCarFinished;
import com.sap.charging.sim.event.EventEnergyPriceChange;
import com.sap.charging.sim.util.SimulationListener;
import com.sap.charging.sim.util.SimulationListenerCSV;
import com.sap.charging.sim.util.SimulationListenerJSON;
import com.sap.charging.util.Callback;
import com.sap.charging.util.Loggable;
import com.sap.charging.util.TimeUtil;

public class Simulation implements Loggable {

	private final DataGenerator dataSim;
	
	/**
	 * Length of simulation
	 * Timeslot RELATIVE to DataGenerator.START_BASE_TIMESLOT (e.g. 20)
	 */
	public final int simTimeslots;
	public final int simStartSeconds;
	private final int simNumSeconds;
	private final Strategy strategy;
	
	private int intervalReoptimizationEvents = 900;
	
	private final State state;
	private final HashMap<Integer, ArrayList<Event>> timedEvents;
	private final List<Event> allEvents;
	
	private final SimulationResult simulationResult;
	private final MethodTimerState timeResponse;
	private boolean enableCSVStorage;
	private boolean enableJSONStorage;
	
	/**
	 * These listeners get called once per second with the 
	 * current state.
	 */
	private ArrayList<SimulationListener> stateListeners;
	
	/**
	 * 2: Print everything
	 * 1: Print unique things (e.g. constructor, start simulation)
	 * 0: Print nothing
	 */
	public static int verbosity = 2;
	public int getVerbosity() {
		return verbosity;
	}
	
	
	
	/**
	 * 
	 * @param dataGenerator Possibly randomized data for simulation (before chaos)
	 */
	public Simulation(DataGenerator dataGenerator, Strategy strategy) {
		this.dataSim = dataGenerator;
		this.simTimeslots = dataSim.getEnergyPriceHistory().getNTimeslots();
		this.simStartSeconds = DataGenerator.START_BASE_TIMESLOT * 15 * 60; 
		this.simNumSeconds = simTimeslots * 15 * 60;
		this.strategy = strategy;
		
		log(1, "Constructing simulation with " 
					+ simTimeslots + " timeslots (" + simNumSeconds + "s)"
					+ ", " + dataSim.getCars().size() + " cars"
					+ ", " + dataSim.getChargingStations().size() + " charging stations, and fuse tree:");
		log(1, dataSim.getFuseTree());
		
		this.state = new State(simStartSeconds, dataSim.getChargingStations(), 
				dataSim.getFuseTree(), dataSim.getCars(), dataSim.getEnergyPriceHistory());
		this.allEvents = new ArrayList<>();
		this.timedEvents = new HashMap<>(dataSim.getEnergyPriceHistory().getNTimeslots() + 
				this.getDataSim().getCars().size()*3);
		
		this.simulationResult = new SimulationResult(dataSim, state, strategy);

		this.timeResponse = this.simulationResult.timeSolution;
		this.stateListeners = new ArrayList<>();
	}
	
	/**
	 * Default is 900s
	 * @return
	 */
	public int getIntervalReoptimizationEvents() {
		return intervalReoptimizationEvents;
	}
	public void setIntervalReoptimizationEvents(int intervalReoptimizationEvents) {
		this.intervalReoptimizationEvents = intervalReoptimizationEvents;
	}
	
	
	
	public void init() {
		log(1, "Initializing sim events...");
		
		// Construct list of SimulationEvent for strategy to react to
		// Add car arrivals and departures
		log(1, "Adding " + dataSim.getCars().size()*2 + " car events...");
		for (Car car : dataSim.getCars()) {
			addCar(car, true, true);
		}
		
		// Add energy price changes (every intervalReoptimizationEvents seconds (default 900s)
		int nEnergyPriceChangeEvents = 0;
		for (int time=simStartSeconds;time<24*60*60;time+=intervalReoptimizationEvents) {
			Event ePriceChange = new EventEnergyPriceChange(LocalTime.ofSecondOfDay(time), dataSim.getEnergyPriceHistory());
			addEvent(ePriceChange);
			nEnergyPriceChangeEvents++;
		}
		log(1, "Added " + nEnergyPriceChangeEvents  + " energy price change events...");
		
		
		// Order list 
		allEvents.sort(new Comparator<Event>() {
			@Override
			public int compare(Event e1, Event e2) {
				return e1.timestamp.isBefore(e2.timestamp) ? -1 : 1;
			}
		});
		log(1, "Added " + allEvents.size() + " events.");
		log(1, "First event is at t=" + allEvents.get(0).getSecondsOfDay()
				+ ", Last event is at t=" + allEvents.get(allEvents.size()-1).getSecondsOfDay());
		
		if (isCSVStorageEnabled()) {
			log(1, "CSV Storage enabled. Adding CSV listener.");
			SimulationListenerCSV listener = new SimulationListenerCSV();
			this.addStateListener(listener);
			this.getSimulationResult().setSimulationListenerCSV(listener);
		}

		if (isJSONStorageEnabled()) {
			log(1, "JSON Storage enabled. Adding JSON listener.");
			SimulationListenerJSON listener = new SimulationListenerJSON();
			this.addStateListener(listener);
			this.getSimulationResult().setSimulationListenerJSON(listener);
		}

	}
	
	/**
	 * This can also be called DURING a simulation and takes care of adding the car to necessary lists.
	 * @param car
	 * @param addArrivalEvent If true, uses car.timestampArrival to add the arrival event
	 * @param addDepartureEvent If true, uses car.timestampDeparture to add the departure event
	 */
	public void addCar(Car car, boolean addArrivalEvent, boolean addDepartureEvent) {
		if (this.dataSim.getCars().contains(car) == false) {
			log(2, "Adding new car id=" + car.getId());
			this.dataSim.getCars().add(car);
		}
		
		if (addArrivalEvent == true) {
			Event eArrival = new EventCarArrival(car.timestampArrival, car);
			addEvent(eArrival);
		}
		if (addDepartureEvent == true) {
			Event eDeparture = new EventCarDeparture(car.timestampDeparture, car);
			addEvent(eDeparture);
		}
		
	}
	
	/**
	 * This can also be called DURING a simulation and takes care of adding the charging station to necessary lists.
	 * @param chargingStation
	 */
	public void addChargingStation(ChargingStation chargingStation) {
		if (this.getDataSim().getChargingStations().contains(chargingStation) == false) {
			// Add to charging station list
			this.dataSim.getChargingStations().add(chargingStation);
			// Add to fuse tree
			final AtomicBoolean added = new AtomicBoolean(false);
			this.dataSim.getFuseTree().traverseTree(new Callback<FuseTreeNode>() {
				@Override
				public void callback(FuseTreeNode item) {
					// Add to first fuse in tree that has charging stations
					if (item instanceof Fuse && added.get() == false) {
						Fuse fuse = (Fuse) item;
						// Try to add to a fuse with any charging station children or one that has no children
						if (fuse.getDirectChargingStationChildren().size() > 0 || fuse.hasChildren() == false) {
							fuse.addChild(chargingStation);
							added.set(true);
						}
					}
				}
			});
			
			if (added.get() == false) {
				throw new RuntimeException("New charging station id=" + chargingStation.getId() + " was unable to be added to FuseTree.");
			}
			
			this.dataSim.getFuseTree().refreshListOfChargingStations();
			
			// Add to state
			state.addFreeChargingStation(chargingStation);
			
		}
		else {
			throw new RuntimeException("Attempted to add chargingStation name=" + chargingStation.getName() + " multiple times, already exists in list.");
		}
	}
	
	public void addEvent(Event event) {
		if (event instanceof EventCarFinished) {
			log(2, "Adding carFinished event for n=" 
					+ ((EventCarFinished) event).car.getId() 
					+ ", i=" + ((EventCarFinished) event).chargingStation.getId()
					+ " at t=" + event.getSecondsOfDay() + " (k=" + state.currentTimeslot + ")");
		}
		else {
			log(2, "Adding " + event.getClass().getSimpleName() + ""
					+ " at t=" + event.getSecondsOfDay() + " (k=" + TimeUtil.getTimeslotFromSeconds(event.getSecondsOfDay()) + ")");
		}
		this.allEvents.add(event);
		
		int secondsOfDay = (int) event.getSecondsOfDay();
		
		ArrayList<Event> localTimedEvents = getLocalTimedEvents(secondsOfDay);
		if (localTimedEvents == null) {
			localTimedEvents = new ArrayList<Event>();
			this.timedEvents.put(secondsOfDay, localTimedEvents);
		}
		localTimedEvents.add(event);
	}
	
	/**
	 * Gets the list of events for a given secondsOfDay
	 * @param secondsOfDay
	 * @return
	 */
	public ArrayList<Event> getLocalTimedEvents(int secondsOfDay) {
		return this.timedEvents.get(secondsOfDay);
	}
	
	
	/**
	 * Contains the simulation loop and simulates a complete day.
	 */
	public synchronized void simulate() {
		log(1, "Simulating with "
				+ "t_0=" + simStartSeconds 
				+ ", t_max=" + (simStartSeconds+simNumSeconds));
		// For loop: each iteration something can be changed (or not)
		// One iteration per new car, per car leaving,
		// and every 15 mins (new energy prices)
		// ==> one iteration per (potential) "change" 
		for (int t=state.currentTimeSeconds;
				t<simStartSeconds+simNumSeconds; //  && paused == false
				t++) {
			
			// Per iteration: Call strategy with current state
			simulateStep(t);
		}
		log(1, "Strategy took " + timeResponse.getTime() + "s.");
		
		if (strategy instanceof StrategyAlgorithmic && ((StrategyAlgorithmic) strategy).isNonlinearChargingRecognized()) {
			log(1, "Nonlinear charging planned capacity cache hits: " + ((StrategyAlgorithmic) strategy).getScheduler().plannedCapacityHashMapHits);
		}
	}
	
	/**
	 * Returns if this simulation has started or not
	 */
	public boolean hasStarted() {
		return this.state.currentTimeSeconds > simStartSeconds;
	}
	
	public boolean isFinished() {
		return this.state.currentTimeSeconds+1 // is 0-indexed, starts at 0 
				>= this.simNumSeconds; 		   // count of seconds, starts at 1
	}
	
	
	
	
	/**
	 * Represents one step in the simulation (one iteration in the for loop)
	 * Calls strategy.react
	 */
	private void simulateStep(int t) {
		ArrayList<Event> events = getLocalTimedEvents(t);
		
		//System.out.println("Simulation::simulateStep Simulating step t=" + t);
		
		state.setTimeSeconds(t);
		
		// Give strategy chance to react to events happening in this second
		if (events != null) {
			try (MethodTimer timer = new MethodTimer(this.timeResponse)) {
				for (Event event : events) {
					strategy.react(state, event);
				}
			}
		}
		
		// Call any listeners before updating
		for (SimulationListener listener : stateListeners) {
			listener.callbackBeforeUpdate(state);
		}
		
		// Update state (e.g. change car current capacities)
		// based on delta from last time step
		try (MethodTimer timer = new MethodTimer(this.getSimulationResult().timeProblemConstruction)) {
			updateState(t);
		}
		
		// Call any listeners after updating (--> cars will be more charged)
		// And to allow training to get reward for invalid state
		for (SimulationListener listener : stateListeners) {
			listener.callbackAfterUpdate(state);
		}
				
		// Check if state is still valid
		
		try (MethodTimer timer = new MethodTimer(this.getSimulationResult().timeProblemConstruction)) {
			Validation.validateState(state);
		}
		catch (ValidationException e) {
			log(2, "Current power assignments at k=" + state.currentTimeslot + ", t=" + state.currentTimeSeconds);
			Collections.sort(state.getCurrentPowerAssignments(), new Comparator<PowerAssignment>() {
				@Override
				public int compare(PowerAssignment o1, PowerAssignment o2) {
					return o1.chargingStation.getId() - o2.chargingStation.getId();
				}
			});
			
			for (PowerAssignment power : state.getCurrentPowerAssignments()) {
				log(2, power.toString());
			}
			log(0, "ValidationException message:");
			log(0, e.getMessage());
			//log(0, "Car plans:");
			//for ()
			throw new SimulationInvalidStateException("Validity error with nCars=" + dataSim.getCars().size());
		}
	}
	
	
	/**
	 * In case a more granular approach is required: instead of using simulate() call each step externally
	 */
	public void simulateNextStep() {
		if (this.isFinished() == false) {
			simulateStep(state.currentTimeSeconds);
			state.incrementTimeSeconds();
		}
	}
	
	public void updateState(int t) {
		// Update power assignments based on currentPlan of cars
		// every second
		updateStatePowerAssignmentsFromPlan();
		
		// Add car current capacities and update variablesP for result
		for (PowerAssignment powerAssignment : state.getCurrentPowerAssignments()) {
			double lastChargedCapacity = powerAssignment.car.getChargedCapacity();
			updateStateCarCapacity(t, powerAssignment);
			updateStateSimulationResult(lastChargedCapacity, powerAssignment);
		}
	}
	
	
	private void updateStatePowerAssignmentsFromPlan() {

		for (CarAssignment carAssignment : state.getCurrentCarAssignments()) {
			Car car = carAssignment.car;
			ChargingStation chargingStation = carAssignment.chargingStation;

			if (car.isFullyCharged() == false) {
				int k = state.currentTimeslot;

				// How much current will the car use at k? If nonlinear charging is active, 
				// car may decide based on exponential function (based on CV phase in CCCV)
				
				// First check if a plan exists. If no plan exists, use infrastructure/car maximums 
				double plannedCurrent = (car.getCurrentPlan() != null) ? 
						Math.max(0, car.getCurrentPlan()[k]) : // plans may be initialized at all slots with -1. This is obviously not the current to be assigned
						Math.min(car.sumUsedPhases * chargingStation.fusePhase1, car.sumUsedPhases * car.maxCurrentPerPhase);
						
				// Next use this as input for (potential) nonlinear charging, the constant current in CCCV
				double maxBatteryCurrent = (car.carBattery.getNonlinearCharging()) ?
						car.carBattery.batterySim.getCurrentBasedOnSoC(car.carBattery.getSoC(), plannedCurrent*car.sumUsedPhases) : 
						plannedCurrent * car.sumUsedPhases;

						
				// Check whether each phase is connected throughout the fuse tree
				boolean phase1ConnectedInFuseTree = chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_1); 
				boolean phase2ConnectedInFuseTree = chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_2); 
				boolean phase3ConnectedInFuseTree = chargingStation.isPhaseAtStationConnectedInFuseTree(Phase.PHASE_3); 
						
				double phase1 = phase1ConnectedInFuseTree ? car.canLoadPhase1*maxBatteryCurrent/car.sumUsedPhases : 0; 
				double phase2 = phase2ConnectedInFuseTree ? car.canLoadPhase2*maxBatteryCurrent/car.sumUsedPhases : 0; 
				double phase3 = phase3ConnectedInFuseTree ? car.canLoadPhase3*maxBatteryCurrent/car.sumUsedPhases : 0; 
				
				if (state.isCarPowerAssigned(car)) {
					PowerAssignment powerAssignment = state.getCurrentPowerAssignment(car);
					powerAssignment.setPhase1(phase1);
					powerAssignment.setPhase2(phase2);
					powerAssignment.setPhase3(phase3);
				}
				else {
					state.addPowerAssignment(car, chargingStation, phase1, phase2, phase3);
				}
			}
			
			if (car.getCurrentPlan() != null && car.isFullyCharged() == false) {
				
			}
		}
	}
	
	/**
	 * Update model: car.chargedCapacity
	 * @param t
	 * @param powerAssignment
	 * @return The startChargedCapacity of car (before updating)
	 */
	private void updateStateCarCapacity(int t, PowerAssignment powerAssignment) {
		// Add currentCapacity of cars
		Car car = powerAssignment.car;
		ChargingStation chargingStation = powerAssignment.chargingStation;
		car.addChargedCapacity(1, powerAssignment.getPhase1() + powerAssignment.getPhase2() + powerAssignment.getPhase3());
		//System.out.println("chargedCapacity: " + (car.getChargedCapacity()-startChargedCapacity));
		// If any are full, add event for car finished on next second (t+1)
		// ==> car stops charging
		if (car.isFullyCharged()) {
			LocalTime timestamp = TimeUtil.getTimestampFromSeconds(t+1);
			Event event = new EventCarFinished(timestamp, car, chargingStation);
			addEvent(event);
		}
	}
	
	/**
	 * Update simulationResult: Update variablesP by adding to average 
	 * the amount that was charged
	 */
	private void updateStateSimulationResult(double lastChargedCapacity, PowerAssignment powerAssignment) {
		// Update variablesP for export
		Car car = powerAssignment.car;
		for (int j=1;j<=3;j++) {
			int i = powerAssignment.chargingStation.getId();
			int k = state.currentTimeslot;
			
			double ampereHours;
			if (car.isFullyCharged()== false) {
				ampereHours = EnergyUtil.getAmpereHours(1, powerAssignment.getPhaseByInt(j));
			}
			else {
				double missingAmpereHours = car.getMaxCapacity() - car.getCurrentCapacity() - lastChargedCapacity;
				double phaseProportion = car.canLoadPhase(j) / car.sumUsedPhases;
				ampereHours = missingAmpereHours * phaseProportion / CONSTANTS.CHARGING_EFFICIENCY;
			}
			simulationResult.addVariableP(i, j, k, ampereHours);
			simulationResult.addToCarInputCurrents(car, k, ampereHours);
			simulationResult.addToChargingStationOutputCurrents(powerAssignment.chargingStation, k, ampereHours);
		}
	}
	
	public void addStateListener(SimulationListener listener) {
		this.stateListeners.add(listener);
	}
	
	public SimulationResult getSimulationResult() {
		return simulationResult;
	}
	
	public DataGenerator getDataSim() {
		return dataSim;
	}
	
	public int getSecondsFirstCarArrival() {
		for (Event event : getSimulationEvents()) {
			if (event instanceof EventCarArrival) {
				return event.getSecondsOfDay();
			}
		}
		return -1;
	}
	
	public List<Event> getSimulationEvents() {
		return allEvents;
	}
	public HashMap<Integer, ArrayList<Event>> getTimedSimulationEvents() {
		return timedEvents;
	}

	public State getState() {
		return state;
	}

	public Strategy getStrategy() {
		return strategy;
	}

	public boolean isJSONStorageEnabled()
	{
		return enableJSONStorage;
	}

	public void setEnableJSONStorage(boolean enableJSONStorage)
	{
		this.enableJSONStorage = enableJSONStorage;
	}

	public boolean isCSVStorageEnabled() {
		return enableCSVStorage;
	}

	public void setEnableCSVStorage(boolean enableCSVStorage) {
		this.enableCSVStorage = enableCSVStorage;
	}



	
}










