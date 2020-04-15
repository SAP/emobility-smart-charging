package com.sap.charging.model;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.util.JSONKeys;

/**
 * Also: Loading Spot, Charging Spot, ... Currently, all fuses directly at a
 * charging station can hold 32A
 */

public class ChargingStation implements FuseTreeNode {

	public enum StationStatus {
		Free /* The ChargingStation is free and can be used by the next incoming car */,
		Charging /* The station is occupied with an ongoing charge session. */,
		Reserved /*
					 * The charge station is currently reserved for a cetain car and not available
					 * to everyone.
					 */, Blocked /*
									 * The charge station is blocked, either because a charging session is over or
									 * because a different car parked there.
									 */,
		Maintenance /* Charge station is in maintenance mode. No charge sessions possible. */,
		Disconnected /*
						 * The connection to the charge station was lost and therefore no information is
						 * available.
						 */
	};

	/**
	 * Name/ID of charging station (i)
	 */
	private int id;

	/**
	 * Each charging station phase may not consume the phase that is used by a car.
	 * Example: chargingStation i0: 1->1; 2->2; 3->3 chargingStation i1: 1->2; 2->3;
	 * 3->1 chargingStation i2: 1->3; 2->1; 3->2 chargingStation i3: 1->1; 2->2;
	 * 3->3 ... ("Phasenwanderung")
	 */
	private HashMap<Phase, Phase> phaseToGrid;

	/**
	 * Reverse of phaseToGrid
	 */
	private HashMap<Phase, Phase> phaseToChargingStation;

	/**
	 * Maximum current the fuse will hold (in amps) for phase j=1
	 */
	public double fusePhase1 = Double.MIN_VALUE;
	/**
	 * Maximum current the fuse will hold (in amps) for phase j=2
	 */
	public double fusePhase2 = Double.MIN_VALUE;
	/**
	 * Maximum current the fuse will hold (in amps) for phase j=3
	 */
	public double fusePhase3 = Double.MIN_VALUE;

	private final boolean phase1Connected;
	private final boolean phase2Connected;
	private final boolean phase3Connected;

	public boolean isBEVAllowed;
	public boolean isPHEVAllowed;

	public StationStatus status;

	@JsonIgnore
	private FuseTreeNode parent;

	@JsonCreator
	public ChargingStation(@JsonProperty(value = "id", required = true) int id,
			@JsonProperty("fusePhase1") double fusePhase1, @JsonProperty("fusePhase2") double fusePhase2,
			@JsonProperty("fusePhase3") double fusePhase3,
			@JsonProperty(value = "phase1Connected") Boolean phase1Connected,
			@JsonProperty(value = "phase2Connected") Boolean phase2Connected,
			@JsonProperty(value = "phase3Connected") Boolean phase3Connected,
			@JsonProperty("phaseToGrid") HashMap<Phase, Phase> phaseToGrid,
			@JsonProperty("phaseToChargingStation") HashMap<Phase, Phase> phaseToChargingStation) {
		this.id = id;
		this.fusePhase1 = fusePhase1;
		this.fusePhase2 = fusePhase2;
		this.fusePhase3 = fusePhase3;

		// Phase connections are optional. If they are not passed the Boolean variables will be null
		this.phase1Connected = phase1Connected == null ? true : phase1Connected;
		this.phase2Connected = phase2Connected == null ? true : phase2Connected;
		this.phase3Connected = phase3Connected == null ? true : phase3Connected;

		this.sanityCheckPhaseConnected();

		this.isBEVAllowed = true;
		this.isPHEVAllowed = true;
		status = StationStatus.Free;

		this.sanityCheckPhaseMatching(phaseToGrid, phaseToChargingStation);

		// If neither parameter is passed, use default phases (1:1, 2:2, 3:3)
		if (phaseToGrid == null && phaseToChargingStation == null) {
			this.setPhaseMatching(Phase.PHASE_1, Phase.PHASE_2, Phase.PHASE_3);
		} else {
			this.phaseToGrid = phaseToGrid;
			this.phaseToChargingStation = phaseToChargingStation;
		}

	}

	private void sanityCheckPhaseMatching(HashMap<Phase, Phase> phaseToGrid,
			HashMap<Phase, Phase> phaseToChargingStation) {
		if ((phaseToGrid == null && phaseToChargingStation != null)
				|| (phaseToGrid != null && phaseToChargingStation == null)) {
			throw new RuntimeException(
					"chargingStation.phaseToGrid and chargingStation.phaseToChargingStation must either both be null or both set as parameters.");
		}
	}

	public ChargingStation() {
		status = StationStatus.Free;

		this.phase1Connected = true;
		this.phase2Connected = true;
		this.phase3Connected = true;
	}

	@Override
	public double getFusePhase(Phase phase) {
		switch (phase) {
		case PHASE_1:
			return fusePhase1;
		case PHASE_2:
			return fusePhase2;
		case PHASE_3:
			return fusePhase3;
		}
		return -1;
	}

	public double getFusePhase(int phase) {
		return getFusePhase(Phase.getByInt(phase));
	}

	@Override
	public boolean isPhase1Connected() {
		return this.phase1Connected;
	}

	@Override
	public boolean isPhase2Connected() {
		return this.phase2Connected;
	}

	@Override
	public boolean isPhase3Connected() {
		return this.phase3Connected;
	}

	@JsonIgnore
	public String getName() {
		return "i" + this.id;
	}

	public void setID(int id) {
		this.id = id;
	}

	public int getId() {
		return this.id;
	}

	public HashMap<Phase, Phase> getPhaseToGrid() {
		return this.phaseToGrid;
	}

	public HashMap<Phase, Phase> getPhaseToChargingStation() {
		return this.phaseToChargingStation;
	}

	public void setPhaseMatching(Phase phase1Grid, Phase phase2Grid, Phase phase3Grid) {
		phaseToGrid = new HashMap<>();
		phaseToGrid.put(Phase.PHASE_1, phase1Grid);
		phaseToGrid.put(Phase.PHASE_2, phase2Grid);
		phaseToGrid.put(Phase.PHASE_3, phase3Grid);

		phaseToChargingStation = new HashMap<>();
		phaseToChargingStation.put(phase1Grid, Phase.PHASE_1);
		phaseToChargingStation.put(phase2Grid, Phase.PHASE_2);
		phaseToChargingStation.put(phase3Grid, Phase.PHASE_3);
	}

	public Phase getPhaseConsumed(Phase phaseAtChargingStation) {
		return phaseToGrid.get(phaseAtChargingStation);
	}

	public Phase getPhaseConsumed(int phaseAtChargingStation) {
		return phaseToGrid.get(Phase.getByInt(phaseAtChargingStation));
	}

	/**
	 * 
	 * @param actualPhase
	 * @return
	 */
	public Phase getPhaseGridToChargingStation(Phase phaseGrid) {
		return phaseToChargingStation.get(phaseGrid);
	}

	@Override
	public String toString() {
		String result = "ChargingStation i" + getId() + ": 3x " + fusePhase1 + "A. ";
		if (phaseToGrid != null) {
			result += "Matching: " + "1->" + phaseToGrid.get(Phase.PHASE_1).asInt() + "; " + "2->"
					+ phaseToGrid.get(Phase.PHASE_2).asInt() + "; " + "3->" + phaseToGrid.get(Phase.PHASE_3).asInt()
					+ ", ";
		}
		result += "isBEVAllowed=" + isBEVAllowed + ", " + "isPHEVAllowed=" + isPHEVAllowed;
		return result;
	}

	@Override
	public ArrayList<FuseTreeNode> getChildren() {
		return null;
	}

	@Override
	public boolean hasChildren() {
		return false;
	}

	@Override
	public FuseTreeNode getParent() {
		return parent;
	}

	@Override
	public void setParent(FuseTreeNode parent) {
		this.parent = parent;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJSONObject() {
		JSONObject result = new JSONObject();
		result.put(JSONKeys.JSON_KEY_NAME, getName());
		result.put(JSONKeys.JSON_KEY_INDEX_I, getId());

		// Fuses
		result.put(JSONKeys.JSON_KEY_FUSE_PHASE_1, getFusePhase(Phase.PHASE_1));
		result.put(JSONKeys.JSON_KEY_FUSE_PHASE_2, getFusePhase(Phase.PHASE_2));
		result.put(JSONKeys.JSON_KEY_FUSE_PHASE_3, getFusePhase(Phase.PHASE_3));

		// Phase Matching
		JSONObject phaseMatching = new JSONObject();
		phaseMatching.put(JSONKeys.JSON_KEY_PHASE_1, getPhaseConsumed(Phase.PHASE_1).asStringConst());
		phaseMatching.put(JSONKeys.JSON_KEY_PHASE_2, getPhaseConsumed(Phase.PHASE_2).asStringConst());
		phaseMatching.put(JSONKeys.JSON_KEY_PHASE_3, getPhaseConsumed(Phase.PHASE_3).asStringConst());
		result.put(JSONKeys.JSON_KEY_PHASE_MATCHING, phaseMatching);

		// Are BEV/PHEV allowed?
		result.put(JSONKeys.JSON_KEY_IS_BEV_ALLOWED, isBEVAllowed);
		result.put(JSONKeys.JSON_KEY_IS_PHEV_ALLOWED, isPHEVAllowed);

		// Fuse tree node type for from json
		result.put(JSONKeys.JSON_KEY_FUSE_TREE_NODE_TYPE, JSONKeys.JSON_KEY_FUSE_TREE_NODE_TYPE_CHARGING_STATION);

		return result;
	}

	public static ChargingStation fromJSON(JSONObject o) {
		ChargingStationFactory factory = ChargingStationFactory.builder()
				.setIndexI(Integer.valueOf(o.get(JSONKeys.JSON_KEY_INDEX_I).toString()))
				.isBEVAllowed((boolean) o.get(JSONKeys.JSON_KEY_IS_BEV_ALLOWED))
				.isPHEVAllowed((boolean) o.get(JSONKeys.JSON_KEY_IS_PHEV_ALLOWED))
				.fusePhases(Double.valueOf(o.get(JSONKeys.JSON_KEY_FUSE_PHASE_1).toString()),
						Double.valueOf(o.get(JSONKeys.JSON_KEY_FUSE_PHASE_2).toString()),
						Double.valueOf(o.get(JSONKeys.JSON_KEY_FUSE_PHASE_3).toString()));

		JSONObject phaseMatching = (JSONObject) o.get(JSONKeys.JSON_KEY_PHASE_MATCHING);
		Phase p1 = Phase.getByString((String) phaseMatching.get(JSONKeys.JSON_KEY_PHASE_1));
		Phase p2 = Phase.getByString((String) phaseMatching.get(JSONKeys.JSON_KEY_PHASE_2));
		Phase p3 = Phase.getByString((String) phaseMatching.get(JSONKeys.JSON_KEY_PHASE_3));

		return factory.setPhaseMatching(p1, p2, p3).build();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChargingStation other = (ChargingStation) obj;
		if (Double.doubleToLongBits(fusePhase1) != Double.doubleToLongBits(other.fusePhase1))
			return false;
		if (Double.doubleToLongBits(fusePhase2) != Double.doubleToLongBits(other.fusePhase2))
			return false;
		if (Double.doubleToLongBits(fusePhase3) != Double.doubleToLongBits(other.fusePhase3))
			return false;
		if (id != other.id)
			return false;
		if (isBEVAllowed != other.isBEVAllowed)
			return false;
		if (isPHEVAllowed != other.isPHEVAllowed)
			return false;
		if (phaseToChargingStation == null) {
			if (other.phaseToChargingStation != null)
				return false;
		} else if (!phaseToChargingStation.equals(other.phaseToChargingStation))
			return false;
		if (phaseToGrid == null) {
			if (other.phaseToGrid != null)
				return false;
		} else if (!phaseToGrid.equals(other.phaseToGrid))
			return false;
		if (status != other.status)
			return false;
		return true;
	}

}
