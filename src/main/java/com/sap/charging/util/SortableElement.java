package com.sap.charging.util;

public class SortableElement<T> implements Comparable<SortableElement<T>> {

	public final T index;
	public final double value;
	
	public SortableElement(T index, double value) {
		this.index = index;
		this.value = value;
	}
	
	@Override
	public int compareTo(SortableElement<T> otherElement) {
		if (this.value < otherElement.value) return -1;
		if (this.value > otherElement.value) return 1;
		return 0;
		//return (int) (this.value - otherElement.value);
	}
	
	@Override
	public String toString() {
		return "index=" + index + "; value=" + value;
	}

}
