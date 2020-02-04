package com.sap.charging.opt.heuristics;

import java.util.List;

import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.FuseTree;
import com.sap.charging.opt.Instance;

public abstract class InstanceHeuristic extends Instance {

	public InstanceHeuristic(List<Car> cars, List<ChargingStation> chargingStations,
			EnergyPriceHistory energyPriceHistory, FuseTree fuseTree) {
		super(cars, chargingStations, energyPriceHistory, fuseTree);
	}

	
	
	

}
