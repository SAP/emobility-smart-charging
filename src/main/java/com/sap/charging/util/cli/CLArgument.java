package com.sap.charging.util.cli;

import java.util.Arrays;

public class CLArgument<T> {
	
	public final String name;
	private T value;
	private boolean isValueSet;
	
	public Class<T> type;
	public final boolean optional;
	public final T defaultValue;
	public final T[] allowedValues;
	
	public CLArgument(String name, Class<T> type, boolean optional, T defaultValue, T[] allowedValues) {
		this.name = name;
		this.type = type;
		this.optional = optional;
		this.defaultValue = defaultValue;
		this.allowedValues = allowedValues;
	}
	
	public void setValue(T value) throws CLArgumentInvalidValue {
		if (allowedValues == null) {
			this.value = value;
		}
		else {
			boolean allowed = false;
			for (T allowedValue : allowedValues) {
				if (value.equals(allowedValue)) {
					allowed = true;
					this.value = value;
				}
			}
			if (allowed == false) {
				throw new CLArgumentInvalidValue(this.name, value);
			}
		}
		isValueSet = true;
	}
	
	public T getValue() {
		return value;
	}
	
	public boolean isValueSet() {
		return isValueSet;
	}
	
	@Override
	public String toString() {
		String result = name + ": type=" + type.getName() + 
			   ", optional=" + optional + 
			   ", defaultValue=" + defaultValue;
		if (allowedValues != null) {
			result += ", allowedValues=" + Arrays.toString(allowedValues);
		}
		return result;
	}
	
	
}
