package com.sap.charging.realTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.model.Fuse;
import com.sap.charging.model.FuseTreeNode;
import com.sap.charging.model.battery.BatterySim;
import com.sap.charging.opt.lp.Objective;
import com.sap.charging.opt.lp.Variable;
import com.sap.charging.opt.solution.model.DayaheadSchedule;
import com.sap.charging.realTime.model.CarAssignment;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureForecast;
import com.sap.charging.realTime.util.TimeslotSorter;
import com.sap.charging.realTime.util.TimeslotSortingCriteria;
import com.sap.charging.sim.eval.Validation;
import com.sap.charging.sim.eval.exception.FuseTreeException;
import com.sap.charging.util.SortableElement;
import com.sap.charging.util.TimeUtil;
import com.sap.charging.util.Util;

public class StrategyAlgorithmic extends Strategy {
	
	private final CarDepartureForecast carDepartureForecast;
	private final DayaheadSchedule schedule;
	private boolean recognizeNonlinearCharging = false; // Do we assume cars charge linearly (always with max current) or CCCV (exponential falloff at high SoC)? 
	private boolean reoptimizeOnStillAvailableAfterExpectedDepartureTimeslot = true; // for legacy code: previously, on last timestamp if currentTimeslot >= timeslotDeparture, car was reoptimized one last time
	
	// For legacy code: Previously, if a car is rescheduled, we assign 0A to the violated timeslot instead of the maximum possible amount
	// Example: 32A assigned and we are 14A over allowed. Reduce by 14A instead of by 32A
	// Default setting true, to be backwards compatible
	private boolean rescheduleCarsWith0A = true;  
	
	private final StrategyAlgorithmicChargeScheduler scheduler;
	
	public StrategyAlgorithmic() {
		this(CarDepartureForecast.getDefaultCarDepartureForecast(), null);
	}
	
	public StrategyAlgorithmic(CarDepartureForecast carDepartureForecast) {
		this(carDepartureForecast, null);
	}
	
	public StrategyAlgorithmic(DayaheadSchedule schedule) {
		this(CarDepartureForecast.getDefaultCarDepartureForecast(), schedule);
	}
	
	public StrategyAlgorithmic(CarDepartureForecast carDepartureForecast, DayaheadSchedule schedule) {
		this.carDepartureForecast = carDepartureForecast;
		this.schedule = schedule;
		this.scheduler = new StrategyAlgorithmicChargeScheduler(this);
	}
	
	
	
	/****************************
	 * Prioritization methods: How should EVs be compared and prioritized?
	 * - Fair share
	 * - Load imbalance: After assigning schedules, who creates the largest load imbalance? Via I_N (Current on Neutralleiter) Computationally this is awkward...
	 ****************************/
	
	/**
	 * Objective for prioritizing fair share. Used in priority function. Timeslots will be sorted by index
	 */
	public final Objective objectiveFairShare = new Objective(0);
	
	/**
	 * Minimizes peaks, creates a smooth aggregated energy curve. Conflicts with energy price minimization
	 */
	public final Objective objectivePeakShaving = new Objective(0);
	
	/**
	 * Minimizes energy costs. Conflicts with peak shaving
	 */
	public final Objective objectiveEnergyCosts = new Objective(1); 
	
	/**
	 * Objective for prioritizing load imbalance (see http://www.electrician2.com/electa1/electa4htm.html). Used in priority function
	 */
	public final Objective objectiveLoadImbalance = new Objective(0);
	
	
	private Objective getHighestObjective() {
		Objective highestObjective = null;
		double highestWeight = -1;
		if (highestWeight < objectiveFairShare.getWeight()) {
			highestWeight = objectiveFairShare.getWeight();
			highestObjective = objectiveFairShare;
		}
		if (highestWeight < objectivePeakShaving.getWeight()) {
			highestWeight = objectivePeakShaving.getWeight();
			highestObjective = objectivePeakShaving;
		}
		if (highestWeight < objectiveEnergyCosts.getWeight()) {
			highestWeight = objectiveEnergyCosts.getWeight();
			highestObjective = objectiveEnergyCosts;
		}
		if (highestWeight < objectiveLoadImbalance.getWeight()) {
			highestWeight = objectiveLoadImbalance.getWeight();
			highestObjective = objectiveLoadImbalance;
		}
		return highestObjective;
	}
	
	public TimeslotSortingCriteria getSortingCriteriaByObjective() {
		Objective highestObjective = getHighestObjective();
		if (highestObjective == objectivePeakShaving) 
			return TimeslotSortingCriteria.PEAK_DEMAND;
		if (highestObjective == objectiveEnergyCosts) 
			return TimeslotSortingCriteria.PRICE;
		if (highestObjective == objectiveFairShare || highestObjective == null) {
			return TimeslotSortingCriteria.INDEX;
		}
		
		throw new RuntimeException("Recheck objective weights");
	}
	
	
	
	public boolean isNonlinearChargingRecognized() {
		return recognizeNonlinearCharging;
	}
	
	/**
	 * If true: Recognizes nonlinear charging and uses nonlinear battery simulation (with CCCV as charging algorithm) 
	 * Also disables energy prices (always sort timeslots by index). 
	 * @param recognizeNonlinearCharging
	 */
	public void setRecognizeNonlinearCharging(boolean recognizeNonlinearCharging) {
		this.recognizeNonlinearCharging = recognizeNonlinearCharging;
		this.scheduler.setNonlinearChargingRecognized(recognizeNonlinearCharging);
		if (recognizeNonlinearCharging == true) {
			this.objectiveEnergyCosts.setWeight(-100);
			this.objectivePeakShaving.setWeight(-100);
			this.objectiveLoadImbalance.setWeight(-100);
		}
	}
	
	public boolean reoptimizeOnStillAvailableAfterExpectedDepartureTimeslot() {
		return reoptimizeOnStillAvailableAfterExpectedDepartureTimeslot;
	}

	public void setReoptimizeOnStillAvailableAfterExpectedDepartureTimeslot(
			boolean reoptimizeOnStillAvailableAfterExpectedDepartureTimeslot) {
		this.reoptimizeOnStillAvailableAfterExpectedDepartureTimeslot = reoptimizeOnStillAvailableAfterExpectedDepartureTimeslot;
	}

	public boolean rescheduleCarsWith0A() {
		return rescheduleCarsWith0A;
	}

	public void setRescheduleCarsWith0A(boolean rescheduleCarsWith0A) {
		this.rescheduleCarsWith0A = rescheduleCarsWith0A;
	}

	public StrategyAlgorithmicChargeScheduler getScheduler() {
		return this.scheduler;
	}
	
	
	/****************************
	 * Filling up methods: How should charging schedules be filled for those EVs that are prioritized?
	 * - Energy costs
	 * - Minimize changes to load imbalance per timeslot
	 ****************************/
	
	
	@Override
	public void reactCarArrival(State state, Car car) {
		if (state.isAnyChargingStationFree() == false && state.isCarCurrentlyAssigned(car) == false) {
			log(1, "No free charging station for car id=" + car.getId()) ;
			return;
		}
		
		int currentTimeSeconds = state.currentTimeSeconds;
		int currentK = TimeUtil.getTimeslotFromSeconds(currentTimeSeconds);
		
		
		// Check if car is allowed to park (only relevant if dayahead schedule is not null)
		// If variableX exists: Always
		// If variableX does not exist: Only allowed to park if there is space (planned cars have priority!)
		Variable variableX = null;
		if (schedule != null) {
			log(1, "Checking schedule for car id=" + car.getId() + "...", true, false);
			variableX = schedule.getVariableX(car.getId());
			if (variableX != null) {
				log(1, "Found variableX=" + variableX, false, true);
			}
			else {
				// Check if there is space. If not enough space (planned cars reserve every charging station), EV may not charge
				int allowedLatenessSeconds = 1*15*60; // Allow one timeslot (900s) of lateness. After that the spot will not be reserved anymore
				
				int nCarsPlanned = schedule.getReservedSpotsAtTimeslot(state, allowedLatenessSeconds);
				if (nCarsPlanned >= state.nChargingStations) {
					log(1, "Did not find precomputed schedule in day-ahead plan. car id=" + car.getId() + " may not charge, nCarsPlanned=" + nCarsPlanned + " (those in dayahead schedule) >= nChargingStations=" + state.nChargingStations, false, true);
					return;
				}
				else {
					log(1, "Did not find precomputed schedule in day-ahead plan. car id=" + car.getId() + " may charge, nCarsPlanned=" + nCarsPlanned + " (those in dayahead schedule) < nChargingStations=" + state.nChargingStations, false, true);
				}
			}
			
		}
		
		ChargingStation chargingStation; 
		CarAssignment carAssignment; 
		if (state.isCarCurrentlyAssigned(car) == true) {
			carAssignment = state.getCurrentCarAssignment(car); 
			chargingStation = carAssignment.chargingStation;
		}
		else {
			chargingStation = state.getFirstFreeChargingStation();
			carAssignment = state.addCarAssignment(car, chargingStation);
		}
		
		
		double[] currentPlan;
		
		if (variableX != null && variableX.getValue() == 1) {
			// EV is in plan
			carAssignment.setExpectedDepartureTimeSeconds(car.timestampDeparture.toSecondOfDay());
			int originalChargingStationId = variableX.getIndex("i");
			
			int plannedEVArrival = TimeUtil.getTimestampFromTimeslot(car.getFirstAvailableTimeslot()).toSecondOfDay();
			if (currentTimeSeconds <= plannedEVArrival) {
				// EV is early, apply original plan
				currentPlan = schedule.getChargingStationPlan(originalChargingStationId);
				car.setCurrentPlan(currentPlan);
				int expectedDepartureTimeslot = car.getLastAvailableTimeslot();
				
				// Check whether currentPlan is still enough to fill up EV (SoC has changed)
				/*final double remainingPlannedSum = getPlannedCapacity(car, currentTimeSeconds);
				final double chargingNeeds = car.getMissingCapacity();
				if (chargingNeeds > remainingPlannedSum) {
					double desiredCapacity = chargingNeeds - remainingPlannedSum;
					List<SortableElement<Integer>> timeslotsByPrice = getSortedTimeslotsByPrice(state, currentK, maxK);
					fillChargingPlan(car, chargingStation, 
							desiredCapacity, timeslotsByPrice, currentTimeSeconds);
				}*/
				
				scheduler.fillChargingPlanByCost(state, currentK, expectedDepartureTimeslot, car, chargingStation, currentTimeSeconds);
				
				log(2, "car n=" + car.getId() + " is early! Applying currentPlan:");
				log(2, "currentPlan=" + Arrays.toString(currentPlan));
			}
			else {
				// EV is late, fill up with cheapest available slot (where not planned)
				currentPlan = schedule.getChargingStationPlan(originalChargingStationId);
				car.setCurrentPlan(currentPlan);
				int latenessSeconds = currentTimeSeconds - plannedEVArrival;
				log(2, "car n=" + car.getId() + " is late by " + latenessSeconds + "s! originalPlannedCapacity=" + scheduler.getPlannedCapacity(chargingStation, car, currentTimeSeconds));
				log(2, "originalPlan=" + Arrays.toString(currentPlan));
				
				int expectedDepartureTimeslot = car.getLastAvailableTimeslot();
				
				// Correct original plan to reflect lateness
				for (int k=0; k<currentK;k++) {
					//int kSeconds = TimeUtil.getTimestampFromTimeslot(k).toSecondOfDay();
					//int diffSeconds = currentTimeSeconds - kSeconds;
					if (k < currentK) {
						currentPlan[k] = 0;
					}
				}
				log(2, "correctedPlan=" + Arrays.toString(currentPlan));
				
				
				scheduler.fillChargingPlanByCost(state, currentK, expectedDepartureTimeslot, car, chargingStation, currentTimeSeconds);
				
				/*// Calculate how much additional charging this car needs due to being late
				final double currentPlannedSum = getPlannedCapacity(car, currentTimeSeconds);
				double desiredCapacity = car.getMissingCapacity() - currentPlannedSum;
				log(2, "car n=" + car.getId() +  ": currentPlannedSum=" + currentPlannedSum + 
						", desiredCapacity=" + desiredCapacity);
				
				// Fill up with cheapest available slot (where not planned)
				// until charging needs are satisfied OR no available slots
				List<SortableElement<Integer>> timeslotsByPrice = getSortedTimeslotsByPrice(state, currentK, maxK);
				fillChargingPlan(car, chargingStation, 
						desiredCapacity, timeslotsByPrice, currentTimeSeconds);*/
			}
		}
		else {
			// EV is not in plan
			log(2, "Unplanned EV arrival n=" + car.getId() + ", car.curCapacity=" + car.getCurrentCapacity());
			currentPlan = new double[state.energyPriceHistory.getNTimeslots()];
			
			if (isNonlinearChargingRecognized()) {
				// Initialize plan with "null" values (-1) to indicate no value has yet been assigned to each slot
				//Arrays.fill(currentPlan, -1);
			}
			
			
			car.setCurrentPlan(currentPlan);
			
			// Get the timeslot in which the car is expected to leave
			// Use fraction for simulations that are not 96 timeslots long
			//int expectedDepartureTimeslot = (int) (carDepartureForecast.getExpectedDepartureTimeslot(state, car) *
			//		state.energyPriceHistory.getNTimeslots()*1.0 /96.0); 
			
			int expectedDepartureTimeSeconds = carDepartureForecast.getExpectedDepartureTimeSeconds(state, car);
			int expectedDepartureTimeslot = TimeUtil.getTimeslotFromSeconds(expectedDepartureTimeSeconds);
			if (currentK >= expectedDepartureTimeslot || expectedDepartureTimeslot > state.energyPriceHistory.getNTimeslots()-1) {
				expectedDepartureTimeSeconds = state.energyPriceHistory.getNTimeslots() * 15 * 60 - 1;
				expectedDepartureTimeslot = state.energyPriceHistory.getNTimeslots()-1;
			}
			carAssignment.setExpectedDepartureTimeSeconds(expectedDepartureTimeSeconds);
			
			
			scheduler.fillChargingPlanToMinSoC(state, currentK, state.energyPriceHistory.getNTimeslots(), car, chargingStation, currentTimeSeconds);
			scheduler.fillChargingPlanByCost(state, currentK, expectedDepartureTimeslot, car, chargingStation, currentTimeSeconds);
		}
		
		// Validity check for cars below minCurrent
		for (int k=0;k<currentPlan.length;k++) {
			if (currentPlan[k] < car.minCurrentPerPhase && currentPlan[k] > 0) {
				log(2, "car n=" + car.getId() + " is below minCurrent with currentPlan[" + k + "]=" + currentPlan[k] + ". Correcting to minCurrent=" + car.minCurrentPerPhase);
				currentPlan[k] = car.minCurrentPerPhase; // TODO: This is not optimal
			}
		}
		
		// Validity check for pre-fuses
		// Which timeslots are violated?
		Map<Integer, FuseTreeException> violatingTimeslots = getInitialFuseViolations(state, car);
		resolveViolations(state, violatingTimeslots);
		
	}
	
	
	
	
	/**
	 * 
	 * @param carAssignments
	 * @param currentTimeSeconds
	 * @param timeslot Which timeslot is the priority calculated for? Important for nonlinear charging as maxCurrent will be different later on
	 * @return
	 */
	public List<SortableElement<CarAssignment>> getSortedCarsByPriority(List<CarAssignment> carAssignments, int currentTimeSeconds, int timeslot) {
		List<SortableElement<CarAssignment>> sortedCars = new ArrayList<>();
		for (CarAssignment carAssignment : carAssignments) {
			int departureTimeSeconds = carAssignment.getExpectedDepartureTimeSeconds();
			double priority = getChargingPriority(carAssignment, currentTimeSeconds, departureTimeSeconds);
			sortedCars.add(new SortableElement<CarAssignment>(carAssignment, priority));
		}
		Collections.sort(sortedCars);
		return sortedCars;
	}
	
	/**
	 * Calls getSortedCarsByPriority(List<CarAssignment> carAssignments, int currentTimeSeconds, int timeslot) with the timeslot=currentTimeslot
	 * @param carAssignments
	 * @param currentTimeSeconds
	 * @return
	 */
	public List<SortableElement<CarAssignment>> getSortedCarsByPriority(List<CarAssignment> carAssignments, int currentTimeSeconds) {
		return this.getSortedCarsByPriority(carAssignments, currentTimeSeconds, TimeUtil.getTimeslotFromSeconds(currentTimeSeconds)); 
	}

	
	
	
	
	/**
	 * If nonlinear charging is not enabled: return min{ chargingStation.fuse, car.maxCurrentPerPhase }
	 * 
	 * If nonlinear charging is enabled: 
	 * Given current plan, what will current i(t) be at time=t seconds (in future)? 
	 * This question is asked at time=t_0 (currentTimeSeconds. 
	 * As initial SoC z(t_0) will be used.
	 * 
	 * To answer this question the nonlinear battery simulation will be simulated for t-t_0 steps. 
	 * 
	 * @param chargingStation
	 * @param car
	 * @param currentTimeSeconds: t_0 
	 * @param timeSeconds: t
	 * @return
	 */
	/*public double getMaxCurrentPerPhasePossible(ChargingStation chargingStation, Car car, int currentTimeSeconds, int timeSeconds) {

		
		if (isNonlinearChargingRecognized()) {

			
			double current = Double.MAX_VALUE;
			
			

			double currentPerPhase = current / car.sumUsedPhases;
			log(2, "After simulating from currentTimeSeconds=" + currentTimeSeconds + " to timeSeconds=" + timeSeconds + "(maxtime=" + maxtime + ") soc=" + soc + " and current=" + current + " with params=" + params);
			return currentPerPhase;
			
		}
		else {
			return Math.min(chargingStation.fusePhase1, car.maxCurrentPerPhase);
		}
	}*/
	
	
	
	
	

	/**
	 * Prioritizes cars according to how flexible they are. Flexibility takes into account
	 * maxCurrent (in total), the current capacity and the remaining time.
	 * <br>
	 * Cars not at Min SoC are prioritized above those above Min SoC (==> positive priority)
	 * Cars above Min SoC (==> negative priority)
	 * <br>
	 * The absolute remaining time is used, i.e. if the car is longer there than planned, delta t would be negative
	 * 
	 * 
	 * <br>
	 * Higher numeric result = higher priority, i.e. should be charged
	 * @param car
	 * @param currentTimeSeconds
	 * @param departureTimeSeconds
	 * @return
	 */
	public double getChargingPriority(CarAssignment carAssignment, int currentTimeSeconds, int departureTimeSeconds) {
		Car car = carAssignment.car;
		double missingCapacityToMinSoC = car.getMissingCapacityToMinSoC();
		
		double maxCurrentPerPhase = Math.min(car.maxCurrentPerPhase, carAssignment.chargingStation.fusePhase1);
		double maxCurrent = car.sumUsedPhases * maxCurrentPerPhase;
		
		// use absolute number here for a system of prioritization where the closer the urgency is to 0 the higher the priority is
		int urgencyTimeSeconds = Math.abs(departureTimeSeconds - currentTimeSeconds);
		double priority;
		
		if (missingCapacityToMinSoC > 0) {
			// Case 1: Car is below minSoC
			priority = missingCapacityToMinSoC*1.0 / ((1.0 * urgencyTimeSeconds * maxCurrent)+1e-8);	
		}
		else {
			// Case 2: Car is above minSoC. In order to keep the same system of priority
			// (charging needs divided by remaining time) 
			// and a negative growth the closer the EV is to the max SoC:
			// Subtract a large number
			double missingCapacityToMaxSoC = car.getMissingCapacity();
			priority = missingCapacityToMaxSoC*1.0 / ((1.0 * urgencyTimeSeconds * maxCurrent)+1e-8) - 10000;
		}
		
		return priority;
	}
	
	/**
	 * Checks the state for any violations to the fuse tree, for each timeslot k.
	 * @param state
	 * @param newCar
	 * @return
	 */
	public Map<Integer, FuseTreeException> getInitialFuseViolations(State state, Car newCar) {
		Map<Integer, FuseTreeException> violatingTimeslots = new TreeMap<>();
		for (int k=state.currentTimeslot;k<state.energyPriceHistory.getNTimeslots();k++) { 
			try {
				Validation.validateFuseTreeAtTimeslot(state,  k);
			}
			catch (FuseTreeException e) {
				if (newCar != null) {
					log(2, e.getFuse() + " planned for k=" + k + " by " + Util.formatDoubleArray(e.getSumConsumed()) + " while planning car=n" + newCar.getId());
				}
				violatingTimeslots.put(k, e);
			}
		}
		return violatingTimeslots;
	}
	
	
	
	/**
	 * Resolve all fuse violations. 
	 * @param state
	 * @param violatingTimeslots
	 */
	public void resolveViolations(State state, Map<Integer, FuseTreeException> violatingTimeslots) {
		boolean[] blockedTimeslots = new boolean[state.energyPriceHistory.getNTimeslots()];
		boolean blockedAny = false;
		
		while (violatingTimeslots.keySet().isEmpty() == false) { // While there are any violations left while planning this car
			// Handle one violation (at earliest timeslot)
			int violatingK = (int) violatingTimeslots.keySet().toArray()[0];
			blockedTimeslots[violatingK] = true;
			blockedAny = true;
			log(2, "Blocking timeslot k=" + violatingK);
			handleViolation(state, violatingK, blockedTimeslots, violatingTimeslots);
		}
		
		if (blockedAny == true) {
			log(2, "Blocked timeslots after resolving violations: ", true, false);
			for (int k=0;k<blockedTimeslots.length;k++) {
				if (blockedTimeslots[k] == true)
					log(2, k + ", ", false, false);
			}
			log(2, "", false, true);
		}
	}
	
	/**
	 * Handle a single violation
	 * @param state
	 * @param violatingK
	 * @param blockedTimeslots
	 * @param violatingTimeslots
	 */
	public void handleViolation(State state, int violatingK, boolean[] blockedTimeslots, Map<Integer, FuseTreeException> violatingTimeslots) {
		// Get all car assignments relevant to this violation
		FuseTreeException fuseTreeException = violatingTimeslots.get(violatingK);
		if (fuseTreeException == null) {
			throw new RuntimeException("No fuseTreeException found for violatingK=" + violatingK + ". Map of violatingTimeslots includes " + violatingTimeslots.size() + " items"); 
		}
		
		FuseTreeNode violatedFuse = fuseTreeException.getFuse();
		List<CarAssignment> carAssignments = state.getCarAssignmentsByFuse(violatedFuse);
		List<SortableElement<CarAssignment>> sortedViolatingCars = getSortedCarsByPriority(carAssignments, state.currentTimeSeconds, violatingK);
		
		boolean thisFuseViolationFixed = false; // Has the violation for THIS fuse been fixed?
		while (thisFuseViolationFixed == false) {
			
			log(2, "Handling violation at timeslot k=" + violatingK + " with exception=" + fuseTreeException.getMessage());
			
			// Make changes to charging plan for car with lowest priority
			if (sortedViolatingCars.size() == 0) {
				throw new RuntimeException("Rescheduled all n=" + state.cars.size() + " cars at k=" + violatingK + " but violation is not fixed!");
			}
			int indexViolatingCar = 0;
			// Loop through the list of cars until a car is found, which can reduce the power of the fuse with the highest delta
			for (int indexCurrentCar=0; indexCurrentCar<sortedViolatingCars.size();indexCurrentCar++) {
				CarAssignment carAssignmentLowestPriority = sortedViolatingCars.get(indexCurrentCar).index;
				// Check consumption on correct phase --> Check whether EV with lowest priority causes exception
				// Differentiate between charging station exceptions and fuse exceptions:
				// On fuses use currentPerGridPhase
				// On chargingStation use currentPerStationPhase (so that it matches the correct fuseSize)
				double[] consumption = fuseTreeException.getFuse() instanceof Fuse ? 
						carAssignmentLowestPriority.getCurrentPerGridPhase(violatingK) :
						carAssignmentLowestPriority.getCurrentPerStationPhase(violatingK); 
						
				double plannedCurrent = consumption[fuseTreeException.getPhaseWithHighestDelta().asInt()-1];
				
				log(2, "Car n=" + carAssignmentLowestPriority.car.getId() + " has lowest priority (" 
						+ sortedViolatingCars.get(0).value 
						+ ") in violatingK=" + violatingK + ", plannedCurrent=" + Util.formatDouble(plannedCurrent) + "A");
				
				if (plannedCurrent > 0) {
					
					rescheduleCar(state, carAssignmentLowestPriority, blockedTimeslots, violatingK, fuseTreeException);
					
					// Check if this violation was fixed: FuseTreeException e must be updated
					try {
						Validation.validateFuseAtTimeslot(violatedFuse, state, violatingK);
						thisFuseViolationFixed = true;
					}
					catch (FuseTreeException updatedException) {
						log(2, "New fuseTreeException: " + updatedException.getMessage());
						if (updatedException.getFuse() != violatedFuse) {
							log(3, "New fuseTreeException describes a different fuse (index=" + updatedException.getFuse().getId() + ") " +
								"compared to the resolved fuseTreeException (index=" + violatedFuse.getId() + ")");
							thisFuseViolationFixed = true; 
						}
						else {
							fuseTreeException = updatedException;
						}
					}
					// Relevant Car was rescheduled. Break the loop and check if the violation was fixed
					indexViolatingCar = indexCurrentCar;
					break;
				}
			}
			sortedViolatingCars.remove(indexViolatingCar); // Remove the car which was rescheduled	
		}
		// Remove violation from map
		violatingTimeslots.remove(violatingK);
		
		log(2, "Checking for new violations after rescheduling violatingK=" + violatingK + "...");		
		// Check for any new violations and add them
		for (int kCheck=state.currentTimeslot;kCheck<state.energyPriceHistory.getNTimeslots();kCheck++) {
			if (//violatingTimeslots.get(kCheck) != null ||   // check a timeslot even if there is no violation already saved for it, since data of the violation may have changed
				(violatingK != kCheck && blockedTimeslots[kCheck] == true)) { 
				// If the timeslot is blocked, don't check it. However, the current violatingK could have created a new exception so it must be rechecked
				continue;
			}
				
			try {
				Validation.validateFuseTreeAtTimeslot(state, kCheck);
			} catch (FuseTreeException eNewError) {
				violatingTimeslots.put(eNewError.timeslot, eNewError);
			}
		}
	}
	
	
	/**
	 * Reschedules a car according to blocked timeslots at a single timeslot
	 * 
	 * Example: Car is supposed to charge 16A at k=30
	 * ChargePlan[30] = 0
	 * 15 minutes of 16A (4Ah) will be rescheduled to charge somewhere else
	 */
	public void rescheduleCar(State state, CarAssignment carAssignment, boolean[] blockedTimeslots, int violatingK, FuseTreeException fuseTreeException) {
		Car car = carAssignment.car;
		ChargingStation chargingStation = carAssignment.chargingStation;
		
		TimeslotSortingCriteria sortingCriteria;
		if (car.getMissingCapacityToMinSoC() > 0) {
			// Sort by index, car is under min SoC
			sortingCriteria = TimeslotSortingCriteria.INDEX;
		}
		else {
			// Sort by price, car is over min SoC
			sortingCriteria = getSortingCriteriaByObjective();
		}
		
		// Reschedule from now until end of day
		List<SortableElement<Integer>> sortedTimeslots = TimeslotSorter.getSortedTimeslots(state, state.currentTimeslot, 96, sortingCriteria, blockedTimeslots); 
		double[] planToChange = carAssignment.car.getCurrentPlan();
		double capacityWithPlan = scheduler.getPlannedCapacity(chargingStation, car, state.currentTimeSeconds);
		double originalPlannedCurrent = planToChange[violatingK];
		
		if (rescheduleCarsWith0A) {
			// Legacy / backwards compatible behaviour: Cars are rescheduled by assigning 0A to the violated timeslot instead of the maximum possible
			planToChange[violatingK] = 0;
		}
		else {
			// Example: 
			// EV (32A, 0A, 0A)
			// Station with  2,3,1 phase rotation --> EV is charging on phase 2
			// Fuse (18A, 18A, 18A) 
			// --> 32A assigned, 14A over violated timeslot on phase 2. Should assign 18A
			
			// Which phase leads to the EV's charge plan to be reduced?
			Phase phaseHighestDelta = fuseTreeException.getPhaseWithHighestDelta(); 
			
	        // Amount over the top, the 14A (to resolve the violation)
	        // If the difference is negative (i.e. fuse won't be broken), no need to reduce  
	        double amountToReduceBy = Math.max(0, fuseTreeException.getDeltaByPhase(phaseHighestDelta)); 
	        // Possibly car is planned with less than 14A
	        double amountToReducePlanBy = Math.min(planToChange[violatingK], amountToReduceBy); 
	        // Reduce car plan by 14A (or less, if it was only planned with less)
	        planToChange[violatingK] = planToChange[violatingK] - amountToReducePlanBy;
	        // Check minCurrent validity
	        if (planToChange[violatingK] < car.minCurrentPerPhase) {
	            planToChange[violatingK] = 0;
	        }
	        log(2, "Rescheduling car=" + car.getId() + " at violatingK=" + violatingK + ": Reducing originalCurrent=" + Util.formatDouble(originalPlannedCurrent) +
	        	   "A by amountToReducePlanBy=" + Util.formatDouble(amountToReducePlanBy) + "A to " + Util.formatDouble(planToChange[violatingK]) + 
	        	   "A, phaseHighestDelta=" + phaseHighestDelta + 
	        	   ", consumption=" + fuseTreeException.getSumConsumedByPhase(phaseHighestDelta) + 
	        	   "A, fuse=" + fuseTreeException.getFuse().getFusePhase(phaseHighestDelta) + "A");
	    }
		
		
		// Trial: In order to enable reducing later timeslots, complete plan needs to be filled onward from currentK
		if (isNonlinearChargingRecognized()) {
			for (int k=state.currentTimeslot; k<=car.getLastAvailableTimeslot();k++) {
			//for (int k=violatingK; k<=car.getLastAvailableTimeslot();k++) {
					if (k != violatingK && // don't reset violatingK: this was just adapted
						blockedTimeslots[k] == false) // don't reset those slots that are blocked, because we are unable to refill those after here
					{
						planToChange[k] = 0;
					}
			}
		}
		
		// After changing plan (=rescheduling EVs): What is the planned capacity?
		double capacityWithoutPlan = scheduler.getPlannedCapacity(chargingStation, car, state.currentTimeSeconds);
		
		// Now we must fill plan by the difference
		double desiredCapacity = capacityWithPlan - capacityWithoutPlan;
		
		log(2, "Rescheduling car=" + car.getId() + " at violatingK=" + violatingK + ": capacityWithPlan=" + Util.formatDouble(capacityWithPlan) + 
				"Ah, capacityWithoutPlan=" + Util.formatDouble(capacityWithoutPlan) + "Ah, originalCurrentPlan[" + violatingK + "]=" + originalPlannedCurrent + "A, newCurrentPlan[" + violatingK + "]=" +
				car.getCurrentPlan()[violatingK] + "A at t=" + state.currentTimeSeconds + " with assignment.expectedDepartureTimeslot=" + carAssignment.getExpectedDepartureTimeslot());
		
		scheduler.fillChargingPlan(carAssignment.car, carAssignment.chargingStation, desiredCapacity, sortedTimeslots, state.currentTimeSeconds);
		
	}
	
	
	
	
	@Override
	public void reactCarDeparture(State state, Car carLeaving) {
		//log(2, "Reacting to car n=" + car.getId() + " leaving, meaning flexibilities may have opened up");
		
		int currentK = state.currentTimeslot;
		int currentTimeSeconds = state.currentTimeSeconds;
		//log(2, "Reacting to unexpected stays at currentK=" + currentK + "(currentK >= expectedDepartureTimeslots)");
		for (CarAssignment carAssignment : state.getCurrentCarAssignments()) {
			Car car = carAssignment.car;
			ChargingStation chargingStation = carAssignment.chargingStation;
		
			// CurrentTimeslot can be anything
			// Reoptimize those cars that are not planned to be fully charged
			
			int maxK = (carAssignment.getExpectedDepartureTimeslot() >= currentK) ? 
						carAssignment.getExpectedDepartureTimeslot() :
						state.energyPriceHistory.getNTimeslots();
			
			scheduler.fillChargingPlanToMinSoC(state, currentK, maxK, car, chargingStation, currentTimeSeconds);
			scheduler.fillChargingPlanByCost(state, currentK, maxK, car, chargingStation, currentTimeSeconds);
		}
		
		// Resolve all newly created violations
		Map<Integer, FuseTreeException> violatingTimeslots = getInitialFuseViolations(state, null);
		resolveViolations(state, violatingTimeslots);
	}

	
	@Override
	public void reactCarFinished(State state, Car carFinished) {
		
	}
	
	@Override
	public void reactEnergyPriceChange(State state, EnergyPriceHistory newEnergyPriceHistory) {
		// This is relevant due to inaccuracy in expectedDepartureTimeslot. 
		// Potentially, cars are not fully charged but still there
		int currentK = state.currentTimeslot;
		//log(2, "Reacting to unexpected stays at currentK=" + currentK + "(currentK >= expectedDepartureTimeslots)");
		for (CarAssignment carAssignment : state.getCurrentCarAssignments()) {
			Car car = carAssignment.car;
			ChargingStation chargingStation = carAssignment.chargingStation;
			
			/*log(2, "Car n=" + car.getId() + " is currently charged with " + 
					Util.formatDouble(car.getCurrentCapacity()) + "Ah+" + 
					Util.formatDouble(car.getChargedCapacity()) + "Ah of " + 
					Util.formatDouble(car.maxCapacity) + "Ah");*/
			
			// Only reoptimize when unexpectedly still here
			if (currentK >= carAssignment.getExpectedDepartureTimeslot() && 
				car.isFullyCharged() == false &&
				state.currentTimeSeconds % 900 == 0 && // we are at beginning of timeslot (backwards compatible results since in newer versions this function may be called more often than every 900s
				reoptimizeOnStillAvailableAfterExpectedDepartureTimeslot() // backwards compatible results (default true): With expectedDepartureTimeslot=actualDepartureTimeslot, car is reoptimized if not full yet
			     ) {
				
				log(2, "Car n=" + car.getId() + " is unexpectedly still here and not fully charged! Lacking " +
						Util.formatDouble(car.getMissingCapacity()) + "Ah");
				
				scheduler.fillChargingPlanToFull(state, currentK, state.energyPriceHistory.getNTimeslots(), car, chargingStation, state.currentTimeSeconds);
			}
			
			// Always reoptimize
			/*if (car.isFullyCharged() == false) {
				scheduler.fillChargingPlanToFull(state, currentK, state.energyPriceHistory.getNTimeslots(), car, chargingStation, state.currentTimeSeconds);
			}*/
			
			// Always reoptimize for nonlinear processes (frees up more infrastructure capacity):
			// If we don't fill up plans: Will only reduce plannedCapacity
			// Note: If reoptimization is done only once every timeslot there will be no difference (since we use maxCurrent from beginning of timeslot during planning)
			if (isNonlinearChargingRecognized()) {
				double currentPlanValue = car.getCurrentPlan()[currentK] * car.sumUsedPhases;

				double maxCurrentPerPhaseStatic = Math.min(chargingStation.fusePhase1, car.maxCurrentPerPhase);
				double maxCurrentStatic = car.sumUsedPhases * maxCurrentPerPhaseStatic;
				BatterySim sim = BatterySim.createBatterySimFromCar(car);
				double maxCurrentPossible = sim.getCurrentBasedOnSoC(sim.getSoC(), maxCurrentStatic);
				
				//System.out.println("currentPlanValue: " + currentPlanValue);
				//System.out.println("maxCurrentPossible: " + maxCurrentPossible);
				if (maxCurrentPossible < currentPlanValue) {
					car.getCurrentPlan()[currentK] = maxCurrentPossible / car.sumUsedPhases;
				}
			}
		}
		Map<Integer, FuseTreeException> violatingTimeslots = getInitialFuseViolations(state, null);
		resolveViolations(state, violatingTimeslots);
	}

	@Override
	public void reactReoptimize(State state) {
		this.reoptimize(state);
	}
	
	private void reoptimize(State state) {
		log(1, "Reoptimizing " + state.getCurrentCarAssignments().size() + " current car assignments (nonlinear batteries recognized=" + this.isNonlinearChargingRecognized() + ")..."); 
		log(2, "Fuse tree: " + state.fuseTree.toString()); 
		int currentK = state.currentTimeslot;
		for (CarAssignment carAssignment : state.getCurrentCarAssignments()) {
			Car car = carAssignment.car;
			ChargingStation chargingStation = carAssignment.chargingStation;
			
			// Check if there was no forecasting done
			if (carAssignment.getExpectedDepartureTimeSeconds() == -1) {
				int expectedDepartureTimeSeconds = carDepartureForecast.getExpectedDepartureTimeSeconds(state, car);
				carAssignment.setExpectedDepartureTimeSeconds(expectedDepartureTimeSeconds);
				log(2, "No expected departure time passed in, setting to expectedDepartureTimeSeconds=" + expectedDepartureTimeSeconds); 
			}
			
			// Always reoptimize
			if (car.isFullyCharged() == false) {
				scheduler.fillChargingPlanToMinSoC(state, currentK, state.energyPriceHistory.getNTimeslots(), car, chargingStation, state.currentTimeSeconds);
				scheduler.fillChargingPlanByCost(state, currentK, state.energyPriceHistory.getNTimeslots(), car, chargingStation, state.currentTimeSeconds);
			}
			
			// Always reoptimize for nonlinear processes (frees up more infrastructure capacity):
			// If we don't fill up plans: Will only reduce plannedCapacity
			// Note: If reoptimization is done only once every timeslot there will be no difference (since we use maxCurrent from beginning of timeslot during planning)
			if (isNonlinearChargingRecognized()) {
				double currentPlanValue = car.getCurrentPlan()[currentK] * car.sumUsedPhases;

				double maxCurrentPerPhaseStatic = Math.min(chargingStation.fusePhase1, car.maxCurrentPerPhase);
				double maxCurrentStatic = car.sumUsedPhases * maxCurrentPerPhaseStatic;
				BatterySim sim = BatterySim.createBatterySimFromCar(car);
				double maxCurrentPossible = sim.getCurrentBasedOnSoC(sim.getSoC(), maxCurrentStatic);
				
				if (maxCurrentPossible < currentPlanValue) {
					car.getCurrentPlan()[currentK] = maxCurrentPossible / car.sumUsedPhases;
				}
			}
		}
		Map<Integer, FuseTreeException> violatingTimeslots = getInitialFuseViolations(state, null);
		resolveViolations(state, violatingTimeslots);
	}
	
	
	@Override
	public String getMethod() {
		return schedule == null ? 
				getMethodWithoutScheduleStatic() :
				getMethodWithScheduleStatic();
	}
	
	public static String getMethodWithoutScheduleStatic() {
		return "realTimeAlgorithmic";
	}
	public static String getMethodWithScheduleStatic() {
		return "realTimeAlgorithmicFromDayahead";
	}
	
	public DayaheadSchedule getSchedule() {
		return schedule;
	}

	public CarDepartureForecast getCarDepartureForecast() {
		return this.carDepartureForecast;
	}
	
}
