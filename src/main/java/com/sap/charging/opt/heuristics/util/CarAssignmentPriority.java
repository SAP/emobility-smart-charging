package com.sap.charging.opt.heuristics.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.sap.charging.model.Car;

public class CarAssignmentPriority {
	
	
	public static class CarForecastComparator implements Comparator<Car>  {
		
		private final List<Double> forecastedSoCs;
		
		public CarForecastComparator(List<Double> forecastedSoCs) {
			this.forecastedSoCs = forecastedSoCs;
		}
		
		@Override
		public int compare(Car car1, Car car2) {
			
			double val1 = forecastedSoCs.get(car1.getId());
			double val2 = forecastedSoCs.get(car2.getId());

			//System.out.println("val1=" + val1 + ", val2=" + val2);
			
			if (val1 == val2) {
				return 0;
			}
			if (val1 < val2) {
				return -1;
			}
			return 1;
		}
		
	}
	
	public enum CarComparator {
		/**
		 * Sorts ascending, i.e. earliest car is first
		 */
		ARRIVAL_TIMESLOT(new Comparator<Car>() {
			@Override
			public int compare(Car car1, Car car2) {
				// -1 - less than, 1 - greater than, 0 - equal
				if (car1.getFirstAvailableTimeslot()==car2.getFirstAvailableTimeslot())
					return 0;
				if (car1.getFirstAvailableTimeslot()< car2.getFirstAvailableTimeslot()) 
					return -1;
				return 1;
			}
		}),
		/**
		 * Sorts ascending, i.e. earliest car is first
		 */
		ARRIVAL_TIMESTAMP(new Comparator<Car>() {
			@Override
			public int compare(Car car1, Car car2) {
				// -1 - less than, 1 - greater than, 0 - equal
				return car1.timestampArrival.isBefore(car2.timestampArrival) ?
						-1 : 1;
			}
		}),
		/**
		 * Sorts descending, i.e. car with highest (capacity - state of charge) first
		 */
		ABSOLUTE_STATE_OF_CHARGE(new Comparator<Car>() {
			@Override
			public int compare(Car car1, Car car2) {
				// -1 - less than, 1 - greater than, 0 - equal
				double chargingNeed1 = car1.getMaxCapacity() - car1.getCurrentCapacity();
				double chargingNeed2 = car2.getMaxCapacity() - car2.getCurrentCapacity();
				
				if (chargingNeed1==chargingNeed2)
					return 0;
				if (chargingNeed1< chargingNeed2) 
					return 1;
				return -1;
			}
		}),
		/**
		 * Sorts descending, i.e. car with highest (capacity - state of charge) first
		 */
		RELATIVE_STATE_OF_CHARGE(new Comparator<Car>() {
			@Override
			public int compare(Car car1, Car car2) {
				// -1 - less than, 1 - greater than, 0 - equal
				double soc1 = car1.getCurrentCapacity() / car1.getMaxCapacity();
				double soc2 = car2.getCurrentCapacity() / car2.getMaxCapacity();
				
				if (soc1==soc2)
					return 0;
				if (soc1< soc2) 
					return 1;
				return -1;
			}
		});
	
		public final Comparator<Car> comparator;
		
		CarComparator(Comparator<Car> comparator) {
			this.comparator = comparator;
		}
		
	}
	
	
	
	
	private static List<Integer> sortCarId(List<Car> cars, Comparator<Car> comparator) {
		List<Car> carsCopy = new ArrayList<Car>();
		carsCopy.addAll(cars);
		carsCopy.sort(comparator);
		List<Integer> result = new ArrayList<Integer>();
		for (Car car : carsCopy)
			result.add(car.getId());
		return result;
	}
	
	/**
	 * Sorts a list of cars by their discretized timeslot
	 * @param cars
	 * @return
	 */
	public static List<Integer> sortCarIdByArrivalTimeslot(List<Car> cars) {
		return sortCarId(cars, CarComparator.ARRIVAL_TIMESLOT.comparator);
    }

	/**
	 * Sorts a list of cars by their non-discretized timestamp
	 * @param cars
	 * @return
	 */
	public static List<Integer> sortCarIdByArrivalTimestamp(List<Car> cars) {
		return sortCarId(cars, CarComparator.ARRIVAL_TIMESTAMP.comparator);
	}
	
	  
	/**
	 * Sorts a list of cars by their absolute state of charge, i.e. 
	 * who can charge the most Ah. 
	 * @param cars
	 * @return
	 */
	public static List<Integer> sortCarIdByAbsSoC(List<Car> cars) {
		return sortCarId(cars, CarComparator.ABSOLUTE_STATE_OF_CHARGE.comparator);
	}

	/**
	 * Sorts a list of cars by their relative state of charge, i.e. 
	 * who has the lowest relative SoC. 
	 * @param cars
	 * @return
	 */
	public static List<Integer> sortCarIdByRelSoC(List<Car> cars) {
		List<Integer> result = sortCarId(cars, CarComparator.RELATIVE_STATE_OF_CHARGE.comparator);
		Collections.reverse(result);
		return result;
	}

	
	/**
	 * Sorts a list of cars by their forecasted SoC
	 * @param cars
	 * @return
	 */
	public static List<Integer> sortCarByForecastedSoC(List<Car> cars, List<Double> forecastedSoCs) {
		return sortCarId(cars, new CarForecastComparator(forecastedSoCs));
	}
	
	
	
	
	
	
	
	
}
