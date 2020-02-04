package com.sap.charging.opt.heuristics;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.FuseTree;
import com.sap.charging.opt.lp.Equation;
import com.sap.charging.opt.lp.InstanceLP;
import com.sap.charging.opt.lp.util.SolverSCIP;

public abstract class InstanceHeuristicLP extends InstanceHeuristic {

	public SolverSCIP solver = null;
	
	public InstanceHeuristicLP(List<Car> cars, List<ChargingStation> chargingStations,
			EnergyPriceHistory energyPriceHistory, FuseTree fuseTree) {
		super(cars, chargingStations, energyPriceHistory, fuseTree);
		solver = new SolverSCIP();
	}

	protected InstanceLP instanceLP;
	protected boolean isProblemConstructed = false;
	public boolean scipApplyRelativeGapSetting = true;
	
	public InstanceLP getInstanceLP() {
		return this.instanceLP;
	}
	
	public abstract void prepareInstanceLP();
	
	
	
	public void constructProblem() {
		this.prepareInstanceLP();
		instanceLP.constructProblem();
		this.isProblemConstructed = true;
	}
	public void constructProblem(ArrayList<Equation> allRestrictions) {
		this.prepareInstanceLP();
		instanceLP.constructProblem(allRestrictions);
		this.isProblemConstructed = true;
	}
	
			
	@Override
	public JSONObject getSolutionJSON() {
		if (this.isProblemConstructed == false) {
			throw new RuntimeException("ERROR: Call .constructProblem first!");
		}
		
		if (scipApplyRelativeGapSetting == true) {
			solver.setApplyRelativeGapSetting(true);
		}
		
		instanceLP.setSolver(solver);
		instanceLP.solveProblem();
		
		this.timeProblemConstruction.addTime(instanceLP.timeProblemConstruction.getTime());
		this.timeSolution.addTime(instanceLP.timeSolution.getTime());
		
		JSONObject solution = instanceLP.getSolutionJSON();
		return solution;
	}
	
	
	
}	
