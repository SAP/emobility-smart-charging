package com.sap.charging.model.battery;

public class BatterySimParameters {
	
	public BatteryData batteryData;

	public double initialSoC;
	public int initialStep = 0;
	
	public double capacity;
	
	public ChargeAlgorithm chargeAlgorithm;
	public double constantCurrent;
	public double constantPower;
	public double terminalVoltage;
	
	public int maxtime;
	
	private BatterySimParameters() {

	
	}
	
	
	
	
	public static BatterySimParameters buildFromBatteryData(BatteryData batteryData) {
		
		BatterySimParameters params = new BatterySimParameters();
		params.batteryData = batteryData;
		params.initialSoC = 0;
		params.initialStep = 0;
		params.chargeAlgorithm = batteryData.getDefaultChargeAlgorithm();
		params.constantCurrent = batteryData.getDefaultConstantCurrent();
		params.constantPower = batteryData.getDefaultConstantPower();
		params.capacity = batteryData.getDefaultCapacity();
		params.terminalVoltage = batteryData.getDefaultTerminalVoltage();
		params.maxtime = 24*60*60;
		
		return params;
	}
	
	
	public static BatterySimParameters buildDefaultParams() {
		
		BatteryData batteryData = new BatteryData_Sample();
		return buildFromBatteryData(batteryData);
	}
	
	@Override
	public String toString() {
		return "BatterySimParameters: initialSoC=" + initialSoC + ", capacity=" + capacity + "Ah, constantCurrent=" + constantCurrent + "A, terminalVoltage=" + 
				terminalVoltage + "V, constantPower=" + constantPower + ", initialStep=" + initialStep;
	}
	
	public BatterySimParameters copy() {
		
		BatterySimParameters params = new BatterySimParameters();
		params.batteryData = this.batteryData;
		params.initialSoC = this.initialSoC;
		params.initialStep = this.initialStep;
		params.chargeAlgorithm = this.chargeAlgorithm;
		params.constantCurrent = this.constantCurrent;
		params.constantPower = this.constantPower;
		params.capacity = this.capacity;
		params.terminalVoltage = this.terminalVoltage;
		params.maxtime = this.maxtime;
		
		return params;
	}


	
	
}
