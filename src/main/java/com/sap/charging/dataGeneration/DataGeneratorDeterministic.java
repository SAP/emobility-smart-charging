package com.sap.charging.dataGeneration;

import java.time.LocalTime;
import java.util.ArrayList;

import com.sap.charging.model.Car;
import com.sap.charging.model.CarFactory;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.ChargingStationFactory;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.EnergyUtil;
import com.sap.charging.model.CarFactory.CarModel;
import com.sap.charging.model.CarFactory.CarType;

public class DataGeneratorDeterministic extends DataGenerator {

	/**
	 * Always deterministic, since uses the same model.
	 * @param nChargingStations
	 * @return
	 */
	public DataGenerator generateChargingStations(int nChargingStations) {
		chargingStations = new ArrayList<>();
		for (int i=minChargingStationId;i<nChargingStations+minChargingStationId;i++) {
			ChargingStation chargingStation = ChargingStationFactory.builder()
					.buildFromStandard(ChargingStationFactory.Standard.KeContact_P30)
					.setIndexI(i)
					.build();
			
			if (generateIdealChargingStations == false) {
				if (i%2==0) chargingStation.isPHEVAllowed= false;
				if (i%2==1) chargingStation.isBEVAllowed = false;
			}
			
			chargingStations.add(chargingStation);
		}
		return this;
	}
	
	public ArrayList<Car> generateDefaultCars() {
		ArrayList<Car> result = new ArrayList<>();
		int nTimeslots = this.getEnergyPriceHistory().getNTimeslots();
		int indexN = minCarId;

		Car car0 = CarFactory.builder()
				.set(CarModel.TESLA_MODEL_S)
				.id(indexN++)
				.currentCapacity(EnergyUtil.calculateIFromP(82, 1)) // max 85
				.availableTimeslots(0, 5, nTimeslots)
				.availableTimestamps(LocalTime.of(0, 0), LocalTime.of(1, 30))
				.immediateStart(false)
				.suspendable(false)
				.canUseVariablePower(false)
				.carType(CarType.BEV)
				.build();
		result.add(car0);
		Car car1 = CarFactory.builder()
				.set(CarModel.NISSAN_LEAF_2016)
				.id(indexN++)
				.currentCapacity(EnergyUtil.calculateIFromP(20, 1)) // max 30
				.availableTimeslots(0, 0, nTimeslots)
				.availableTimestamps(LocalTime.of(0, 0), LocalTime.of(0, 15))
				.carType(CarType.PHEV)
				.build();
		result.add(car1);
		Car car2 = CarFactory.builder()
				.set(CarModel.TESLA_MODEL_S)
				.id(indexN++)
				.currentCapacity(EnergyUtil.calculateIFromP(77, 1))
				.availableTimeslots(1, 2, nTimeslots)
				.availableTimestamps(LocalTime.of(0, 15), LocalTime.of(0, 45))
				.carType(CarType.BEV)
				.build();
		result.add(car2);
		Car car3 = CarFactory.builder()
				.set(CarModel.NISSAN_LEAF_2016)
				.id(indexN++)
				.currentCapacity(EnergyUtil.calculateIFromP(20, 1)) 
				.availableTimeslots(3, 3, nTimeslots)
				.availableTimestamps(LocalTime.of(0,45), LocalTime.of(1,0))
				.carType(CarType.PHEV)
				.build();
		result.add(car3);
		Car car4 = CarFactory.builder()
				.set(CarModel.TESLA_MODEL_S)
				.id(indexN++)
				.currentCapacity(EnergyUtil.calculateIFromP(83, 1)) // max 85
				.availableTimeslots(2, 4, nTimeslots)
				.availableTimestamps(LocalTime.of(0, 30), LocalTime.of(1, 15))
				.immediateStart(false)
				.suspendable(true)
				.canUseVariablePower(true)
				.carType(CarType.BEV)
				.build();
		result.add(car4);
		
		return result;
	}
	
	/**
	 * Returns generateDefaultCars()
	 */
	@Override
	public DataGenerator generateCars(int nCars) {
		cars = generateDefaultCars();
		if (nCars < cars.size() && nCars > 0) {
			int nTooMany = cars.size() - nCars;
			while (nTooMany > 0) {
				cars.remove(cars.size()-1);
				nTooMany--;
			}
		}
		return this;
	}

	public static final double[] defaultEnergyPriceHistory = 
			new double[]{
					90, 55, 60, 55, 45, 50
			};
	
	/**
	 * @param lengthK Ignored
	 */
	public DataGenerator generateEnergyPriceHistory(int lengthK) {
		energyPriceHistory = new EnergyPriceHistory(defaultEnergyPriceHistory);
		return this;
	}

	@Override
	public DataGenerator clone() {
		throw new RuntimeException("ERROR: Not implemented yet");
	}

	
	

}
