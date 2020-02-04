package com.sap.charging.dataGeneration;

import java.time.LocalTime;
import java.util.List;

import com.sap.charging.dataGeneration.carDistributions.CarArrivalDepartureDistribution.Tuple;
import com.sap.charging.model.CarProcessData;
import com.sap.charging.model.CarFactory.CarModel;

/**
 * More sophisticated version of DataGeneratorRandom adding attributes for carPark, carType, wday (weekday)
 * 
 * Same data generating as DataGeneratorRandom except for generateCars
 *
 */
public class DataGeneratorRandomProcesses extends DataGeneratorRandom {

	private final List<CarProcessData> carProcessData;
	
	public DataGeneratorRandomProcesses(int seed, int nCars) {
		super(seed, false);
		this.carProcessData = CarProcessData.sampleHistoricalCarProcesses(nCars, seed);
	}
	
	public DataGeneratorRandomProcesses(int seed, List<CarProcessData> carProcessData) {
		super(seed, false);
		this.carProcessData = carProcessData;
	}

	@Override
	protected CarModel[] getCarModelSubset(int n) {
		// Filter by PHEV or BEV models for car n
		//String carType = carProcessData.get(n).carType;
		/*if (carType.equals("B")) {
			return Arrays.stream(CarModel.values()).filter(c -> c.carType.equals(CarType.BEV)).toArray(size -> new CarModel[size]);
		}
		else {
			return Arrays.stream(CarModel.values()).filter(c -> c.carType.equals(CarType.PHEV)).toArray(size -> new CarModel[size]);
		}*/
		return new CarModel[] {CarModel.TESLA_MODEL_S};
	}
	
	@Override
	protected Tuple generateNonUniformArrivalDepartureTimeslots(int n) {
		// Use timestamps sampled from R for car n and round down to 15 minute timeslots
		return new Tuple(
				carProcessData.get(n).timestampArrival / (15*60),
				carProcessData.get(n).timestampDeparture / (15*60)
				);
	}
	
	@Override
	protected LocalTime[] generateArrivalDepartureTimestamp(Tuple tuple, int n) {
		// Use timestamps sampled from R for car n
		return new LocalTime[] {
				LocalTime.ofSecondOfDay(carProcessData.get(n).timestampArrival),
				LocalTime.ofSecondOfDay(carProcessData.get(n).timestampDeparture)
		};
	}
	
	@Override
	public CarProcessData getCarProcessData(int n) {
		return carProcessData.get(n);
	}

}





