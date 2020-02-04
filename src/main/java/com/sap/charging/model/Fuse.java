package com.sap.charging.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.util.JSONKeys;
import com.sap.charging.util.JSONSerializable;

public class Fuse implements FuseTreeNode {

	public double fusePhase1;
	public double fusePhase2;
	public double fusePhase3;
	
	@JsonIgnore
	private FuseTreeNode parent;
	private ArrayList<FuseTreeNode> children;
	
	private final int id;
	
	/**
	 * Assumption: Physically, each phase has its own fuse.
	 * For ease of modeling the tree (of the electrical system), this class contains 
	 * three fuses, assumed to be the same size. 
	 * 
	 * @param indexL h_{l,j}
	 * @param fuseSize The size of each of the three fuses in A (Amperes)
	 */
	public Fuse(int id, double fuseSize) {
		this.fusePhase1 = fuseSize;
		this.fusePhase2 = fuseSize;
		this.fusePhase3 = fuseSize;
		
		this.id = id;
		
		this.children = new ArrayList<FuseTreeNode>();
	}
	
	@JsonCreator
	public Fuse(
			@JsonProperty(value="id", required=true) int id,
			@JsonProperty(value="fusePhase1", required=true) double fusePhase1,
			@JsonProperty(value="fusePhase2", required=true) double fusePhase2,
			@JsonProperty(value="fusePhase3", required=true) double fusePhase3,
			@JsonProperty(value="children", required=true) ArrayList<FuseTreeNode> children) {
		this.fusePhase1 = fusePhase1;
		this.fusePhase2 = fusePhase2;
		this.fusePhase3 = fusePhase3;
		
		this.id = id;
		
		this.children = children; 
	}
	
	public void setFuseSize(double fuseSize) {
		this.fusePhase1 = fuseSize;
		this.fusePhase2 = fuseSize;
		this.fusePhase3 = fuseSize;
	}
	
	public int getId() {
		return this.id;
	}
	
	/**
	 * Three parameters of h_{l,j}
	 */
	@JsonIgnore
	public String getName() {
		return "l" + getId();
	}
	
	@Override
	public double getFusePhase(Phase phase) {
		if (phase == null)
			return -1;
		double result = -1;
		switch (phase) {
			case PHASE_1: result = fusePhase1; break;
			case PHASE_2: result = fusePhase2; break;
			case PHASE_3: result = fusePhase3; break;
		}
		return result;
	}
	
	/**
	 * 
	 * @param child Can be a fuse or a ChargingStation
	 */
	public void addChild(FuseTreeNode child) {
	    child.setParent(this);
		this.children.add(child);
	}
	
	public ArrayList<FuseTreeNode> getChildren() {
		return this.children;
	}
	public boolean hasChildren() {
		return this.children.size() > 0;
	}
	
	@Override
	public FuseTreeNode getParent() {
		return parent;
	}

	@Override
	public void setParent(FuseTreeNode parent) {
		this.parent = parent;
	}

	/**
	 * Searches the entire subtree for charging station children
	 * @return
	 */
	@JsonIgnore
  	public ArrayList<ChargingStation> getChargingStationChildren() {
  		ArrayList<ChargingStation> result = new ArrayList<>();
  		Stack<FuseTreeNode> stack = new Stack<>();
		stack.push(this);
		while (stack.isEmpty() == false) {
			FuseTreeNode currentNode = stack.pop();
			if (currentNode.hasChildren()) {
				for (int i=currentNode.getChildren().size()-1;i>=0;i--) {
					stack.add(currentNode.getChildren().get(i)); // Add last item first because stack is LIFO queue
				}
			}
			if (currentNode instanceof ChargingStation) {
				result.add((ChargingStation) currentNode);
			}
		}
  		return result;
  	}
  
  	/**
  	 * Gets all direct children that are charging stations
  	 * @return
  	 */
  	@JsonIgnore
 	public List<ChargingStation> getDirectChargingStationChildren() {
  		return this.getChildren().stream()
  				.filter(child -> child instanceof ChargingStation)
  				.map(node -> (ChargingStation) node)
  				.collect(Collectors.toList());
  	}
  	
  
  	@Override
	public String toString() {
		return "Fuse " + getName() + ": 3x " + this.fusePhase1 + "A";
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJSONObject() {
		JSONObject result = new JSONObject();
		result.put(JSONKeys.JSON_KEY_NAME, getName());
		result.put(JSONKeys.JSON_KEY_INDEX_L, getId());
		
		// Fuses
		result.put(JSONKeys.JSON_KEY_FUSE_PHASE_1, getFusePhase(Phase.PHASE_1));
		result.put(JSONKeys.JSON_KEY_FUSE_PHASE_2, getFusePhase(Phase.PHASE_2));
		result.put(JSONKeys.JSON_KEY_FUSE_PHASE_3, getFusePhase(Phase.PHASE_3));
		return result;
	}
	
	public static Fuse fromJSON(JSONObject o, FuseTreeChargingStationCounter counter, List<ChargingStation> chargingStations) {
		Fuse result = new Fuse(Integer.valueOf(o.get(JSONKeys.JSON_KEY_INDEX_L).toString()),
				Double.valueOf(o.get(JSONKeys.JSON_KEY_FUSE_PHASE_1).toString()));
		
		JSONArray children = (JSONArray) o.get(JSONKeys.JSON_KEY_FUSE_TREE_CHILDREN);
		for (Object child : children) {
			JSONObject childJSON = (JSONObject) child;
			String type = (String) childJSON.get(JSONKeys.JSON_KEY_FUSE_TREE_NODE_TYPE);
			FuseTreeNode childNode = null;
			if (type.equals(JSONKeys.JSON_KEY_FUSE_TREE_NODE_TYPE_CHARGING_STATION)) {
				if (counter.isLimitReached() == false) {
					int indexI = JSONSerializable.getJSONAttributeAsInt(childJSON.get(JSONKeys.JSON_KEY_INDEX_I));
					
					ChargingStation chargingStation = null;
					for (ChargingStation chargingStationLoop : chargingStations) {
						if (chargingStationLoop.getId() == indexI)
							chargingStation = chargingStationLoop;
					}
					
					if (chargingStation == null) {
						//System.out.println("Fuse::fromJSON chargingStation i=" + indexI + " not found in list, generating from JSON...");
						chargingStation = ChargingStation.fromJSON(childJSON);
						chargingStations.add(chargingStation);
					}
					
					
					childNode = chargingStation; //ChargingStation.fromJSON(childJSON);
					counter.addChargingStation();
					result.addChild(childNode);
				}
			}
			else if (type.equals(JSONKeys.JSON_KEY_FUSE_TREE_NODE_TYPE_FUSE)) {
				childNode = Fuse.fromJSON(childJSON, counter, chargingStations);
				result.addChild(childNode);
			}
		}
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((children == null) ? 0 : children.hashCode());
		long temp;
		temp = Double.doubleToLongBits(fusePhase1);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(fusePhase2);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(fusePhase3);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Fuse other = (Fuse) obj;
		if (children == null) {
			if (other.children != null)
				return false;
		} else if (!children.equals(other.children))
			return false;
		if (Double.doubleToLongBits(fusePhase1) != Double.doubleToLongBits(other.fusePhase1))
			return false;
		if (Double.doubleToLongBits(fusePhase2) != Double.doubleToLongBits(other.fusePhase2))
			return false;
		if (Double.doubleToLongBits(fusePhase3) != Double.doubleToLongBits(other.fusePhase3))
			return false;
		if (id != other.id)
			return false;
		return true;
	}

}









