package com.sap.charging.model;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.util.JSONSerializable;

@JsonTypeInfo(use = NAME, include = PROPERTY)
@JsonSubTypes({ 
	@JsonSubTypes.Type(value = Fuse.class, name = "Fuse"),
	@JsonSubTypes.Type(value = ChargingStation.class, name = "ChargingStation") 
})
public interface FuseTreeNode extends JSONSerializable {

	public int getId();

	public default boolean isPhaseConnected(Phase phase) {
		switch (phase) {
		case PHASE_1: return isPhase1Connected(); 
		case PHASE_2: return isPhase2Connected(); 
		case PHASE_3: return isPhase3Connected(); 
		}
		throw new IllegalArgumentException("unknown phase=" + phase); 
	}
	public boolean isPhaseAtGridConnectedInFuseTree(Phase phase); 
	
	public boolean isPhase1Connected(); 
	public boolean isPhase2Connected(); 
	public boolean isPhase3Connected(); 
	
	public double getFusePhase(Phase phase);

	public ArrayList<FuseTreeNode> getChildren();

	@JsonIgnore
	public FuseTreeNode getParent();

	public void setParent(FuseTreeNode parent);

	public boolean hasChildren();

	/**
	 * Valid states: 
	 * phase is connected and fuseSize = 0
	 * phase is connected and fuseSize > 0
	 * phase is not connected fuseSize = 0
	 * Invalid state: 
	 * phase is NOT connected but fuseSize > 0
	 */
	public default void sanityCheckPhaseConnected() {
		for (Phase phase : Phase.values()) {
			if (isPhaseConnected(phase) == false && this.getFusePhase(phase) > 0) {
				throw new IllegalArgumentException("Error on fuseTreeNode id=" + this.getId() + ": Phase " + 
					phase + " is not connected but fuseSize=" + this.getFusePhase(phase) +
					". For consistency fuseSize should be 0 when phase is not connected."); 
			}
			
		}
	
	}
	
}


