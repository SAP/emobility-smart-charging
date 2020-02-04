package com.sap.charging.model.battery;

import com.sap.charging.model.Car;
import com.sap.charging.model.EnergyUtil;
import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.TimeUtil;

/**
 * Resistor-only (R-only) equivalent circuit model of battery CCCV or CPCV
 * charging
 *
 */
public class BatterySim {

	public static int nSimulationSteps = 0;
	public static int nSimulations = 0;
	
	private final BatterySimParameters params;

	private int step = 0;
	
	private double chargedAh;
	private double soc;
	private double nextCurrent;
	
	private final boolean enableEfficiency;
	private final boolean enableStore;
	private double[] storeSOC;
	private double[] storeCurrent;
	private double[] storeTerminalVoltage;
	private double[] storeOpenCircuitVoltage;
	private double[] storePower;
	private double[] storeR0;
	
	
	public BatterySim(BatterySimParameters params) {	
		this(params, false, true);
	}
	
	// For performance reasons, give option to not store historical values
	public BatterySim(BatterySimParameters params, boolean enableStore, boolean enableEfficiency) {
		this.params = params;
		this.enableStore = enableStore;
		this.enableEfficiency = enableEfficiency;
		
		if (params.initialSoC > 1 || params.initialSoC < 0) {
			throw new RuntimeException("initialSoC=" + params.initialSoC);
		}
		
		this.soc = params.initialSoC;
		this.step = params.initialStep;
		
		if (enableStore == true) {
			this.storeSOC = new double[params.maxtime];
			this.storeCurrent = new double[params.maxtime];
			this.storeTerminalVoltage = new double[params.maxtime];
			this.storeOpenCircuitVoltage = new double[params.maxtime];
			this.storePower = new double[params.maxtime];
			this.storeR0 = new double[params.maxtime];
		}
		
		nSimulations++;
		
	}
	
	/**
	 * Copies while holding state (soc, nextCurrent)
	 * store is NOT copied
	 * @return
	 */
	/*public BatterySim copy() {
		BatterySim batterySim = new BatterySim(this.params.copy(), this.enableStore, this.enableEfficiency);
		batterySim.
		
		return batterySim;
	}*/
	
	public BatterySimParameters getBatterySimParameters() {
		return params;
	}
	
	/**
	 * Stateless function. Does NOT move forward simulation
	 * 
	 * @return Current
	 */
	public double getCurrentBasedOnSoC(double soc, double maxCurrentAllowed) {

		double r0 = params.batteryData.getResistanceFromSOC(soc);
		double v_ocv = params.batteryData.getOCVFromSOC(soc); // Open circuit voltage which depends on SoC
		double maxV = params.terminalVoltage;

		double ik; // Cell current
		if (params.chargeAlgorithm == ChargeAlgorithm.CCCV) {

			ik = (v_ocv - maxV) / r0; // compute test ik to achieve maxV
			ik = Math.max(-params.constantCurrent, ik); // but limit current to no more than CC in mag.

		} else {
			// CPCV
			ik = (v_ocv - Math.sqrt(Math.pow(v_ocv, 2) - 4 * r0 * (-params.constantPower))) / (2 * r0);
			if (v_ocv - ik * r0 > maxV) { // too much!
				ik = (v_ocv - maxV) / r0; // do CV instead
			}
		}
		
		// Check chargePlans. If infrastructure says to charge with less than this must be taken into account
		ik = Math.max(ik, -maxCurrentAllowed);
		
		if (enableStore) {
			this.storeSOC[step] = soc;
			this.storeCurrent[step] = -ik;
			this.storeTerminalVoltage[step] = v_ocv - ik*r0;
			this.storeOpenCircuitVoltage[step] = v_ocv;
			this.storePower[step] = - ik * this.storeTerminalVoltage[step];
			this.storeR0[step] = r0;
		}
		
		BatterySim.nSimulationSteps++;
		
		return -ik;
	}
	
	/**
	 * Each step is one second
	 * 
	 * Soc is updated after applying this current for one second
	 * 
	 * @param soc
	 * @param maxCurrentAllowed
	 * @return Current at START of second
	 */
	public void simulateNextStep(double maxCurrentAllowed) {
		
		double current = getCurrentBasedOnSoC(this.soc, maxCurrentAllowed);
		//this.lastCurrent = current;
		
		this.step++;
		
		double chargedAhDelta = (enableEfficiency) ? CONSTANTS.CHARGING_EFFICIENCY*EnergyUtil.getAmpereHours(1, current) : EnergyUtil.getAmpereHours(1, current);
		double chargedSoCDelta = chargedAhDelta / params.capacity;
		
		this.chargedAh += chargedAhDelta;
		this.soc += chargedSoCDelta;
		
		//return current;

	}
	
	/**
	 * Each step is one second
	 * 
	 * Given static maxCurrentAllowed: How much SoC did we have at previous timestep?
	 * 
	 * Will be a bit imprecise because:
	 *	z[k+1] = z[k] + i[k]/Q
	 *	z[k] = z[k+1] - i[k]/Q
	 *	Second equation would be correct. Instead what we do: 
	 *  z[k-1] = z[k] - i[k]/Q 
	 *	Because we don't know i[k-1]
	 * 
	 * @return
	 */
	public void simulatePreviousStep(double maxCurrentAllowed) {
		
		double current = getCurrentBasedOnSoC(this.soc, maxCurrentAllowed);
		this.nextCurrent = current;
		
		this.step--;
		
		double chargedAhDelta = (enableEfficiency) ? CONSTANTS.CHARGING_EFFICIENCY*EnergyUtil.getAmpereHours(1, current) : EnergyUtil.getAmpereHours(1, current);
		double chargedSoCDelta = chargedAhDelta / params.capacity;
		
		this.chargedAh -= chargedAhDelta;
		this.soc -= chargedSoCDelta;
		
	}
	
	
	/**
	 * Simulate with given chargePlan from startTimeSeconds until endTimeSeconds (exlusive)
	 * 
	 * Simulation is stateful, i.e. getSoC() can be called and it can be continued
	 * 
	 * Last timeslot of charge plan is taken into account
	 * 
	 * Noop if startTimeSeconds=endTimeSeconds
	 * @param startTimeSeconds Parallel simulation: used to get index of chargePlan
	 * @param chargePlan -1 values will be ignored (regarded as null and not yet set). Is used in place of car.getCurrentPlan()
	 * @param maxCurrentAllowedStatic min of charging station and car.maxcurrent
	 */
	public void simulate(int startTimeSeconds, int endTimeSeconds, Car car, double maxCurrentAllowedStatic) {
		simulate(startTimeSeconds, endTimeSeconds, car, maxCurrentAllowedStatic, -1);
	}
	
	
	/**
	 *
	 * @param startTimeSeconds
	 * @param endTimeSeconds
	 * @param car
	 * @param maxCurrentAllowedStatic
	 * @param ignoreChargePlanTimeslot Ignore certain timeslot in charge plan. Pass a negative number if all charge plan timeslots should be taken into account
	 */
	public void simulate(int startTimeSeconds, int endTimeSeconds, Car car, double maxCurrentAllowedStatic, int ignoreChargePlanTimeslot) {
		if (step > startTimeSeconds) {
			// We have already simulated this step 
			// Example: Step = 200, startTimeSeconds=100
			throw new RuntimeException("We have already simulated step=" + step + ", with startTimeSeconds=" + startTimeSeconds);
		}
		if (step != 0 && startTimeSeconds > step) {
			// We are not continuing simulation 
			// Example: step = 100, startTimeSeconds=200 (skipping 100s)
			throw new RuntimeException("StartTimeSeconds=" + startTimeSeconds + " should be equal to step=" + step + " to avoid holes in simulation");
		}
		
		this.step = startTimeSeconds;
		for (int step=startTimeSeconds; step<endTimeSeconds; step++) {
			
			int timeslot = TimeUtil.getTimeslotFromSeconds(step);
			
			double maxCurrentAllowed = maxCurrentAllowedStatic;
			if (car.getCurrentPlan() != null && car.getCurrentPlan()[timeslot] >= 0 && timeslot != ignoreChargePlanTimeslot) {
				maxCurrentAllowed = Math.min(maxCurrentAllowedStatic, car.getCurrentPlan()[timeslot]*car.sumUsedPhases);
			}
			
			
			this.simulateNextStep(maxCurrentAllowed);
		}
		
		this.nextCurrent = this.getCurrentBasedOnSoC(this.getSoC(), maxCurrentAllowedStatic);
	}
	
	
	public void simulateBackwards(double maxCurrentAllowedStatic) {
		
	}
	
	
	
	
	/**
	 * Returns chargedAh at END of the second (after applying getLastCurrent() for 1s)
	 * @return
	 */
	public double getChargedAh() {
		return this.chargedAh;
	}
	
	
	/**
	 * Returns the soc at the END of the second (after applying getLastCurrent() for 1s)
	 * @return
	 */
	public double getSoC() {
		return this.soc;
	}
	
	/**
	 * Returns the current used at the START of the second
	 * 
	 * If current was never set, calculate an initial value
	 * @return
	 */
	/*public double getLastCurrent(double maxCurrentAllowed) {
		if (Double.isNaN(this.lastCurrent)) {
			return this.getCurrentBasedOnSoC(this.soc, maxCurrentAllowed);
		}
		else {
			return this.lastCurrent;
		}
	}*/
	
	/**
	 * Current that will be used to charge during the next second 
	 * INDEPENDANT OF charge schedules, only maxCurrentAllowedStatic used during last simulate() call
	 * @return
	 */
	public double getNextCurrent() {
		return this.nextCurrent;
	}
		
	
	
	
	public static BatterySim createBatterySimFromCar(Car car) {
		return createBatterySimFromCar(car, 24*60*60); // 86400 seconds per day
	}
	
	public static BatterySim createBatterySimFromCar(Car car, int maxtime) {
		double soc = car.carBattery.getSoC(); // Initial SOC
		
		BatterySimParameters params = car.carBattery.batterySimParams.copy();
		params.initialSoC = soc;
		params.maxtime = maxtime;
		BatterySim sim = new BatterySim(params, false, true);
		
		return sim;
	}
	
	
	
	
	public static void main(String[] args) {
		BatterySimParameters params = BatterySimParameters.buildDefaultParams();
		
		BatterySim sim = new BatterySim(params);
		System.out.println(sim.getCurrentBasedOnSoC(0.9, 96));
		
	}
	
	public void writeResultsToCSV(String path) {
		if (enableStore == false) {
			throw new RuntimeException("Please use constructor BatterySim(params, true) to enable storage.");
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("step; soc; current; terminalVoltage; openCircuitVoltage; power; r0\n");
		
		for (int i=0;i<storeSOC.length;i++) {
			sb.append(i + ";");
			sb.append(storeSOC[i] + ";");
			sb.append(storeCurrent[i] + ";");
			sb.append(storeTerminalVoltage[i] + ";");
			sb.append(storeOpenCircuitVoltage[i] + ";");
			sb.append(storePower[i] + ";");		
			sb.append(storeR0[i] + "\n");
		}
		
		FileIO.writeFile(path, sb.toString());		
	}

}









