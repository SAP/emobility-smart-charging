package com.sap.charging.opt;

import java.util.List;

import org.json.simple.JSONObject;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.FuseTree;
import com.sap.charging.opt.Instance;

public class InstanceEmpty extends Instance{

	public InstanceEmpty(List<Car> cars, 
			List<ChargingStation> chargingStations, 
			EnergyPriceHistory energyPriceHistory,
			FuseTree fuseTree) {
		super(cars, chargingStations, energyPriceHistory, fuseTree);
	}
	
	public InstanceEmpty(DataGenerator data) {
		super(data);
	}

	@Override
	protected JSONObject getSolutionJSON() {
		return null;
	}

	@Override
	public String getMethod() {
		return "empty";
	}
	
}
