package com.sap.charging.model;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.sap.charging.model.battery.BatteryData;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.Loggable;

public class CarFactory implements Loggable {

	@Override
	public int getVerbosity() {
		return Simulation.verbosity;
	}
	
	private final static int initialId = -1;
	private static final AtomicInteger idGen = new AtomicInteger(initialId);

	public enum CarType {
		BEV, PHEV, PETROL, DIESEL;
		
		@JsonCreator // This is the factory method and must be static
	    public static CarType fromString(String string) {
			return CarType.valueOf(string); 
	    }
	}

	public enum CarModel {
		TESLA_MODEL_S("Tesla Model S85", CarType.BEV, EnergyUtil.calculateIFromP(85, 1), // kWH
				EnergyUtil.calculateIFromP(3, 1), // min 3
				EnergyUtil.calculateIFromP(22.1, 1), 1, 1, 1, false, false, true),
		NISSAN_LEAF_2016("Nissan Leaf 2016", CarType.BEV, EnergyUtil.calculateIFromP(30, 1), // kWh
				EnergyUtil.calculateIFromP(1.4, 1), // min 1.4
				EnergyUtil.calculateIFromP(6.6, 1), 1, 0, 0, false, false, true),
		BMW_I3_2017("BMW i3 2017", CarType.BEV, 94, // Ah
				EnergyUtil.calculateIFromP(1.4, 1), // min
				EnergyUtil.calculateIFromP(11, 1), // max
				1, 1, 0, true, true, false),
		MERCEDES_GLC_350e("MERCEDES GLC 350e", CarType.PHEV, EnergyUtil.calculateIFromP(8.7, 1), // 8.7 kWh
				EnergyUtil.calculateIFromP(1, 1), // TBD: no official sources
				EnergyUtil.calculateIFromP(3.7, 1), // this is official
				1, 0, 0, false, false, true), // TBD: No official source
		RENAULT_ZOE_R240("Renault Zoe R240", CarType.BEV, 95.5, // Ah max capacity
				EnergyUtil.calculateIFromP(1, 1), // TBD: no official source. Min total current in A
				EnergyUtil.calculateIFromP(11, 1), // Max total current in A
				1, 1, 1, true, true, false),
		/*
		 * Added 08.07.2019 with new values from
		 * https://pushevs.com/2019/02/10/renault-zoe-ze-40-full-battery-specs/
		 * http://queenbattery.com.cn/index.php?controller=attachment&id_attachment=109
		 */
		RENAULT_ZOE_ZE40("Renault Zoe ZE40", CarType.BEV, 65.6, // Ah max capacity
				0, // 6A minimum defined by protocol (IEC something?). Not used so that final SoC can reach 1
				32*3, // Max total current in A, 32A per phase or 22.080kW total
				1, 1, 1, true, true, false);
		
		
		public final String modelName;
		public final CarType carType;
		public final double maxCapacity;
		public final double minCurrent;
		public final double maxCurrent;
		public final double canLoadPhase1;
		public final double canLoadPhase2;
		public final double canLoadPhase3;
		public final boolean suspendable;
		public final boolean canUseVariablePower;
		public final boolean immediateStart;

		CarModel(String modelName, CarType carType, double maxCapacity, double minCurrent, double maxCurrent,
				double canLoadPhase1, double canLoadPhase2, double canLoadPhase3, boolean suspendable,
				boolean canUseVariablePower, boolean immediateStart) {
			this.modelName = modelName;
			this.carType = carType;
			this.maxCapacity = maxCapacity;
			this.minCurrent = minCurrent;
			this.maxCurrent = maxCurrent;
			this.canLoadPhase1 = canLoadPhase1;
			this.canLoadPhase2 = canLoadPhase2;
			this.canLoadPhase3 = canLoadPhase3;
			this.suspendable = suspendable;
			this.canUseVariablePower = canUseVariablePower;
			this.immediateStart = immediateStart;
		}

		public double getSumPhases() {
			return canLoadPhase1 + canLoadPhase2 + canLoadPhase3;
		}

	}

	private int id = initialId;
	private String name;
	private String modelName;
	private CarType carType;
	private boolean[] availableSlots;
	private LocalTime timestampArrival;
	private LocalTime timestampDeparture;
	private double currentCapa;
	private double maxCapa;
	private double minLoadingState = -1;

	private double minCurrent;
	// private double minPower;

	private double maxCurrent;
	// private double maxPower;

	private boolean suspendable;
	private boolean canUseVariablePower;
	private boolean immediateStart;
	private double canLoadPhase1;
	private double canLoadPhase2;
	private double canLoadPhase3;
	private boolean nonlinearCharging;
	private BatteryData batteryData;
	
	CarFactory() {
	}

	public static CarFactory builder() {
		return new CarFactory();
	}

	public Car build() {
		if (id == initialId) {
			id = idGen.incrementAndGet();
		}
		if (name == null) {
			name = "n" + id;
		}
		/*
		 * if(minCurrent<0){ minCurrent = EnergyUtil.calculateIFromP(minPower,
		 * sumPhases()); } if(maxCurrent<0){ maxCurrent =
		 * EnergyUtil.calculateIFromP(maxPower, sumPhases()); }
		 */
		// System.out.println(currentCapa + "Ah/" + maxCapa + "Ah");

		if (suspendable == true && immediateStart == true) {
			log(1, "CarFactory::build WARNING: suspendable=" + suspendable + " and immediateStart=" + true
					+ ", which does not make sense.");
		}

		if (this.minLoadingState == -1) {
			this.minLoadingState = maxCapa / 2;
			// System.out.println("CarFactory::build Setting minLoadingState to maxCapa/2 =
			// " + this.minLoadingState);
		}

		if (currentCapa > maxCapa) {
			throw new RuntimeException("ERROR: Attemping to build car n=" + id + " with curCapacity=" + currentCapa + 2
					+ ", maxCapacity=" + maxCapa);
		}
		if (minLoadingState < 0 || minLoadingState > maxCapa) {
			throw new RuntimeException(
					"ERROR: Attemping to build car n=" + id + " with minLoadingState=" + minLoadingState);
		}
		if (currentCapa < 0) {
			throw new RuntimeException("ERROR: Attemping to build car n=" + id + " with curCapacity=" + currentCapa);
		}

		return new Car(id, name, modelName, carType, currentCapa, availableSlots, timestampArrival, timestampDeparture,
				maxCapa, minCurrent, getMinCurrentPerPhase(), maxCurrent, getMaxCurrentPerPhase(), suspendable,
				canUseVariablePower, immediateStart, minLoadingState, canLoadPhase1, canLoadPhase2, canLoadPhase3, 
				nonlinearCharging, batteryData);
	}

	public CarFactory id(int id) {
		this.id = id;
		return this;
	}

	public CarFactory modelName(String modelName) {
		this.modelName = modelName;
		return this;
	}

	public CarFactory name(String name) {
		this.name = name;
		return this;
	}

	public CarFactory phases(boolean canLoadPhase1, boolean canLoadPhase2, boolean canLoadPhase3) {
		this.canLoadPhase1 = canLoadPhase1 ? 1 : 0;
		this.canLoadPhase2 = canLoadPhase2 ? 1 : 0;
		this.canLoadPhase3 = canLoadPhase3 ? 1 : 0;
		return this;
	}

	public CarFactory phases(double canLoadPhase1, double canLoadPhase2, double canLoadPhase3) {
		this.canLoadPhase1 = canLoadPhase1;
		this.canLoadPhase2 = canLoadPhase2;
		this.canLoadPhase3 = canLoadPhase3;
		return this;
	}

	public CarFactory minCurrent(double minCurrent) {
		this.minCurrent = minCurrent;
		// this.minPower = -1;
		return this;
	}

	public CarFactory maxCurrent(double maxCurrent) {
		this.maxCurrent = maxCurrent;
		return this;
	}

	public CarFactory maxCapacity(double maxCapacity) {
		this.maxCapa = maxCapacity;
		return this;
	}

	public CarFactory currentCapacity(double currentCapacity) {
		this.currentCapa = currentCapacity;
		return this;
	}

	public CarFactory minLoadingState(double minLoadingState) {
		this.minLoadingState = minLoadingState;
		return this;
	}

	public CarFactory immediateStart(boolean immediateStart) {
		this.immediateStart = immediateStart;
		return this;
	}

	public CarFactory suspendable(boolean suspendable) {
		this.suspendable = suspendable;
		return this;
	}

	public CarFactory canUseVariablePower(boolean canUseVariablePower) {
		this.canUseVariablePower = canUseVariablePower;
		return this;
	}

	public CarFactory carType(CarType carType) {
		this.carType = carType;
		return this;
	}
	
	public CarFactory nonlinearCharging(boolean nonlinearCharging) {
		this.nonlinearCharging = nonlinearCharging;
		return this;
	}
	
	public CarFactory batteryData(BatteryData batteryData) {
		this.batteryData = batteryData;
		return this;
	}
	

	public CarFactory set(CarModel model) {
		modelName = model.modelName;
		carType = model.carType;
		maxCapa = model.maxCapacity;
		minCurrent = model.minCurrent;
		maxCurrent = model.maxCurrent;
		suspendable = model.suspendable;
		canUseVariablePower = model.canUseVariablePower;
		immediateStart = model.immediateStart;
		canLoadPhase1 = model.canLoadPhase1;
		canLoadPhase2 = model.canLoadPhase2;
		canLoadPhase3 = model.canLoadPhase3;

		return this;
	}

	private double getMinCurrentPerPhase() {
		return minCurrent / sumPhases();
	}

	private double getMaxCurrentPerPhase() {
		return maxCurrent / sumPhases();
	}

	private int sumPhases() {
		return (int) (Math.ceil(canLoadPhase1) + Math.ceil(canLoadPhase2) + Math.ceil(canLoadPhase3));
	}

	public double getMaxCapacity() {
		return maxCapa;
	}

	
	/**
	 * Start and end is inclusive: 0,1 ==> available for timeslots k=0 and k=1 0,0
	 * ==> available for timeslots k=0 1,1 ==> available for timeslots k=1
	 * 
	 * @param startAvailable (first timeslot=0) inclusive
	 * @param endAvailable   inclusive
	 * @param nTimeslots     Number of timeslots (how far are we scheduling into the
	 *                       future?)
	 */
	public CarFactory availableTimeslots(int startAvailable, int endAvailable, int nTimeslots) {
		// startAvailable and endAvailable are 0-indexed

		availableSlots = new boolean[nTimeslots];
		for (int k = 0; k < nTimeslots; k++) {
			availableSlots[k] = (k >= startAvailable && k <= endAvailable) ? true : false;
		}

		boolean warning = false;
		if (endAvailable >= nTimeslots) {
			log(1, "WARNING: Car availability (kStart=" + startAvailable
					+ ",kEnd=" + endAvailable + ") is longer than nTimeslots (" + nTimeslots + ")");
			warning = true;
		}
		if (startAvailable > endAvailable) {
			log(1, "WARNING: Car availability (kStart=" + startAvailable
					+ "kEnd," + endAvailable + ") is invalid, id=" + id);
			warning = true;
		}

		if (warning == true) {
			log(1, "CarFactory::availableTimeslots " + Arrays.toString(availableSlots));
		}

		return this;
	}

	public CarFactory availableTimestamps(LocalTime timestampArrival, LocalTime timestampDeparture) {
		if (timestampArrival.isAfter(timestampDeparture)) {
			throw new RuntimeException("ERROR: Attempting to build car n=" + id + " with timestampArrival="
					+ timestampArrival.toSecondOfDay() + ", timestampDeparture=" + timestampDeparture.toSecondOfDay());
		}

		this.timestampArrival = timestampArrival;
		this.timestampDeparture = timestampDeparture;
		return this;
	}

}
