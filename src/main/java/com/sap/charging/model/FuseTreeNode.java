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

	public double getFusePhase(Phase phase);

	public ArrayList<FuseTreeNode> getChildren();

	@JsonIgnore
	public FuseTreeNode getParent();

	public void setParent(FuseTreeNode parent);

	public boolean hasChildren();

}
