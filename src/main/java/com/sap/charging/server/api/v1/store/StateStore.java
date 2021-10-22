package com.sap.charging.server.api.v1.store;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.EnergyUtil;
import com.sap.charging.model.Fuse;
import com.sap.charging.model.FuseTree;
import com.sap.charging.realTime.State;
import com.sap.charging.realTime.model.CarAssignment;
import com.sap.charging.realTime.model.CarAssignmentStore;
import com.sap.charging.server.api.v1.exception.MissingParameterException;
import com.sap.charging.server.api.v1.exception.ParameterCombinationException;
import com.sap.charging.server.api.v1.exception.UnknownCarException;
import com.sap.charging.server.api.v1.exception.UnknownChargingStationException;
import com.sap.charging.util.TimeUtil;

public class StateStore {

	public final int currentTimeSeconds;
	public final List<ChargingStationStore> chargingStations;

	@JsonIgnore
	public final List<ChargingStation> chargingStationsConverted; 
	
	public final FuseTree fuseTree;
	public final Double maximumSiteLimitKW;
	public final List<Car> cars;
	public final EnergyPriceHistory energyPriceHistory;
	public final List<CarAssignmentStore> carAssignments;

	@JsonCreator
	public StateStore(@JsonProperty(value = "currentTimeSeconds", required = true) int currentTimeSeconds,
			@JsonProperty("fuseTree") FuseTree fuseTree,

			@JsonProperty("chargingStations") List<ChargingStationStore> chargingStations,
			@JsonProperty("maximumSiteLimitKW") Double maximumSiteLimitKW,

			@JsonProperty(value = "cars", required = true) List<Car> cars,
			@JsonProperty("energyPriceHistory") EnergyPriceHistory energyPriceHistory,
			@JsonProperty(value = "carAssignments", required = true) List<CarAssignmentStore> carAssignments) {
		this.currentTimeSeconds = currentTimeSeconds;
		
		// Allowed parameter combinations: 
		// chargingStations & maximumSiteLimitKW
		// fuseTree
		
		if (chargingStations == null && fuseTree == null) {
			throw new MissingParameterException("Pass either fuseTree or chargingStations together with maximumSiteLimitKW");
		} else if (chargingStations != null && fuseTree != null) {
			throw new ParameterCombinationException("If passing a fuse tree, add charging stations in the fuse tree as children of fuses." + 
													" Do not pass in a fuse tree as well as a separate array of charging stations."); 
		} else if (chargingStations != null) {
			this.chargingStations = chargingStations;
			this.chargingStationsConverted = chargingStations.stream()
					.map(store -> store.toChargingStation()).collect(Collectors.toList());
		} else {
			// Fuse tree is passed, get charging stations from it
			this.chargingStations = fuseTree.getListOfChargingStations().stream()
					.map(station -> ChargingStationStore.fromChargingStation(station)).collect(Collectors.toList());
			this.chargingStationsConverted = fuseTree.getListOfChargingStations(); 
			
		}

		if (fuseTree == null && (maximumSiteLimitKW == null || chargingStations == null)) {
			throw new MissingParameterException("fuseTree", "maximumSiteLimitKW and chargingStations");
		}
		this.fuseTree = fuseTree;
		this.maximumSiteLimitKW = maximumSiteLimitKW;
		this.cars = cars;

		if (energyPriceHistory == null) {
			this.energyPriceHistory = new EnergyPriceHistory(24 * 4, null);
		} else {
			this.energyPriceHistory = energyPriceHistory;
		}
		this.carAssignments = carAssignments;
		this.sanityCheckCarAssignments(this.chargingStations, this.cars, this.carAssignments);
	}

	/**
	 * Check that all carAssignment cars and charging stations can be found (by id)
	 * 
	 * @param chargingStations
	 * @param cars
	 * @param carAssignments
	 */
	private void sanityCheckCarAssignments(List<ChargingStationStore> chargingStations, List<Car> cars,
			List<CarAssignmentStore> carAssignments) {
		for (CarAssignmentStore carAssignment : carAssignments) {
			boolean carFound = false;
			for (Car car : cars) {
				if (car.getId() == carAssignment.carID) {
					carFound = true;
				}
			}

			if (carFound == false) {
				throw new UnknownCarException(carAssignment.carID);
			}

			boolean chargingStationFound = false;
			for (ChargingStationStore chargingStation : chargingStations) {
				if (chargingStation.id == carAssignment.chargingStationID) {
					chargingStationFound = true;
				}
			}
			if (chargingStationFound == false) {
				throw new UnknownChargingStationException(carAssignment.chargingStationID);
			}
		}
	}

	public State toState() {

		State state = new State(0, this.chargingStationsConverted, null, cars, energyPriceHistory);

		state.currentTimeSeconds = currentTimeSeconds;
		state.currentTimeslot = TimeUtil.getTimeslotFromSeconds(currentTimeSeconds);

		// Prepare cars
		for (Car car : state.getCars()) {
			if (car.getCurrentPlan() == null) {
				car.setCurrentPlan(new double[energyPriceHistory.getNTimeslots()]);
			}
		}

		if (this.fuseTree != null) {
			// Fuse tree is optional. If it was passed, use original
			state.fuseTree = fuseTree;
			state.fuseTree.refreshFuseTreeNodeParents(); 
		} else {
			// Prepare fuse tree by using maximumSiteLimit and chargingStationStore
			double fuseSizePerPhase = EnergyUtil.calculateIFromP(maximumSiteLimitKW, 3); // in ampere

			Fuse rootFuse = new Fuse(0, fuseSizePerPhase);
			for (ChargingStation chargingStation : this.chargingStationsConverted) {
				rootFuse.addChild(chargingStation);
			}
			
			state.fuseTree = new FuseTree(rootFuse, chargingStations.size());
		}
		
		
		// Prepare car assignments
		for (CarAssignmentStore carAssignmentStore : carAssignments) {
			Car car = state.getCar(carAssignmentStore.carID);
			ChargingStation chargingStation = state.getChargingStation(carAssignmentStore.chargingStationID);
			CarAssignment carAssignment = state.addCarAssignment(car, chargingStation);
			if (car.getTimestampDeparture() > 0) {
				carAssignment.setExpectedDepartureTimeSeconds(car.getTimestampDeparture());
			}
		}

		// Build power assignments from current plans of cars
		for (CarAssignment carAssignment : state.getAllCarAssignments()) {
			Car car = carAssignment.car;
			if (car.getCurrentPlan() != null) {
				double currentPlanValue = car.getCurrentPlan()[state.currentTimeslot];
				state.addPowerAssignment(car, carAssignment.chargingStation, car.canLoadPhase1 * currentPlanValue,
						car.canLoadPhase2 * currentPlanValue, car.canLoadPhase3 * currentPlanValue);
			}
		}
		return state;
	}

}
