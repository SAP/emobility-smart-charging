package com.sap.charging.model.battery;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.charging.model.Car;
import com.sap.charging.model.EnergyUtil;
import com.sap.charging.opt.CONSTANTS;

public class CarBattery {

	  
	/**
	 * Maximum capacity in Ah (Q).
	 * Calculation example BMW i3: 353V * 94Ah ~ 33kWh
	 */
	public final double maxCapacity;
	
	/**
	 * Current (start) capacity in Ah (b_n), at start of simulation
	 */
	private double currentCapacity;
	/**
	 * Loaded capacity in Ah. Used as a dynamic field for realtime applications (simulation).
	 */
	private double chargedCapacity = 0;
		
	/**
	 * Should the car pull its current linearly (unrealistic, always use maxCurrent) 
	 * or nonlinearly (how its done in practice to protect battery life)?
	 */
	private final boolean nonlinearCharging;
	
	/**
	 * Nonlinear battery simulation that can simulate CCCV and CPCV curves
	 */
	public final BatterySim batterySim;

	/**
	 * Parameters for nonlinear battery simulation
	 */
	public final BatterySimParameters batterySimParams;
	
	//private final Car car;
	
	@JsonCreator
	public CarBattery(@JsonProperty("car") Car car,
			@JsonProperty("currentCapacity") double currentCapacity, 
			@JsonProperty("maxCapacity") double maxCapacity, 
			@JsonProperty("nonlinearCharging") boolean nonlinearCharging, 
			@JsonProperty("batteryData") BatteryData batteryData) {
		//this.car = car;
		
		this.currentCapacity = currentCapacity;
		this.maxCapacity = maxCapacity;
		this.nonlinearCharging = nonlinearCharging;
		
		if (nonlinearCharging == false) {
			this.batterySimParams = null;
			this.batterySim = null;
		}
		else {
			this.batterySimParams = BatterySimParameters.buildFromBatteryData(batteryData);
			this.batterySimParams.capacity = this.maxCapacity;
			this.batterySimParams.initialSoC = this.getSoC();
			//params.car = car;
			this.batterySim = new BatterySim(this.batterySimParams, false, false);
		}
		
	}
	
	public double getMaxCapacity() {
		return maxCapacity;
	}
	public double getCurrentCapacity() {
		return currentCapacity;
	}
	public void setCurrentCapacity(double currentCapacity) {
		this.currentCapacity = currentCapacity;
	}
	public double getChargedCapacity() {
		return chargedCapacity;
	}
	public void setChargedCapacity(double chargedCapacity) {
		this.chargedCapacity = chargedCapacity;
	}
	public void resetChargedCapacity() {
		this.chargedCapacity = 0;
	}
	
	@JsonIgnore
	public double getSoC() {
		return (this.getCurrentCapacity()+this.getChargedCapacity()) / this.getMaxCapacity();
	}
	
	@JsonIgnore
	public boolean isFullyCharged() {
		 return this.getCurrentCapacity()+ this.getChargedCapacity() >= this.getMaxCapacity();
	}
	
	public boolean getNonlinearCharging() {
		return this.nonlinearCharging;
	}
	
	
	/**
	 * Can we use function based on time? (February summary)
	 * No. If it was only suspended we can delay function, but sometimes we also get 10A restriction
	 * 
	 * 
	 * @return
	 */
	/*private double getCurrentBasedOnSoC() {
		
	}*/
	
	 /**
	   * Adds to curCapacity how much was charged over the given amount of seconds
	   * Will maximally add up to maxCapacity. 
	   * @param seconds Number of seconds that was charged with the given current
	   * @param current In A
	   */
	public void addChargedCapacity(int deltaSeconds, double maxCurrentAllowed) {
		double current;
		if (nonlinearCharging == false) {
			current = maxCurrentAllowed;
		}
		else {
			// Return current based on min{exponential function of SoC, maxCurrentPerPhaseAllowed}
			current = batterySim.getCurrentBasedOnSoC(this.getSoC(), maxCurrentAllowed);
		}
					
		double ampereHours = CONSTANTS.CHARGING_EFFICIENCY * EnergyUtil.getAmpereHours(deltaSeconds, current);
		this.chargedCapacity += ampereHours;
		if (this.isFullyCharged()) {
			this.chargedCapacity = this.maxCapacity - this.getCurrentCapacity();
		}
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CarBattery other = (CarBattery) obj;
		if (batterySim == null) {
			if (other.batterySim != null)
				return false;
		} else if (!batterySim.equals(other.batterySim))
			return false;
		if (batterySimParams == null) {
			if (other.batterySimParams != null)
				return false;
		} else if (!batterySimParams.equals(other.batterySimParams))
			return false;
		if (Double.doubleToLongBits(chargedCapacity) != Double.doubleToLongBits(other.chargedCapacity))
			return false;
		if (Double.doubleToLongBits(currentCapacity) != Double.doubleToLongBits(other.currentCapacity))
			return false;
		if (Double.doubleToLongBits(maxCapacity) != Double.doubleToLongBits(other.maxCapacity))
			return false;
		if (nonlinearCharging != other.nonlinearCharging)
			return false;
		return true;
	}


	
	
	
	
	
}
