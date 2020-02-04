package com.sap.charging.model.battery;

/**
 *
 */
public class BatteryData_Sample extends BatteryData {

	
	private final double[] ocv = new double[] {
			3.167, 3.413, 3.446, 3.488, 3.537, 3.571, 3.593, 3.610, 3.625, 3.642, 
		    3.663, 3.695, 3.755, 3.799, 3.846, 3.895, 3.945, 3.997, 4.051, 4.108,
		    4.166
	};
	
	/** 
	 * SoC values measured along ocv (as it may not be equidistant)
	 */
	private final double[] ocv_soc = new double[] {
			0,0.05,0.1,0.15,0.2,0.25,0.3,0.35,0.4,0.45,0.5,0.55,0.6,0.65,0.7,0.75,0.8,0.85,0.9,0.95,1
	};
	
	
	private final double defaultConstantCurrent = 32*3;
	private final double defaultConstantPower = 114.6*3;
	private final double defaultCapacity = 65.6;
	
	// Unused, u-shaped R depending on SOC
	// paste0(interp1(. 
	// Original data is for soc 0, 0.05, 0.1, 0.2, 0.3, ...
	// Linear interpolate 0.15, 0.25, ... so we have equidistant soc array (for faster interp1 without binary search)
	private final double resistance[] = new double[] {
			//0.00335, 0.00212, 0.00196, 0.00184, 0.00166, 0.00160, 0.00164, 0.00168, 0.00171, 0.00172, 0.00173, 0.00175, 0.00186
			0.00335, 0.00212, 0.00196, 0.0019, 0.00184, 0.00175, 0.00166, 0.00163, 0.0016, 0.00162, 0.00164, 0.00166, 0.00168, 0.001695, 0.00171, 0.001715, 0.00172, 0.001725, 0.00173, 0.00175, 0.00186
	};
	
	// paste0((0:20)/20, collapse=", ")
	private final double resistance_soc[] = new double[] {
			0, 0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9, 0.95, 1
	};
	
	
	@SuppressWarnings("unused")
	private final double defaultR_Const = 0.005;
	
	private final ChargeAlgorithm defaultChargeAlgorithm = ChargeAlgorithm.CCCV;
	private final double defaultTerminalVoltage = ocv[ocv.length-1];
	
	@Override
	public double[] getOCVArray() {
		return ocv;
	}
	
	@Override
	public double[] getOCV_SOCArray() {
		return ocv_soc;
	}
	
	@Override
	public double[] getResistanceArray() {
		return resistance;
	}
	
	@Override
	public double[] getResistance_SOCArray() {
		return resistance_soc;
	}
	
	
	
	@Override
	public double getDefaultConstantCurrent() {
		return defaultConstantCurrent;
	}

	@Override
	public double getDefaultConstantPower() {
		return defaultConstantPower;
	}

	@Override
	public ChargeAlgorithm getDefaultChargeAlgorithm() {
		return defaultChargeAlgorithm;
	}


	@Override
	public double getDefaultCapacity() {
		return defaultCapacity;
	}

	@Override
	public double getDefaultTerminalVoltage() {
		return defaultTerminalVoltage;
	}
	
	
	
	
	
	
}
