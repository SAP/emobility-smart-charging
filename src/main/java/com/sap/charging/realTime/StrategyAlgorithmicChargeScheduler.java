package com.sap.charging.realTime;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.battery.BatterySim;
import com.sap.charging.model.battery.BatterySimParameters;
import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.realTime.util.PlannedCapacityKey;
import com.sap.charging.realTime.util.TimeslotSorter;
import com.sap.charging.realTime.util.TimeslotSortingCriteria;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.Loggable;
import com.sap.charging.util.SortableElement;
import com.sap.charging.util.TimeUtil;
import com.sap.charging.util.Util;

public class StrategyAlgorithmicChargeScheduler implements Loggable {

	public int getVerbosity() {
		return Simulation.verbosity;
	}
	
	private boolean recognizeNonlinearCharging;
	private final StrategyAlgorithmic strategy;
	private boolean enablePlannedCapacityCache = false;
	
	public StrategyAlgorithmicChargeScheduler(StrategyAlgorithmic strategy) {
		this.strategy = strategy;
	}
	
	
	public void setNonlinearChargingRecognized(boolean recognizeNonlinearCharging) {
		this.recognizeNonlinearCharging = recognizeNonlinearCharging;
	}
	
	public boolean isNonlinearChargingRecognized() {
		return recognizeNonlinearCharging;
	}
	
	
	public boolean isEnablePlannedCapacityCache() {
		return enablePlannedCapacityCache;
	}


	public void setEnablePlannedCapacityCache(boolean enablePlannedCapacityCache) {
		this.enablePlannedCapacityCache = enablePlannedCapacityCache;
	}


	/**
	 * Convenience method for getPlannedCapacity with all parameters that uses car's current charge plan and until end of day
	 * @param chargingStation
	 * @param car
	 * @param currentTimeSeconds
	 * @return
	 */
	public double getPlannedCapacity(ChargingStation chargingStation, Car car, int currentTimeSeconds) {
		return getPlannedCapacity(chargingStation, car, currentTimeSeconds, currentTimeSeconds, car.getCurrentPlan().length*15*60);
	}
	
	/**
	 * getPlannedCapacity in interval from intervalSecondsStart until intervalSecondsEnd (currentTimeSeconds is needed to get SoC at intervalSecondsStart)
	 * 
	 * @param chargingStation
	 * @param car
	 * @param currentTimeSeconds
	 * @param intervalSecondsStart
	 * @param intervalSecondsEnd
	 * @return
	 */
	public double getPlannedCapacity(ChargingStation chargingStation, Car car, int currentTimeSeconds, int intervalSecondsStart, int intervalSecondsEnd) {
		if (isNonlinearChargingRecognized() == false) {
			return getPlannedCapacityLinear(car, currentTimeSeconds, intervalSecondsStart, intervalSecondsEnd);
		}
		else {
			return getPlannedCapacityNonlinear(chargingStation, car, currentTimeSeconds, intervalSecondsStart, intervalSecondsEnd); 
		}
	}
	
	
	public double getPlannedCapacityLinear(Car car, int currentTimeSeconds, int intervalSecondsStart, int intervalSecondsEnd) {
		double chargedAhUntilInterval = getPlannedCapacityLinear(car, currentTimeSeconds, intervalSecondsStart);
		double chargedAhAtEndOfInterval = getPlannedCapacityLinear(car, currentTimeSeconds, intervalSecondsEnd);
		
		return chargedAhAtEndOfInterval - chargedAhUntilInterval;
	}
	
	/**
	 * For a car, its current plan and the current timestamp, how much will be charged (Ah) until secondsEnd
	 * 
	 * Past timeslots are NOT taken into account
	 * @param currentPlan
	 * @param currentTimeSeconds will be used as basis for only taking fraction of first timeslot
	 * @return Charged Ah of plan from currentTimeSeconds to secondsEnd
	 */
	public double getPlannedCapacityLinear(Car car, int currentTimeSeconds, int secondsEnd) {
		double resultCurrent = 0;
		for (int k=0;k<car.getCurrentPlan().length;k++) {
			
			if (car.getCurrentPlan()[k] < 0) {
				// Means this timeslot has not been initialized yet, ignore it
				continue;
			}

			int timeslotStartSeconds = TimeUtil.getSecondsFromTimeslot(k);
			int timeslotEndSeconds = timeslotStartSeconds + 900;
			
			int secondsAfterTimeslotStart = currentTimeSeconds - timeslotStartSeconds;
			if (secondsAfterTimeslotStart <= 15*60 && secondsAfterTimeslotStart >= 0) {
				// If currentTimeSeconds lies in middle of timeslot=k
				// Example: CurrentTimeSeconds = 28450, timeslotStartSeconds = 27900 (k=31)
				// Here we can't charge the full 900s but only 900-550=350 seconds
				resultCurrent += 1.0*(15*60 - secondsAfterTimeslotStart)/(15.0*60.0) * car.getCurrentPlan()[k];
			}
			else if (timeslotStartSeconds >= currentTimeSeconds) {
				// Otherwise, as long as we are looking at future timeslots we can use full timeslot for charging
				resultCurrent += car.getCurrentPlan()[k];
			}
			
			if (secondsEnd <= timeslotEndSeconds) {
				// Take into account secondsEnd by subtracting proportionally from result
				// After secondsEnd we are done
				// Example: k=95 --> timeslotEnd is 86400. With secondsEnd=86400 subtract nothing
				// Example: k=95 --> timeslotEnd is 86400. With secondsEnd=85800 subtract 2/3 (because we only get to charge 1/3 with timeslotStart=85500)
				// But: currentTimeSeconds could also be > timeslotStartSeconds (start=300, end=600, in this case still subtract 1/3)
				
				int secondsBeforeTimeslotEnd = timeslotEndSeconds - secondsEnd;
				resultCurrent -= 1.0*secondsBeforeTimeslotEnd / (15*60) * car.getCurrentPlan()[k];
				break;
			}
			
		}
		// Convert from current (A) to charged energy (Ah)
		return CONSTANTS.CHARGING_EFFICIENCY * car.sumUsedPhases * resultCurrent / 4;		
	}
	
	
	
	HashMap<PlannedCapacityKey, Double> plannedCapacityHashMap = new HashMap<>();
	public int plannedCapacityHashMapHits = 0;
	private PlannedCapacityKey getCacheKey(Car car, int currentTimeSeconds, int intervalSecondsStart, int intervalSecondsEnd) {
		//return car.carBattery.getSoC() + ";" + currentTimeSeconds + ";" + intervalSecondsStart + ";" + 
		//	intervalSecondsEnd + ";" + Arrays.toString(car.getCurrentPlan());
		
		return new PlannedCapacityKey(car.carBattery.getSoC(), currentTimeSeconds, intervalSecondsStart, intervalSecondsEnd, car.getCurrentPlan());
	}
	
	private void cacheGetPlannedCapacityNonlinear(Car car, int currentTimeSeconds, 
			int intervalSecondsStart, int intervalSecondsEnd, double plannedCapacity) {
		PlannedCapacityKey key = getCacheKey(car, currentTimeSeconds, intervalSecondsStart, intervalSecondsEnd);
		//System.out.println("key=" + key + ", hashCode=" + key.hashCode());
		plannedCapacityHashMap.put(key, plannedCapacity);
	}
	/*private Double getPlannedCapacityNonlinearFromCache(Car car, int currentTimeSeconds, 
			int intervalSecondsStart, int intervalSecondsEnd) {
		String key = getCacheKey(car, currentTimeSeconds, intervalSecondsStart, intervalSecondsEnd);
		return plannedCapacityHashMap.get(key);
	}*/
	public HashMap<PlannedCapacityKey, Double> getPlannedCapacityHashMap() {
		return plannedCapacityHashMap;
	}
	
	/**
	 * Note: Does NOT use car's chargePlan, instead use chargePlan[] to test hypothetical scenario (to compare what will happen if we change one value in a plan)
	 * 
	 * Assume we start nonlinear battery sim from currentTimeSeconds=now. This has same interface to linear getPlannedCapacity
	 * 
	 * Example scenario with t0=300 and we want to know how much will be charged between t1=1800 and t2=2700 (exclusive):
	 * currentTimeSeconds = 300
	 * secondsStart = 1800
	 * secondsEnd = 2700
	 * 
	 * Difference between simulation until 1800 and continuing that simulation until 2700
	 * interval between 1800 and 2700
	 * 
	 * currentTimeSeconds can be=secondsStart
	 *  
	 * @param car
	 * @param chargePlan
	 * @return
	 */
	public double getPlannedCapacityNonlinear(ChargingStation chargingStation, Car car, int currentTimeSeconds, int intervalSecondsStart, int intervalSecondsEnd) {
		if (intervalSecondsStart < currentTimeSeconds || 
			intervalSecondsEnd < currentTimeSeconds || 
			intervalSecondsEnd < intervalSecondsStart) {
			throw new RuntimeException("currentTimeSeconds=" + currentTimeSeconds + ", intervalSecondsStart=" + intervalSecondsStart + ", intervalSecondsEnd=" + intervalSecondsEnd);
		}
		
		Double result;
		PlannedCapacityKey key = isEnablePlannedCapacityCache() ? getCacheKey(car, currentTimeSeconds, intervalSecondsStart, intervalSecondsEnd) : null;
		//if ((result = getPlannedCapacityNonlinearFromCache(car, currentTimeSeconds, intervalSecondsStart, intervalSecondsEnd)) != null) {
		if (getPlannedCapacityHashMap().containsKey(key) && isEnablePlannedCapacityCache()) {
			//System.out.println(result);
			//System.out.println("Key=" + key + " already exists");
			plannedCapacityHashMapHits++;
			result = getPlannedCapacityHashMap().get(key);
			return result;
		}
		else {
			
		}
		
		
		BatterySim sim = BatterySim.createBatterySimFromCar(car);
		double maxCurrentStatic = Math.min(car.sumUsedPhases*chargingStation.fusePhase1, car.sumUsedPhases*car.maxCurrent);
		
		// First, simulate until start of interval and record result
		sim.simulate(currentTimeSeconds, intervalSecondsStart, car, maxCurrentStatic);
		double chargedAhUntilInterval = sim.getChargedAh(); // t0 to t1
		
		// Next, simulate until end of interval and record result
		sim.simulate(intervalSecondsStart, intervalSecondsEnd, car, maxCurrentStatic);
		double chargedAhEndOfInterval = sim.getChargedAh(); // t0 to t2
		
		result = chargedAhEndOfInterval - chargedAhUntilInterval;
	
		if (isEnablePlannedCapacityCache())
			cacheGetPlannedCapacityNonlinear(car, currentTimeSeconds, intervalSecondsStart, intervalSecondsEnd, result);
		//System.out.println("Key=" + key + " : " + result + ", does not exist yet");
		
		return result;
	}
	
	
	
	
	
	/**
	 * Fills a car's charging plan up to its minSoC, using timeslots ordered by index (fills from minK onwards "greedy")
	 * @param state
	 * @param minK
	 * @param maxK
	 * @param car
	 * @param chargingStation
	 * @param currentTimeSeconds
	 */
	public void fillChargingPlanToMinSoC(State state, int minK, int maxK, Car car, ChargingStation chargingStation, int currentTimeSeconds) {
		// Fill to minimum state of charge
		List<SortableElement<Integer>> timeslotsByIndex = TimeslotSorter.getSortedTimeslots(state, minK, maxK, TimeslotSortingCriteria.INDEX); // getSortedTimeslotsByIndex(state, minK, maxK);
		double currentPlannedSum = getPlannedCapacity(chargingStation, car, currentTimeSeconds);
		double missingCapacityToMinSoC = car.getMissingCapacityToMinSoC() - currentPlannedSum;
		if (missingCapacityToMinSoC > 0) {
			// Fill greedy using min state of charge
			log(2, "Filling up to minimum SoC with desiredCapacity=" + missingCapacityToMinSoC + "Ah...");
			fillChargingPlan(car, chargingStation, missingCapacityToMinSoC, timeslotsByIndex, currentTimeSeconds);
		}
	}
	
	
	public void fillChargingPlanByCost(State state, int minK, int maxK, Car car, ChargingStation chargingStation, int currentTimeSeconds) {
		// Fill up optimally
		List<SortableElement<Integer>> timeslotsByPrice = TimeslotSorter.getSortedTimeslots(state, minK, maxK, strategy.getSortingCriteriaByObjective());  // getSortedTimeslotsByPrice(state, minK, maxK);
		double currentPlannedSum = getPlannedCapacity(chargingStation, car, currentTimeSeconds);
		double desiredCapacity = car.getMissingCapacity() - currentPlannedSum; //- Math.max(car.getCurrentCapacity()+car.getChargedCapacity(), 
												   //			car.minLoadingState);
		if (desiredCapacity > 0) {
			log(2, "Filling up rest (n=" + car.getId() + ") with missingCapacity=" + car.getMissingCapacity() + "-currentPlannedSum=" + currentPlannedSum + " ==> desiredCapacity=" + desiredCapacity + "Ah, minK=" + minK + ", maxK=expectedDepartureTimeslot=" + maxK + "...");
			// Fill greedy using min state of charge
			fillChargingPlan(car, chargingStation, desiredCapacity, timeslotsByPrice, currentTimeSeconds);
		}
	}
	
	
	public void fillChargingPlanToFull(State state, int minK, int maxK, Car car, ChargingStation chargingStation, int currentTimeSeconds) {
		// Fill to full as soon as possible
		List<SortableElement<Integer>> timeslotsByIndex = TimeslotSorter.getSortedTimeslots(state, minK, maxK, TimeslotSortingCriteria.INDEX); // getSortedTimeslotsByIndex(state, minK, maxK);
		double currentPlannedSum = getPlannedCapacity(chargingStation, car, currentTimeSeconds);
		double missingCapacity = car.getMissingCapacity() - currentPlannedSum;
		log(2, "Filling up to full SoC with desiredCapacity=" + missingCapacity + "Ah...");
		if (missingCapacity > 0) {
			fillChargingPlan(car, chargingStation, missingCapacity, timeslotsByIndex, currentTimeSeconds);
		}
	}
	
	
	/**
	 * How much current is needed to fill desired capacity (and spread over timeslot)?
	 * @param desiredCapacity Ampere-hours (Ah) desired
	 * @return
	 */
	public double getCurrentToFillTimeslot_linear(double desiredCapacity, double sumUsedPhases, int timeslotStartSeconds, int timeslotEndSeconds, double originalPlannedTimeslotCurrent) {
		double current = desiredCapacity * (3600.0/900); // 4 timeslots per hour (Ah -> A)
		current /= CONSTANTS.CHARGING_EFFICIENCY;  // During charging take into account efficiency
		
		double fractionOfTimeslot = (timeslotEndSeconds-timeslotStartSeconds) / 900.0; // It can be the case that we don't have the total timeslot to charge
		current /= fractionOfTimeslot; 
		
		double currentPerPhase = current / sumUsedPhases; // take into account (car will charge same amount on each phase, plan is per phase)
		currentPerPhase += originalPlannedTimeslotCurrent; 
	
		return currentPerPhase; 
	}
	
	
	
	/**
	 * Given a car and its charging plan, how much should the charging plan be filled by?
	 * 
	 * If nonlinear charging is enabled: Each call to this function creates one battery simulation
	 * @param car
	 * @param chargingStation
	 * @param missingPlannedCapacity
	 * @param sortedEnergyPrices
	 * @param currentPlan
	 * @param currentTimeSeconds
	 */
	public void fillChargingPlan(Car car, ChargingStation chargingStation,
			double desiredCapacity, List<SortableElement<Integer>> sortedEnergyPrices, int currentTimeSeconds) {
		
		double[] currentPlan = car.getCurrentPlan();
		BatterySim batterySim = (isNonlinearChargingRecognized()) ? BatterySim.createBatterySimFromCar(car) : null;
		
		double maxCurrentPerPhaseStatic = Math.min(chargingStation.fusePhase1, car.maxCurrentPerPhase);
		double maxCurrentStatic = car.sumUsedPhases * maxCurrentPerPhaseStatic;
		
		
		log(2, "Filling charging plan for car=" + car.getId() + " at t=" + currentTimeSeconds + " with desiredCapacity=" + desiredCapacity + "Ah..."); 
		
		// When was the start of the last timeslot? Needed for progressing nonlinear battery simulation
		// Initialize with current time so we only progress simulation by fraction of first timeslot
		int lastStartTimeSeconds = currentTimeSeconds;
		
		for (SortableElement<Integer> element : sortedEnergyPrices) {
			boolean breakAfterThis = false;
			int k = element.index;
			int timeslotStartSeconds = Math.max(currentTimeSeconds, TimeUtil.getSecondsFromTimeslot(k)); // If we call fillChargingPlan() in middle of timeslot, only use part of timeslot
			int timeslotEndSeconds = TimeUtil.getSecondsFromTimeslot(k+1);
			
			// originally planned capacity in this timeslot
			double originalPlannedTimeslotCurrent = currentPlan[k] >= 0 ? currentPlan[k] : 0;
			double originalPlannedTimeslotCapacity = getPlannedCapacity(chargingStation, car, currentTimeSeconds, timeslotStartSeconds, timeslotEndSeconds); 
			double originalPlannedCapacity = getPlannedCapacity(chargingStation, car, currentTimeSeconds, timeslotStartSeconds, Math.max(timeslotEndSeconds, car.timestampDeparture.toSecondOfDay())); 
			
			
			double maxCurrentPerPhase;
			if (isNonlinearChargingRecognized() == false) {
				maxCurrentPerPhase = maxCurrentPerPhaseStatic;
			}
			else {

				/* Progress battery simulation until start of this timeslot 
				   Ignore chargePlan in current timeslot to get max possible current at beginning of this timeslot
				   Example: By default chargePlan is filled with 0s. This should be ignored in order to get max possible current depending on SoC */
				batterySim.simulate(lastStartTimeSeconds, timeslotStartSeconds, car, maxCurrentStatic, k);
				lastStartTimeSeconds = timeslotStartSeconds;
				
				// lastCurrent works if no gap in timeslots:
				// 1) 300-300, 300-900 (ignore k=1), 900-1800 (ignore k=2)
				// 2) 300-300, 300-1800 (ignore k=2, k=1 is NOT ignored, correctly. However last current is not set)
				// Instead, use nextCurrent
				maxCurrentPerPhase = batterySim.getNextCurrent() / car.sumUsedPhases;
				
				// maxCurrentPerPhase may be greater than what is required (e.g., fill by 1Ah ==> no need to assign 32A)
				// For this simulate backwards:
				// We want to fill EXACTLY by 1Ah
				// We know z_0 and z_1 = z_0 + 1Ah/Q
				// We know i_1(z_1) (at end of interval)
				// Want to find i_0 charge plan to fit exactly 10Ah 
				// Simulate backward from t=t_0+900 until rest can be filled with linear approach
				BatterySimParameters paramsSimBackwards = batterySim.getBatterySimParameters().copy();
				//log(3, "desiredCapacity=" + desiredCapacity + "Ah while planning k=" + k + ", currentPlan[" + k + "]="  + currentPlan[k]);
				paramsSimBackwards.initialSoC = batterySim.getSoC() + desiredCapacity / batterySim.getBatterySimParameters().capacity; // z_1 = z_0+Ah/Q
				if (paramsSimBackwards.initialSoC > 1 
					// && paramsSimBackwards.initialSoC < 1+1e-2
					) { // Sometimes desiredCapacity is a tiny bit too high
					paramsSimBackwards.initialSoC = 1;
				}
				
				if (paramsSimBackwards.initialSoC > 1) {
					System.out.println("Params for sim backwards:");
					System.out.println(paramsSimBackwards.toString());
					System.out.println("desiredCapacity: " + desiredCapacity + "Ah");
				}
				
				BatterySim simBackwards = new BatterySim(paramsSimBackwards, false, true);
				
				double capacitySoFarNonlinear = 0;
				double maxCurrentPerPhaseSimBackwards = maxCurrentPerPhase;
				for (int step=900;step>0;step--) {
					simBackwards.simulatePreviousStep(maxCurrentStatic);
					
					if (simBackwards.getSoC() < 0) {
						break;
					}
					
					double nextCurrent = simBackwards.getNextCurrent(); //simBackwards.getCurrentBasedOnSoC(simBackwards.getSoC(), maxCurrentStatic);
					
					// How much capacity (Ah) would we get: Convert Ampere in remaining time in this timeslot to Ah
					double capacityRestWithLinear = nextCurrent * step / 3600.0 * CONSTANTS.CHARGING_EFFICIENCY;
					
					// If we can fill rest of the capacity with linear in order to exactly reach desiredCapacity, stop taking max current here and instead take linear current
					log(4, "step=" + step + ", nextCurrent=" + nextCurrent + "A, capacitySoFarNonlinear=" + capacitySoFarNonlinear + "Ah, capacityRestWithLinear=" + capacityRestWithLinear + "Ah");	
					if (capacityRestWithLinear + capacitySoFarNonlinear > desiredCapacity + originalPlannedTimeslotCapacity) {
						// Don't use nextCurrent (since this might be way too much), instead recalculate exact amount
						
						double maxLinearCurrent = (desiredCapacity-capacitySoFarNonlinear) * (3600.0/900); // Convert from Ah in 900s (A/(1/4H)) to A: How much A do I need to charge with to reach Ah in 15 minutes?
						maxLinearCurrent = maxLinearCurrent / CONSTANTS.CHARGING_EFFICIENCY; // efficiency
						maxLinearCurrent = maxLinearCurrent / (step/900.0); // maybe we are in middle of timeslot

						maxCurrentPerPhaseSimBackwards = maxLinearCurrent / car.sumUsedPhases; 
						log(3, "Stopping with maxCurrentPerPhaseSimBackwards=" + maxCurrentPerPhaseSimBackwards + "A  at step=" + step + 
							   " with initialSoC=" + paramsSimBackwards.initialSoC + ", soc(" + step + ")=" + simBackwards.getSoC() + 
							   ", capacityRestWithLinear=" + capacityRestWithLinear + "Ah, capacitySoFarNonLinear=" + capacitySoFarNonlinear + 
							   "Ah, originalPlannedCapacity=" + originalPlannedTimeslotCapacity + "Ah");
						breakAfterThis = true;
						break;
					}
					
					// How much have we charged so far in the nonlinear part?
					capacitySoFarNonlinear = - simBackwards.getChargedAh();
					
				}
				
				maxCurrentPerPhase = Math.min(maxCurrentPerPhase, maxCurrentPerPhaseSimBackwards);
				
				
				// Problem: Linear approximation can lead to later timeslots charging less than what was previously doable
				// Example: [32, 0, 32, 0.00, 32]A plan -> [20.39Ah, 0, 18.19Ah, 0.000Ah, 11.77Ah]
				// 			[32, 0, 32, 1.13, 18]A plan -> [20.39Ah, 0, 18.19Ah, 0.722Ah, 10.91Ah] 
				// This means we need to exactly fill charging plan by small amounts of desired Capacity
				
				// Linear approximation: (this works well at low initial SoC during CC phase but not during CV phase)
				
				
			}
			
			if (desiredCapacity > 1e-8 && currentPlan[k] != maxCurrentPerPhase) {
			
				// Fill up current plan with maximum possible amount							
				if (isNonlinearChargingRecognized() == false) {
					// If linear charging: This statement fills up the last slot (dont use max current but spread it out over 15 mins)
					currentPlan[k] = Math.min(maxCurrentPerPhase, this.getCurrentToFillTimeslot_linear(desiredCapacity, car.sumUsedPhases, timeslotStartSeconds, timeslotEndSeconds, originalPlannedTimeslotCurrent)); 
				}
				else {
					// In nonlinear charging, the "filling up" is done during simulation
					currentPlan[k] = maxCurrentPerPhase;
				}
				
				
				// Check if this would lead to charging below min power
				if (currentPlan[k] < car.minCurrentPerPhase) {
					log(3, "Current=" + currentPlan[k] + "A is less than car.minCurrentPerPhase=" + car.minCurrentPerPhase + ", setting to minCurrentPerPhase."); 
					currentPlan[k] = car.minCurrentPerPhase; // Note: This is not optimal. 
				}
				
				// After update of chargePlan, how many chargedAh did we gain?
				// DesiredcapacityChange: All later timeslots must also be taken into account as changes here may affect timeslots later on
				//double newPlannedTimeslotCapacity = getPlannedCapacity(chargingStation, car, currentTimeSeconds, timeslotStartSeconds, timeslotEndSeconds);
				double newPlannedCapacity = getPlannedCapacity(chargingStation, car, currentTimeSeconds, timeslotStartSeconds,  Math.max(timeslotEndSeconds, car.timestampDeparture.toSecondOfDay()));
				double desiredCapacityChange = newPlannedCapacity - originalPlannedCapacity;
				
				
				log(3, "Filling n=" + car.getId() + ", k=" + k + " with " + 
						currentPlan[k] + 
						"A (previously=" + originalPlannedTimeslotCurrent + "A," + originalPlannedTimeslotCapacity + "Ah). " +
						"desiredCapacityChange=" + desiredCapacityChange + "Ah" + 
						", remaining to be planned: desiredCapacity=" + (desiredCapacity-desiredCapacityChange));
				
				/*if (desiredCapacityChange < 0) {
					//log(0, Arrays.toString(car.getCurrentPlan()));
					throw new RuntimeException("OriginalPlannedTimeslotCurrent=" + originalPlannedTimeslotCurrent + "A, originalPlannedTimeslotCapacity=" + originalPlannedTimeslotCapacity + "Ah" +
							", new currentPlan[" + k + "]=" + currentPlan[k] + "A, newPlannedTimeslotCapacity=" + newPlannedTimeslotCapacity + ", desiredCapacityChange=" + desiredCapacityChange);
				}*/
				if (desiredCapacityChange == 0 && isNonlinearChargingRecognized()) {
					// Exponential function at some point (e.g., 1e-15) is practically 0
					breakAfterThis = true;
				}
				
				// Update how many Ah are missing from desiredCapacity
				desiredCapacity -= desiredCapacityChange;
				
				
				if (breakAfterThis)
					break;
			} 
			else if (desiredCapacity <= 1e-8) { break; }
		}
		log(2, "After filling: desiredCapacity=" + Util.formatDouble(desiredCapacity) + 
				"Ah, plannedCapacity=" + Util.formatDouble(getPlannedCapacity(chargingStation, car, currentTimeSeconds)) + "Ah");
		log(2, "currentPlan=" + Arrays.toString(currentPlan));
	}
	
	
}
