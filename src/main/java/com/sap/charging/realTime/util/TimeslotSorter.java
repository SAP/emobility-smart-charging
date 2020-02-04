package com.sap.charging.realTime.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sap.charging.model.Car;
import com.sap.charging.realTime.State;
import com.sap.charging.util.SortableElement;

public class TimeslotSorter {

	
	public static List<SortableElement<Integer>> getSortedTimeslots(State state, int minK, int maxK, TimeslotSortingCriteria sortingCriteria) {
		return getSortedTimeslots(state, minK, maxK, sortingCriteria, null);
	}
	
	public static List<SortableElement<Integer>> getSortedTimeslots(State state, int minK, int maxK, 
			TimeslotSortingCriteria sortingCriteria, boolean[] blockedTimeslots) {
		
		List<SortableElement<Integer>> sortedTimeslots = new ArrayList<>();
		for (int k=minK;k<maxK;k++) {
			if (blockedTimeslots == null || blockedTimeslots[k] == false) {
				double value = getTimeslotValue(state, k, sortingCriteria);
				sortedTimeslots.add(new SortableElement<Integer>(k, value));
			}
		}
		Collections.sort(sortedTimeslots);
		return sortedTimeslots;
	}
	
	public static double getTimeslotValue(State state, int k, TimeslotSortingCriteria sortingCriteria) {
		switch (sortingCriteria) {
		case INDEX:
			return k;
		case PRICE:
			return state.energyPriceHistory.getPrice(k);
		case PEAK_DEMAND:
			double sumPlanned = 0;
			for (Car car : state.cars) {
				if (car.getCurrentPlan() != null) 
					sumPlanned += car.sumUsedPhases * car.getCurrentPlan()[k];
			}
			return sumPlanned;
		default:
			throw new RuntimeException("NOT IMPLEMENTED");
		}
	}
	
	public static List<SortableElement<Integer>> getSortedTimeslotsByIndex(int minK, int maxK) {
		return TimeslotSorter.getSortedTimeslots(null, minK, maxK, TimeslotSortingCriteria.INDEX); 
	}
	
}
