package com.sap.charging.server.api.v1.store;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyUtil.Phase;

public class ChargingStationStore {

	public final int id;

	public final double fusePhase1;
	public final double fusePhase2;
	public final double fusePhase3;

	private final boolean phase1Connected;
	private final boolean phase2Connected;
	private final boolean phase3Connected;

	public final HashMap<Phase, Phase> phaseToGrid;
	public final HashMap<Phase, Phase> phaseToChargingStation;

	public ChargingStationStore(@JsonProperty(value = "id", required = true) int id,
			@JsonProperty(value = "fusePhase1", required = true) Double fusePhase1,
			@JsonProperty(value = "fusePhase2", required = true) Double fusePhase2,
			@JsonProperty(value = "fusePhase3", required = true) Double fusePhase3,
			@JsonProperty(value = "phase1Connected") Boolean phase1Connected,
			@JsonProperty(value = "phase2Connected") Boolean phase2Connected,
			@JsonProperty(value = "phase3Connected") Boolean phase3Connected,
			@JsonProperty(value = "phaseToGrid") HashMap<Phase, Phase> phaseToGrid,
			@JsonProperty(value = "phaseToChargingStation") HashMap<Phase, Phase> phaseToChargingStation) {
		this.id = id;

		this.fusePhase1 = fusePhase1;
		this.fusePhase2 = fusePhase2;
		this.fusePhase3 = fusePhase3;

		// Phase connections are optional. If they are not passed the Boolean variables
		// will be null
		this.phase1Connected = phase1Connected == null ? true : phase1Connected;
		this.phase2Connected = phase2Connected == null ? true : phase2Connected;
		this.phase3Connected = phase3Connected == null ? true : phase3Connected;

		this.phaseToGrid = phaseToGrid;
		this.phaseToChargingStation = phaseToChargingStation;
	}

	public ChargingStation toChargingStation() {
		return new ChargingStation(id, fusePhase1, fusePhase2, fusePhase3, phase1Connected, phase2Connected,
				phase3Connected, phaseToGrid, phaseToChargingStation);
	}

	public static ChargingStationStore fromChargingStation(ChargingStation station) {
		return new ChargingStationStore(station.getId(), station.fusePhase1, station.fusePhase2, station.fusePhase3,
				station.isPhase1Connected(), station.isPhase2Connected(), station.isPhase3Connected(),
				station.getPhaseToGrid(), station.getPhaseToChargingStation());
	}

}
