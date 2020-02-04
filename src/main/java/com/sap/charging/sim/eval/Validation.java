package com.sap.charging.sim.eval;

import java.util.HashMap;
import java.util.List;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.model.Fuse;
import com.sap.charging.model.FuseTreeNode;
import com.sap.charging.realTime.State;
import com.sap.charging.realTime.model.Assignment;
import com.sap.charging.realTime.model.CarAssignment;
import com.sap.charging.realTime.model.PowerAssignment;
import com.sap.charging.sim.Simulation;
import com.sap.charging.sim.eval.exception.CarAssignmentException;
import com.sap.charging.sim.eval.exception.FuseTreeException;
import com.sap.charging.sim.eval.exception.PowerAssignmentException;
import com.sap.charging.sim.eval.exception.ValidationException;

public class Validation {

	/**
	 * 2: Print everything
	 * 1: Print unique things (e.g. constructor, start simulation)
	 * 0: Print nothing
	 */
	public static int getVerbosity() {
		return Simulation.verbosity;
	}
	/**
	 * Message will only be logged if minVerbosity >= verbosity
	 * @param message
	 * @param minVerbosity
	 */
	public static void log(int minVerbosity, String message) {
		if (getVerbosity() >= minVerbosity) 
			System.out.println(message);
	}
	
	/**
	 * R1: Don't overcharge car
	 * R2: Power may only be > 0 if there is a car assigned to it
	 * R3: Don't overload fuses of charging stations
	 * R7: Ratio 1st to 2nd phase
	 * R8: Max power of car
	 * R9: Min power of car
	 * 
	 * 
	 * @param state
	 * @return
	 * @throws PowerAssignmentException 
	 */
	public static void validateCarPowerAssignments(List<PowerAssignment> powerAssignments) throws PowerAssignmentException {
		for (PowerAssignment p : powerAssignments) {
			Car car = p.car;
			ChargingStation chargingStation = p.chargingStation;
			
			/*if (car.isFullyCharged() && (p.phase1>0 || p.phase2>0 f|| p.phase3>0)) { // R1
				log(1, "Validation::validateCarPowerAssignments R1 (don't overcharge car) has been broken.");
				log(1, "Validation::validateCarPowerAssignments car.isFullyCharged()=" + car.isFullyCharged() +
						", p.phase1=" + p.phase1 + 
						", p.phase2=" + p.phase2 + ", p.phase3=" + p.phase3);
				return false;
			}*/
			
			String message = null;
			if (p.phase1 > chargingStation.fusePhase1 + 1E-6 || // R3
				p.phase2 > chargingStation.fusePhase2 + 1E-6 || 
				p.phase3 > chargingStation.fusePhase3 + 1E-6) {
				message = "Validation::validateCarPowerAssignments R3 (don't overload fuses) has been broken. \n" +
									"Validation::validateCarPowerAssignments p.phase1=" + p.phase1 + ", p.phase2=" + p.phase2 +
									", p.phase3=" + p.phase3 + 
									"; chargingStation.fusePhase1=" + chargingStation.fusePhase1 + 
									", chargingStation.fusePhase2=" + chargingStation.fusePhase2 + 
									", chargingStation.fusePhase3=" + chargingStation.fusePhase3; 
				
				log(1, message);
			}
			if (p.phase1 > car.canLoadPhase1*car.maxCurrentPerPhase + 1E-6 || // R8
				p.phase2 > car.canLoadPhase2*car.maxCurrentPerPhase + 1E-6 || 
				p.phase3 > car.canLoadPhase3*car.maxCurrentPerPhase + 1E-6) {
				message = "Validation::validateCarPowerAssignments R8 (max car power) has been broken. \n" + 
						"Validation::validateCarPowerAssignments car=n" + car.getId() + " car.maxCurrentPerPhase=" + car.maxCurrentPerPhase + 
						", assigned current p.phase1=" + p.phase1;
				log(1, message);
			}
			if ((p.phase1 > 0 && p.phase1 < car.canLoadPhase1*car.minCurrentPerPhase - 1E-6) || // R9
				(p.phase2 > 0 && p.phase2 < car.canLoadPhase2*car.minCurrentPerPhase - 1E-6) || 
				(p.phase3 > 0 && p.phase3 < car.canLoadPhase3*car.minCurrentPerPhase - 1E-6)) {
				message = "Validation::validateCarPowerAssignments R9 (min car power) has been broken. \n" + 
						"Validation::validateCarPowerAssignments car=n" + car.getId() + " car.minCurrentPerPhase=" + car.minCurrentPerPhase + 
						", assigned current p.phase1=" + p.phase1 + ", soc=" + car.carBattery.getSoC() + ", car=" + car.toString();
				log(1, message);
			}
			if (car.canLoadPhase2*p.phase1 != car.canLoadPhase1*p.phase2 || // R6	
				car.canLoadPhase3*p.phase1 != car.canLoadPhase1*p.phase3) { // R7
				message = "Validation::validateCarPowerAssignments R6/R7 (phase ratios) has been broken.";
				log(1, message);
			}
			
			if (message != null) {
				throw new PowerAssignmentException(message);
			}
		}
	}
	
	
	
	
	/**
	 * Returns a boolean whether a fuse tree in the current state is valid
	 * @param state
	 * @return
	 */
	public static boolean isFuseTreeValid(State state) {
		try {
			validateFuseTree(state);
			return true;
		} catch (FuseTreeException e) {
			log(2, e.getMessage());
			return false;
		}
	}
	
	/**
	 * Validates a complete state with respect to the fuse tree (at current timestamp). Convience method for validateFuse(Fuse fuse, State state).
	 * @param state
	 * @return
	 */
	public static void validateFuseTree(State state) throws FuseTreeException {
		validateFuse(state.fuseTree.getRootFuse(), state);
	}
	
	/**
	 * Instead of checking the complete state, only check a given fuse. If this is the root fuse, the complete fuse is checked (at current timestamp).
	 * @param fuse
	 * @param timeslotToCheck
	 * @throws FuseTreeException
	 */
	public static void validateFuse(Fuse fuse, State state) throws FuseTreeException {
		checkSummedChildConsumptionAtTimeslot(fuse, state, -1);
	}
	
	
	
	
	/**
	 * Returns a boolean whether a fuse tree in the current state is valid at a given timeslot (using car's schedule to check).
	 * @param state
	 * @return
	 */
	public static boolean isFuseValidAtTimeslot(Fuse fuse, State state, int timeslotToCheck) {
		try {
			checkSummedChildConsumptionAtTimeslot(fuse, state, timeslotToCheck);
			return true;
		} catch (FuseTreeException e) {
			log(2, e.getMessage());
			return false;
		}
	}
	
	/**
	 * Validates the fuse tree for a given timeslot, using each car's scheduled plan. 
	 * Convinience method for validateFuseAtTimeslot(fuse, state, timeslot)
	 * @param state
	 * @param timeslotToCheck
	 * @return
	 * @throws FuseTreeException 
	 */
	public static void validateFuseTreeAtTimeslot(State state, int timeslotToCheck) throws FuseTreeException {
		validateFuseAtTimeslot(state.fuseTree.getRootFuse(), state, timeslotToCheck);
	}
	
	public static void validateFuseAtTimeslot(FuseTreeNode fuse, State state, int timeslotToCheck) throws FuseTreeException {
		checkSummedChildConsumptionAtTimeslot(fuse, state, timeslotToCheck);
	}
	
	
	
	/**
	 * If timeslotToCheck is below 0, the current timestamp is used (i.e. powerAssignment). If it is above 0, the car's plan is used. 
	 * @param chargingStation
	 * @param state
	 * @param timeslotToCheck
	 * @return
	 */
	public static double[] getCurrentPerGridPhase(ChargingStation chargingStation, State state, int timeslotToCheck) {
		Assignment assignment = getAssignment(chargingStation, state, timeslotToCheck);
		return (assignment != null) ? 
				assignment.getCurrentPerGridPhase(timeslotToCheck) :
				null;
	}

	/**
	 * Returns either the current car assignment (timeslot >= 0) or the current power assignment (timeslot < 0)
	 * @param chargingStation
	 * @param state
	 * @param timeslotToCheck
	 * @return
	 */
	private static Assignment getAssignment(ChargingStation chargingStation, State state, int timeslotToCheck) {
		Assignment assignment;
		if (timeslotToCheck >= 0) {
			assignment = state.getCurrentCarAssignment(chargingStation);
		}
		else {
			assignment = state.getCurrentPowerAssignment(chargingStation);
		}
		return assignment;
	}
	
	public static int checkSummedChildConsumption = 0;
	public static int checkSummedChildConsumptionAtTimeslot = 0;
	public static void checkSummedChildConsumptionAtTimeslot(FuseTreeNode rootItem, State state, 
			int timeslotToCheck) throws FuseTreeException {
		
		/*if (timeslotToCheck > 0) 
			checkSummedChildConsumptionAtTimeslot++;
		else 
			checkSummedChildConsumption++;
		*/
		
		HashMap<FuseTreeNode, double[]> fuseConsumptionMap = new HashMap<>();
		
		for (ChargingStation chargingStation : state.fuseTree.getListOfChargingStations()) {
			double[] consumption = getCurrentPerGridPhase(chargingStation, state, timeslotToCheck);
			// Should also check charging station itself, not just fuses
			if (consumption != null && isFuseUsageValid(chargingStation, consumption) == false) {
				throw new FuseTreeException(chargingStation, consumption, timeslotToCheck); 
			}
			updateFuseParentsConsumption(chargingStation, fuseConsumptionMap, consumption);
		}
		//log(3, "Consumption at k=" + timeslotToCheck + ": " + Arrays.toString(fuseConsumptionMap.get(rootItem)));
		validateFuseUsage(fuseConsumptionMap, timeslotToCheck);
	}
	
	/**
	 * Bottom up, update each parent's consumption
	 * @param item
	 * @param fuseConsumptionMap
	 * @param consumption
	 */
	public static void updateFuseParentsConsumption(FuseTreeNode item, 
			HashMap<FuseTreeNode,double[]> fuseConsumptionMap, double[] consumption) {
		if (consumption == null)
			return;
		
		// Add consumption to all parents
		FuseTreeNode parent = item.getParent();
		while (parent != null) {
			double[] parentConsumption = fuseConsumptionMap.get(parent);
			if (parentConsumption == null){
				parentConsumption = new double[3];
				fuseConsumptionMap.put(parent, parentConsumption);
			}
			parentConsumption[0] += consumption[0];
			parentConsumption[1] += consumption[1];
			parentConsumption[2] += consumption[2];
			
			parent = parent.getParent();
		}
	}
	
	/**
	 * Validates a fuse consumption map to check whether any are being overused
	 * @param fuseConsumptionMap
	 * @param timeslotToCheck
	 * @throws FuseTreeException
	 */
	public static void validateFuseUsage(HashMap<FuseTreeNode,double[]> fuseConsumptionMap, int timeslotToCheck) throws FuseTreeException {
		for (FuseTreeNode fuseToCheck : fuseConsumptionMap.keySet()) {
			double[] consumption = fuseConsumptionMap.get(fuseToCheck);
			if (consumption != null) {
				if (isFuseUsageValid(fuseToCheck, consumption) == false) {
					throw new FuseTreeException((Fuse) fuseToCheck, consumption, timeslotToCheck);
				}
			}
		}
	}
	
	/**
	 * Checks whether a given fuse is being validly used
	 */
	public static boolean isFuseUsageValid(FuseTreeNode fuse, double[] currentPerPhase) {
		return (currentPerPhase[0] <= fuse.getFusePhase(Phase.PHASE_1) + 1e-6 && 
				currentPerPhase[1] <= fuse.getFusePhase(Phase.PHASE_2) + 1e-6 && 
				currentPerPhase[2] <= fuse.getFusePhase(Phase.PHASE_3) + 1e-6);
	}
	
	
	/**
	 * R4,R5: Per car, a maximum of one charging station may be assigned. 
	 * Conversely per charging station, a maximum of one 
	 * car may be assigned. (in other words, a car may only charge
	 * at one charging station)
	 * @param currentState
	 * @return
	 */
	public static void validateCarAssignments(List<CarAssignment> carAssignments) throws CarAssignmentException {
		for (int i=0;i<carAssignments.size();i++) {
			for (int j=0;j<carAssignments.size();j++) {
				if (i==j) 
					continue;
				
				CarAssignment a1 = carAssignments.get(i);
				CarAssignment a2 = carAssignments.get(j);
				
				if (a1.car.equals(a2.car)) {
					String message = "Validation::validateCarAssignments Car n=" + a1.car.getId() + " is already assigned.";
					log(1, message);
					throw new CarAssignmentException(message);
				}
				if (a1.chargingStation.equals(a2.chargingStation)) {
					String message = "Validation::validateCarAssignments ChargingStation i=" + 
							a1.chargingStation.getId() + " is already assigned.";
					log(1, message);
					throw new CarAssignmentException(message);
				}
			}
		}
	}
	
	
	public static boolean isStateValid(State state) {
		try {
			validateState(state);
			return true;
		}
		catch (ValidationException e) {
			return false;
		}
	}
	
	/**
	 * 
	 * @param state
	 * @return
	 */
	public static void validateState(State state) throws ValidationException {
		validateFuseTree(state);
		validateCarAssignments(state.getCurrentCarAssignments());
		validateCarPowerAssignments(state.getCurrentPowerAssignments());
	}

	public static void validateState(State state, int tempVerbosity) throws ValidationException {
		int previousVerbosity = Simulation.verbosity;
		Simulation.verbosity = tempVerbosity;
		validateState(state);
		Simulation.verbosity = previousVerbosity;
	}
	
	
	public static boolean isStateValid(State state, int tempVerbosity) {
		try {
			validateState(state, tempVerbosity);
			return true;
		}
		catch (ValidationException e) {
			return false;
		}
	}
	
}
