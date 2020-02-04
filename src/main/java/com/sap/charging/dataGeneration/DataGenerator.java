package com.sap.charging.dataGeneration;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.sap.charging.dataGeneration.carParks.CarPark;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.Fuse;
import com.sap.charging.model.FuseTree;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.util.TimeUtil;


/**
 * Order to call generate functions in:
 * generateEnergyPriceHistory() -> generateChargingStations() -> generateCars() -> generateFuseTree()
 * 
 */
public abstract class DataGenerator {
	
	/**
	 * Energy price history parameters
	 */
	protected EnergyPriceHistory energyPriceHistory;
	/**
	 * Gives the first timeslot (e.g. 20 = 05:00)
	 */
	public static int START_BASE_TIMESLOT = 0 * 4;
	
	
	/**
	 * Car parameters
	 */
	protected ArrayList<Car> cars;
	protected boolean generateIdealCars = false;
	
	/**
	 * Charging station parameters
	 */
	protected ArrayList<ChargingStation> chargingStations;
	protected boolean generateIdealChargingStations = false;
	
	/**
	 * Fuse tree parameters
	 */
	protected FuseTree fuseTree;
	protected boolean doRotateFuseTreePhases = false;
	protected CarPark carPark = CarPark.CUSTOM;
	
	
	protected int minChargingStationId = 0;
	public void setMinChargingStationId(int minChargingStationId) {
		this.minChargingStationId = minChargingStationId;
	}
	
	public void setCarPark(CarPark carPark) {
		this.carPark = carPark;
	}
	public CarPark getCarPark() {
		return this.carPark;
	}
	
	public final DataGenerator generateChargingStations() {
		if (carPark.defaultNumberChargingStations > 0) {
			return this.generateChargingStations(carPark.defaultNumberChargingStations);
		}
		else {
			throw new RuntimeException("Car park (" + carPark.name() + ") has a defaultNumberChargingStations <=0 (" + carPark.defaultNumberChargingStations + "), please call generateChargingStations(int)");
		}
	}
	public abstract DataGenerator generateChargingStations(int nChargingStations);
	public List<ChargingStation> getChargingStations() {
		return this.chargingStations;
	}
	public ChargingStation getChargingStation(int indexI) {
		if (chargingStations == null) {
			throw new RuntimeException("ERROR: Call generateChargingStations first");
		}
		for (ChargingStation chargingStation : chargingStations)
			if (chargingStation.getId() == indexI) 
				return chargingStation;
		return null;
	}
	
	protected int minCarId = 0;
	public void setMinCarId(int minCarId) {
		this.minCarId = minCarId;
	}
	/**
	 * generateEnergyPriceHistory must be called first
	 * @param nCars
	 * @return
	 */
	public abstract DataGenerator generateCars(int nCars);
	public List<Car> getCars() {
		return this.cars;
	}
	public Car getCar(int indexN) {
		for (Car car : cars) 
			if (car.getId() == indexN) 
				return car;
		return null;
	}
	
	/**
	 * Returns a continuous timestamp based on a discrete timeslot,
	 * in 15 minute intervals. Starts at 05:00. 
	 * timeslot=0 ==> 05:00:00
	 * timeslot=1 ==> 05:15:00
	 * @param timeslot
	 * @return
	 */

	public static LocalTime getTimestamp(int timeslot) {
		return TimeUtil.getTimestampFromTimeslot(START_BASE_TIMESLOT + timeslot);
	}
	
	
	public abstract DataGenerator generateEnergyPriceHistory(int lengthK);
	public EnergyPriceHistory getEnergyPriceHistory() {
		return this.energyPriceHistory;
	}
	
	
	
	public DataGenerator generateFuseTree() {
		if (carPark == CarPark.CUSTOM) {
			throw new RuntimeException("Please call setCarPark(CarPark) to use a default car park");
		}
		
		this.doRotateFuseTreePhases = true;
		int counterChargingStation = 0; 		
		int counterL = 0;
		
		Fuse rootFuse = new Fuse((counterL++), carPark.defaultFuseLevel0);
			
		for (int i=0;i<carPark.defaultNumberFusesLevel1;i++) {
			
			Fuse fuseLevel1 = new Fuse((counterL++), carPark.defaultFuseLevel1);
			
			if (carPark.defaultDepth > 2) {
				for (int j=0;j<carPark.defaultNumberFusesLevel2;j++) {
					
					Fuse fuseLevel2 = new Fuse((counterL++), carPark.defaultFuseLevel2);
					for (int k=0;k<carPark.defaultNumberChargingStationsLowest;k++) {
						
						if (counterChargingStation < carPark.defaultNumberChargingStations) {
							ChargingStation chargingStationI = getChargingStations().get(counterChargingStation);
							chargingStationI.setPhaseMatching(
									Phase.getByInt((k % 3)+1), 
									Phase.getByInt(((k+1) % 3)+1), 
									Phase.getByInt(((k+2) % 3)+1)
							);
							
							fuseLevel2.addChild(chargingStationI);
							counterChargingStation++;
						}
					}
					fuseLevel1.addChild(fuseLevel2);
				}
			}
			else {
				for (int k=0;k<carPark.defaultNumberChargingStationsLowest;k++) {
					
					ChargingStation chargingStationI = getChargingStation(counterChargingStation);
					chargingStationI.setPhaseMatching(
							Phase.getByInt((counterChargingStation % 3)+1), 
							Phase.getByInt(((counterChargingStation+1) % 3)+1), 
							Phase.getByInt(((counterChargingStation+2) % 3)+1)
					);
					
					fuseLevel1.addChild(chargingStationI);
					counterChargingStation++;
				}
			}
			
			rootFuse.addChild(fuseLevel1);
			
		}
		
		fuseTree = new FuseTree(rootFuse, carPark.defaultNumberChargingStationsLowest);
		return this;
	}
	/**
	 * Models a car park infrastructure. 
	 * The following fields should be set previously: 
	 * CONSTANTS.FUSE_LEVEL_0, CONSTANTS.FUSE_LEVEL_1, CONSTANTS.FUSE_LEVEL_2
	 * 
	 * generateChargingStations() must be called first.
	 * @return
	 */
	public DataGenerator generateFuseTree(int nChargingStationsPerLevel2, boolean doRotatePhases) {
		this.doRotateFuseTreePhases = doRotatePhases;
		int counterChargingStation = 0; 		
		int counterL = 0;
		
		int nVerticalFuses = 3; // Steigschienen
		int nHorizontalFuses = 2; // Versorgungsschienen
		
		if (nVerticalFuses * nHorizontalFuses * nChargingStationsPerLevel2 < chargingStations.size()) {
			throw new RuntimeException("Number of charging stations is greater than fits in fuse tree: " +
										(nVerticalFuses*nHorizontalFuses*nChargingStationsPerLevel2) + "<" + chargingStations.size());
		}
		
		Fuse rootFuse = new Fuse((counterL++), CONSTANTS.FUSE_LEVEL_0_SIZE);
		
		for (int i=0;i<nVerticalFuses;i++) {
			// Steigschienen
			Fuse fuseLevel1 = new Fuse((counterL++), CONSTANTS.FUSE_LEVEL_1_SIZE);
			
			for (int j=0;j<nHorizontalFuses;j++) {
				// Versorgungsschienen
				Fuse fuseLevel2 = new Fuse((counterL++), CONSTANTS.FUSE_LEVEL_2_SIZE);
				
				for (int k=0;k<nChargingStationsPerLevel2;k++) {
					// Wallboxen 
					if (counterChargingStation < getChargingStations().size()) {
						// If any wallboxes are not yet assigned
						// Phase consumed is implemented as Phasenwanderung
						ChargingStation chargingStationI = getChargingStation(counterChargingStation);
						if (doRotatePhases == true) {
							chargingStationI.setPhaseMatching(
									Phase.getByInt((k % 3)+1), 
									Phase.getByInt(((k+1) % 3)+1), 
									Phase.getByInt(((k+2) % 3)+1)
							);
						}
						else {
							// No rotating phases: Assign each charging station phase to each actual phase consumed
							chargingStationI.setPhaseMatching(Phase.PHASE_1, Phase.PHASE_2, Phase.PHASE_3);
						}
						
						fuseLevel2.addChild(chargingStationI);
						counterChargingStation++;
					}
				}
				fuseLevel1.addChild(fuseLevel2);
			}
			rootFuse.addChild(fuseLevel1);			
		}
		
		fuseTree = new FuseTree(rootFuse, nChargingStationsPerLevel2);
		return this;
	}
	
	/**
	 * Generates a fuse tree with a single root fuse using CONSTANTS.FUSE_LEVEL_0 per phase
	 * 
	 * Rotates phases for charging stations
	 * @return
	 */
	public DataGenerator generateSimpleFuseTree() {
		return this.generateSimpleFuseTree(CONSTANTS.FUSE_LEVEL_0_SIZE);
	}
	/**
	 * Generates a fuse tree with a single root fuse using CONSTANTS.FUSE_LEVEL_0 per phase
	 * 
	 * Rotates phases for charging stations
	 * @return
	 */
	public DataGenerator generateSimpleFuseTree(double fuseSizePerPhase) {
		Fuse rootFuse = new Fuse(0, CONSTANTS.FUSE_LEVEL_0_SIZE);
		
		for (int i=0;i<getChargingStations().size();i++) {
			
			ChargingStation chargingStationI = getChargingStation(i);
			chargingStationI.setPhaseMatching(
					Phase.getByInt((i % 3)+1), 
					Phase.getByInt(((i+1) % 3)+1), 
					Phase.getByInt(((i+2) % 3)+1)
			);
			rootFuse.addChild(chargingStationI);
			
		}
		
		fuseTree = new FuseTree(rootFuse, getChargingStations().size());
		return this;
	}
	
	
	public FuseTree getFuseTree() {
		return this.fuseTree;
	}
	
	
	public void setIdealCars(boolean idealCars) {
		this.generateIdealCars = idealCars;
	}
	
	public void setIdealChargingStations(boolean idealChargingStations) {
		this.generateIdealChargingStations = idealChargingStations;
	}

	public abstract DataGenerator clone();
	
	
	
	
	
	
}
