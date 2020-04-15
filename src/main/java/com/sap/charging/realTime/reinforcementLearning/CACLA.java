package com.sap.charging.realTime.reinforcementLearning;

import com.sap.charging.dataGeneration.DataGeneratorReal;
import com.sap.charging.model.Car;
import com.sap.charging.realTime.State;
import com.sap.charging.realTime.Strategy;
import com.sap.charging.realTime.model.CarAssignment;
import com.sap.charging.realTime.model.PowerAssignment;
import com.sap.charging.realTime.reinforcementLearning.exploration.ExplorationEpsilonGreedyRandom;
import com.sap.charging.realTime.reinforcementLearning.model.Action;
import com.sap.charging.realTime.reinforcementLearning.model.Agent;
import com.sap.charging.realTime.reinforcementLearning.model.Policy;
import com.sap.charging.realTime.reinforcementLearning.neuralNetwork.Actor;
import com.sap.charging.realTime.reinforcementLearning.neuralNetwork.Critic;
import com.sap.charging.realTime.reinforcementLearning.neuralNetwork.util.CLI;
import com.sap.charging.sim.Simulation;
import com.sap.charging.sim.eval.Validation;
import com.sap.charging.sim.eval.exception.SimulationInvalidStateException;
import com.sap.charging.sim.util.SimulationListener;
import com.sap.charging.util.Loggable;

public class CACLA implements SimulationListener, Loggable {
	
	public static int verbosity = 2;
	public int getVerbosity() {
		return verbosity;
	}
	
	/**
	 * Highly negative number
	 */
	private static final double rewardInvalidSimulation = -1000000;

	/**
	 * Discount factor gamma
	 */
	public double discountFactor;
	
	/**
	 * Agent
	 */
	private final Agent agent;
	
	/**
	 * Psi (used for policy)
	 */
	private Actor actor;
	
	/**
	 * Timestep of reinforcement learning, not of simulation
	 */
	private int timestep = 0;
	
	/**
	 * Theta (used for value approximation)
	 */
	private Critic critic;
	
	
	private Simulation simulation;

	/**
	 * Used as V(s) for value approximation of critic
	 */
	private double[] currentStateValueApproximation;
	private double[] previousChargedCapacity;
	
	/**
	 * Used as "a" in step 9)
	 */
	private Action[] currentChosenAction;
	
	public CACLA() {
		
		//Policy.verbosity = 0;
		Policy policy = new Policy(new ExplorationEpsilonGreedyRandom());
		agent = new Agent(policy);
	}
	
	private void initSimulation() {
		// Could choose random date?
		// 2016-12-03 --> 1 cars
		// 2017-11-26 --> 7 cars
		
		DataGeneratorReal data = new DataGeneratorReal("2017-11-26", 0, true, true);
		data.setIdealCars(true);
		data.generateAll();
		
		for (Car car : data.getCars()) {
			log(1, car.toString());
		}
		
		currentChosenAction = new Action[data.getCars().size()];
		previousChargedCapacity = new double[data.getCars().size()];
		for (int n=0;n<data.getCars().size();n++) {
			previousChargedCapacity[n] = data.getCars().get(n).getChargedCapacity();
		}
		currentStateValueApproximation = new double[data.getCars().size()];
		
		Strategy strategy = new StrategyAssignment();
		Simulation.verbosity = 0;
		simulation = new Simulation(data, strategy);
		simulation.init();
		simulation.addStateListener(this);
	}
	private void simulate() {
		simulation.simulate();
	}
	
	// 1) Inputs
	public void setInputs(double discountFactor
			//, List<State> initialStateDistribution
			) {
		this.discountFactor = discountFactor;
		log(1, "Setting discountFactor gamma=" + discountFactor);
		//this.initialStateDistribution = initialStateDistribution;
	}
	
	// 2) Initialize
	public void init() {
		actor = new Actor();
		critic = new Critic();
		//initialState = this.initialStateDistribution.get(0);
	}
	
	// 3) Repeat until done
	public void startTraining(int nSimulations) {
		//int nSimulations = 1;
		for (int simIteration=0;simIteration<nSimulations;simIteration++) {
			log(1, "In simIteration=" + simIteration + "...");
			this.initSimulation();
			try {
				this.simulate();
			}
			catch (SimulationInvalidStateException e) { /* Ignore */ }
		}
	}
	
	// Print stats, allow manual input testing
	public void finishTraining() {
		log(1, "Critic model:");
		critic.printModel();
		log(1, "------------------------------------------------------------------------", false);
		log(1, "Actor model:");
		actor.printModel();
		CLI cli = new CLI();
		cli.listen(actor, critic);
	}
	
	public void incrementTimestep() {
		timestep++;
		if (timestep % 1000 == 0)
			log(2, "timestep=" + timestep);
	}
	
	// Contents of 3): Repeat until done training
	@Override
	public void callbackBeforeUpdate(State state) {
		// This will reset to an initial state if simulation terminates
		// Perform this for each car
		for (int i=0;i<state.getCurrentCarAssignments().size();i++) {
			CarAssignment carAssignment = state.getCurrentCarAssignments().get(i);
			Car car = carAssignment.car;
			
			if (state.isCarPowerAssigned(car) == false) {
				state.addPowerAssignment(car, carAssignment.chargingStation, 
						0, 0, 0);
			}
			
			PowerAssignment powerAssignment = state.getCurrentPowerAssignment(car);
			
			// 4) Choose action
			currentChosenAction[car.getId()] = agent.getPolicy().chooseAction(powerAssignment, actor, timestep);
			
			// Need value approximation of current state for 6: V(s)
			currentStateValueApproximation[car.getId()] = critic.getValueApproximation(powerAssignment);
			
			// 5) Perform action
			powerAssignment.setPhase1(currentChosenAction[car.getId()].phase1);
			powerAssignment.setPhase2(currentChosenAction[car.getId()].phase2);
			powerAssignment.setPhase3(currentChosenAction[car.getId()].phase3);
			
			if (Validation.isStateValid(state, 0)==false) {
				currentChosenAction[car.getId()].setInvalid();
				break;
			}
		}
	}
	
	// Contents of 3), after update
	@Override
	public void callbackAfterUpdate(State statePrime) {
		
		for (int i=0;i<statePrime.getCurrentCarAssignments().size();i++) {
			Car car = statePrime.getCurrentCarAssignments().get(i).car;
			PowerAssignment powerAssignment = statePrime.getCurrentPowerAssignment(car);
			if (timestep % 1000 == 0) log(1, "timestep=" + timestep + ", car n=" + car.getId() + "; car.getMissingCapacity()=" + car.getMissingCapacity() + ", p.phase1=" + powerAssignment.getPhase1());
			
			// Called after performing action 
			// 5) Observe reward r, new state s'
			double reward;
			if (currentChosenAction[car.getId()].isValid()) {
				/*double maxPossibleReward = EnergyUtil.getAmpereHours(1, 
						CONSTANTS.CHARGING_EFFICIENCY*car.sumUsedPhases*Math.min(car.maxCurrentPerPhase, 
								powerAssignment.chargingStation.fusePhase1));
				
				log(2, "Max possible reward: " + 1000*maxPossibleReward);*/
				reward = 1000*(car.getChargedCapacity() - this.previousChargedCapacity[car.getId()]);
				log(2, "Applying reward r=" + reward +
						" (chargedCapacity=" + car.getChargedCapacity() +
						", previousChargedCapacity=" + this.previousChargedCapacity[car.getId()] + "...");
				this.previousChargedCapacity[car.getId()] = car.getChargedCapacity();
				//Util.sleep(1000);	
			}
			else {
				reward = rewardInvalidSimulation;
				log(1, "Applying reward r=" + reward + " (INVALID STATE) at timestep=" + timestep + "...");
				//Util.sleep(5000);	
			}
			
			// 6) Calculate delta (TD-error)
			// Calculate V(s')
			double valueApproximationStateNext = critic.getValueApproximation(powerAssignment);
			// Calculate V_(t+1) = r + gamma*V(s')
			double valueTarget = reward + discountFactor*valueApproximationStateNext;
			// Calculate delta
			double tdError = valueTarget - currentStateValueApproximation[car.getId()];
			
			log(2, "delta = r + gamma*V(s') - V(s)");
			log(2, tdError + 
					" = " + reward + 
					" + " + discountFactor + 
					" * " + valueApproximationStateNext + 
					" - " + currentStateValueApproximation);
			
			if (car.isFullyCharged()) {
				System.out.println("UPDATING WITH FULLY CHARGED CAR, tdError=" + tdError);
			}
			// 7) Always update critic (based on value target, NOT delta)
			critic.update(powerAssignment, valueTarget);
			
			// Andere schreibweise: if V_(t+1) > V(s) 
			if (tdError > 0) { // 8) Only update actor if good action was chosen
				actor.update(powerAssignment, currentChosenAction[car.getId()]); // 9) Perform update
			}
			log(2, "-----------------------------------t=" + timestep + "--------------------------------------------", false);
			
			if (currentChosenAction[car.getId()].isValid()==false) {
				break;
			}
			
		}
		
		// 10) If s' is terminal
		// 11) Start again
		if (statePrime.getCurrentCarAssignments().size() > 0) 
			incrementTimestep();
	}

	public static void main(String[] args) {
		//Policy.verbosity = 0;
		//NeuralNetwork.verbosity = 0;
		//Validation.verbosity = 0;
		CACLA.verbosity = 1;
		CACLA cacla = new CACLA();
		double discountFactor = 0;
		cacla.setInputs(discountFactor);
		cacla.init();
		cacla.startTraining(10);
		cacla.finishTraining();
	}
	
	

}	
