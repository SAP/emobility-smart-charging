package com.sap.charging.model;

import com.sap.charging.util.JSONKeys;

public class EnergyUtil {
	
	public enum Phase {
		PHASE_1(1, JSONKeys.JSON_KEY_PHASE_1),
		PHASE_2(2, JSONKeys.JSON_KEY_PHASE_2),
		PHASE_3(3, JSONKeys.JSON_KEY_PHASE_3);
		
		private int value;
		private String stringConst;
		
		Phase(int value, String stringConst) {
			this.value = value;
			this.stringConst = stringConst;
		}
		
		public static Phase getByInt(int phase) {
			switch (phase) {
			case 1: return PHASE_1;
			case 2: return PHASE_2;
			case 3: return PHASE_3;
			default: 
				throw new IllegalArgumentException("EnergyUtil::getByInt ERROR: Attempted to retrieve phase by int: " + phase);
			}
		}
		public static Phase getByString(String name) {
			switch (name) {
			case JSONKeys.JSON_KEY_PHASE_1: return PHASE_1;
			case JSONKeys.JSON_KEY_PHASE_2: return PHASE_2;
			case JSONKeys.JSON_KEY_PHASE_3: return PHASE_3;
			default: 
				System.out.println("EnergyUtil::getByString ERROR: Attempted to retrieve phase by string: " + name);
				throw new IllegalArgumentException();
			}

		}
		public int asInt() {
			return this.value;
		}
		public String asStringConst() {
			return this.stringConst;
		}
	}
	
	
	
	/**
	 * Assumes a 400V system (overall) or 230V for each conductor.
	 * Example: 22kW/(3*230V) = 32A  
	 * Calculation is:
	 * (1000*p)/(numberPhasesUsed*230)
	 * @param p The power (Leistung) in kw
	 * @param numberPhasesUsed How many phases are being used? 1, 2 or 3
	 * @return I (current) in Amperes
	 */
	public static double calculateIFromP(double p, int numberPhasesUsed) {
		return (1000*p)/(numberPhasesUsed*230);
	}

	/**
	 * Example: 3*400V*32A/sqrt(3) = 22kW
	 * @param i
	 * @param numberPhasesUsed
	 * @return P (power) in kW
	 */
	public static double calculatePFromI(double i, int numberPhasesUsed) {
		return numberPhasesUsed*230* i / 1000;
	}
	

	/**
	 * How much was charged over the given amount of seconds?
	 * @param deltaSeconds
	 * @param current
	 * @return
	 */
	public static double getAmpereHours(int deltaSeconds, double current) {
		return current*deltaSeconds / 3600.0;
	}
	
	
}
