package com.sap.charging.model;

import com.sap.charging.model.EnergyUtil.Phase;

public class ChargingStationFactory {
	
	public enum Standard {
		/**
		 *  Currently the wallbox used at sap. Type 2, 1-phase=7.4kw, 3-phase=22kw
		 */
		KeContact_P30 {
			@Override
			public void applyToChargingStation(ChargingStation instance) {
				instance.fusePhase1 = 32;
				instance.fusePhase2 = 32;
				instance.fusePhase3 = 32;
				
				instance.isBEVAllowed = true;
				instance.isPHEVAllowed = true;
			}
		};
		
		public abstract void applyToChargingStation(ChargingStation instance);
	}

	private ChargingStation instance;
	
	public ChargingStationFactory() {
		this.instance = new ChargingStation();
	}
	
	public static ChargingStationFactory builder() {
		return new ChargingStationFactory();
	}
	
	public ChargingStationFactory fusePhases(double fusePhase1,
			double fusePhase2, double fusePhase3) {
		instance.fusePhase1 = fusePhase1;
		instance.fusePhase2 = fusePhase2;
		instance.fusePhase3 = fusePhase3;
		return this;
	}
	
	public ChargingStationFactory isBEVAllowed(boolean allowed) {
		instance.isBEVAllowed = allowed;
		return this;
	}
	public ChargingStationFactory isPHEVAllowed(boolean allowed) {
		instance.isPHEVAllowed = allowed;
		return this;
	}
	
	public ChargingStationFactory buildFromStandard(Standard standard) {
		standard.applyToChargingStation(instance);
		return this;
	}
	
	public ChargingStationFactory setIndexI(int indexI) {
		this.instance.setID(indexI);
		return this;
	}
	
	public ChargingStationFactory setPhaseMatching(Phase phase1Consumed, Phase phase2Consumed, Phase phase3Consumed) {
		this.instance.setPhaseMatching(phase1Consumed, phase2Consumed, phase3Consumed);
		return this;
	}
	
	public ChargingStation build() {
		return instance;
	}
}
