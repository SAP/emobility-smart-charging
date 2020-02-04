package com.sap.charging.opt.heuristics;

import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.FuseTree;
import com.sap.charging.opt.heuristics.util.CarAssignmentPriority;
import com.sap.charging.opt.util.MethodTimer;
import com.sap.charging.util.JSONKeys;

public class InstanceHeuristicGreedy extends InstanceHeuristic {

	public InstanceHeuristicGreedy(List<Car> cars, List<ChargingStation> chargingStations,
			EnergyPriceHistory energyPriceHistory, FuseTree fuseTree) {
		super(cars, chargingStations, energyPriceHistory, fuseTree);
	}

	public InstanceHeuristicGreedy(DataGenerator dataGenerator) {
		this(dataGenerator.getCars(),
			 dataGenerator.getChargingStations(),
			 dataGenerator.getEnergyPriceHistory(),
			 dataGenerator.getFuseTree());
	}

	@SuppressWarnings("unchecked")
	private JSONObject getSolution() {
		// Assign a car to a charging station if one is free, else ignore it
		// If one is free: Maximum amps allowed by fuse/car loading
		JSONObject solution = new JSONObject();
		JSONArray variables = new JSONArray();
		solution.put(JSONKeys.JSON_KEY_VARIABLES, variables);
		
		boolean[][] chargingStationAssigned = new boolean[this.getChargingStations().size()][this.getEnergyPriceHistory().getNTimeslots()];
		
		List<Integer> sortedCarIDs = CarAssignmentPriority.sortCarIdByArrivalTimeslot(this.getCars());
		for (int n : sortedCarIDs) {
			Car car = this.getCars().get(n);
			boolean isAssigned = false;
			
			for (int i=0;i<chargingStationAssigned.length && isAssigned == false;i++) {
				ChargingStation chargingStation = this.getChargingStations().get(i);
				
				boolean isFreeEntireTime = true; // Free entire time of car stay
				for (int k=car.getFirstAvailableTimeslot();k<=car.getLastAvailableTimeslot();k++) {
					if (chargingStationAssigned[i][k] == true)
						isFreeEntireTime = false;
				}
				
				if (isFreeEntireTime == true) {
					// If a charging station is free at first available timeslot: take it!
					// Reserve chargingStation for slots in which the car is there
					isAssigned = true;
					variables.add(buildJSONVariableX(i, car.getId(), 1));
					
					double remainingCapacity = car.getMaxCapacity() - car.getCurrentCapacity();
					
					// Build P_i,j,k variables
					for (int k=car.getFirstAvailableTimeslot();k<=car.getLastAvailableTimeslot();k++) {
						double canLoadSum = car.sumUsedPhases;
						double capacityLoadedInTimeslot = 0;
						
						for (int j=1;j<=3;j++) {
							// Take minimum of maximum power or fuse of chargingstation
							// IGNORE min power for now
							double maxPhaseAmps = Math.min(chargingStation.getFusePhase(j), car.maxCurrentPerPhase);
							double power;
							if (remainingCapacity > canLoadSum * maxPhaseAmps/4) {
								// Won't fill capacity in this timeslot
								power = car.canLoadPhase(j) * maxPhaseAmps;
							}			
							else {
								power = car.canLoadPhase(j) / canLoadSum * remainingCapacity * 4;
							}
							capacityLoadedInTimeslot += power/4;
							variables.add(buildJSONVariableP(i, j, k, power));
						}
						remainingCapacity -= capacityLoadedInTimeslot;
						
						chargingStationAssigned[i][k] = true;
					}
				}
			}
		}
		return solution;
	}
			
	@Override
	public JSONObject getSolutionJSON() {
		JSONObject solution = null;
		try (MethodTimer t = new MethodTimer(this.timeSolution)) {
			solution = getSolution();
		}
		return solution;
	}

	@Override
	public String getMethod() {
		return "greedy";
	}

}
