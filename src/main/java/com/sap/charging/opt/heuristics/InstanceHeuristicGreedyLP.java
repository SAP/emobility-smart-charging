package com.sap.charging.opt.heuristics;

import java.util.List;

import org.json.simple.JSONObject;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.FuseTree;
import com.sap.charging.opt.heuristics.util.CarAssignmentPriority;
import com.sap.charging.opt.lp.InstanceLP;
import com.sap.charging.opt.lp.util.SolverSCIP;
import com.sap.charging.opt.util.MethodTimer;

public class InstanceHeuristicGreedyLP extends InstanceHeuristic {
	
	private InstanceLP instanceLP;
	
	
	/**
	 * Goal: Do assignments X_i,n greedily, then optimize power consumption via LP
	 * @param cars
	 * @param chargingStations
	 * @param energyPriceHistory
	 * @param fuseTree
	 */
	public InstanceHeuristicGreedyLP(List<Car> cars, List<ChargingStation> chargingStations,
			EnergyPriceHistory energyPriceHistory, FuseTree fuseTree) {
		super(cars, chargingStations, energyPriceHistory, fuseTree);
		this.instanceLP = new InstanceLP(cars, chargingStations, energyPriceHistory, fuseTree);
	}
	public InstanceHeuristicGreedyLP(DataGenerator data) {
		this(data.getCars(),
			 data.getChargingStations(),
			 data.getEnergyPriceHistory(),
			 data.getFuseTree());
	}
	
	public InstanceLP getInstanceLP() {
		return this.instanceLP;
	}
	
	/**
	 * Prepare X_{i,n} car assignments for LP
	 */
	private void prepareInstanceLP() {
		try (MethodTimer t = new MethodTimer(this.timeProblemConstruction)) {
			this.instanceLP.constructVariables();
			
			// Assign a car to a charging station if one is free, else ignore it
			// If one is free: Maximum amps allowed by fuse/car loading
			boolean[][] chargingStationAssigned = new boolean[this.getChargingStations().size()][this.getEnergyPriceHistory().getNTimeslots()];
			
			List<Integer> sortedCarIDs = CarAssignmentPriority.sortCarIdByArrivalTimeslot(this.getCars());
			for (int n : sortedCarIDs) {
				Car car = this.getCars().get(n);
				boolean isAssigned = false;
				
				for (int i=0;i<chargingStationAssigned.length && isAssigned == false;i++) {
					//ChargingStation chargingStation = this.getChargingStations().get(i);
					
					boolean isFreeEntireTime = true;
					for (int k=car.getFirstAvailableTimeslot();k<=car.getLastAvailableTimeslot();k++) {
						if (chargingStationAssigned[i][k] == true)
							isFreeEntireTime = false;
					}
					
					if (isFreeEntireTime == true) {
						// If a charging station is free at first available timeslot: take it!
						// Reserve chargingStation for slots in which the car is there
						isAssigned = true;
						// Assign X_i,n to this value
						instanceLP.getVariableX(i, n).setValue(1);
						
						for (int k=car.getFirstAvailableTimeslot();k<=car.getLastAvailableTimeslot();k++) {
							chargingStationAssigned[i][k] = true;
						}
					}
				}
			}
		}
	}
	
	
			
	@Override
	public JSONObject getSolutionJSON() {
		prepareInstanceLP();
		instanceLP.constructProblem();
		
		instanceLP.setSolver(new SolverSCIP());
		instanceLP.solveProblem();
		
		this.timeProblemConstruction.addTime(instanceLP.timeProblemConstruction.getTime());
		this.timeSolution.addTime(instanceLP.timeSolution.getTime());
		
		JSONObject solution = instanceLP.getSolutionJSON();
		return solution;
	}
	
	@Override
	public String getMethod() {
		return "greedyLP";
	}

}
