package com.sap.charging.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.charging.util.Callback;
import com.sap.charging.util.JSONKeys;
import com.sap.charging.util.JSONSerializable;

public class FuseTree implements JSONSerializable {

	private final int numberChargingStationsBottomLevel;
	
	@JsonIgnore
	private List<ChargingStation> listOfChargingStations;
	

	private final Fuse rootFuse;
	
	/**
	 * Tree of pre-fuses (e.g. 800A/1250A/4000A) and fuses at charging stations (e.g. 32A)
	 * NOTE: rootFuse must already have all children added.
	 */
	@JsonCreator
	public FuseTree(
			@JsonProperty(value="rootFuse", required=true) Fuse rootFuse, 
			@JsonProperty("numberChargingStationsBottomLevel") int numberChargingStationsBottomLevel) {
		this.rootFuse = rootFuse;
		this.numberChargingStationsBottomLevel = numberChargingStationsBottomLevel;
		refreshListOfChargingStations();
	}
	
	public Fuse getRootFuse() {
		return this.rootFuse;
	}
	
	public FuseTree getSubtree(Fuse newRootFuse) {
		return new FuseTree(newRootFuse, this.numberChargingStationsBottomLevel);
	}
	
	public int getNumberChargingStationsBottomLevel() {
		return numberChargingStationsBottomLevel;
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		traverseTree((item) ->  sb.append(item.toString() + "\n"));
		return sb.toString();
	}
	
	
	
	public void refreshListOfChargingStations() {
		listOfChargingStations = new ArrayList<ChargingStation>();
		traverseTree(new Callback<FuseTreeNode>() {
			public void callback(FuseTreeNode item) {
				if (item instanceof ChargingStation)
					listOfChargingStations.add((ChargingStation) item);
			}
		});
	}
	
	/**
	 * Gets a flat list of charging stations
	 * @return
	 */
	@JsonIgnore
	public List<ChargingStation> getListOfChargingStations() {
		return listOfChargingStations;
	}
	
	
	/**
	 * Traverse the tree in an iterative fashion, via a stack
	 * http://www.geeksforgeeks.org/inorder-tree-traversal-without-recursion/
	 * @param callback
	 */
	public void traverseTree(Callback<FuseTreeNode> callback) {
		Stack<FuseTreeNode> stack = new Stack<>();
		stack.push(this.rootFuse);
		while (stack.isEmpty() == false) {
			FuseTreeNode currentNode = stack.pop();
			if (currentNode.hasChildren()) {
				for (int i=currentNode.getChildren().size()-1;i>=0;i--) {
					stack.add(currentNode.getChildren().get(i)); // Add last item first because stack is LIFO queue
				}
			}
			callback.callback(currentNode);
		}
	}

	
	
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJSONObject() {
		JSONObject result = new JSONObject();
		JSONArray currentParentArray = new JSONArray();
		getJSONTree(this.rootFuse, currentParentArray);
		result.put(JSONKeys.JSON_KEY_FUSE_TREE_ROOT, currentParentArray);
		result.put(JSONKeys.JSON_KEY_FUSE_TREE_NUMBER_CHARGING_STATIONS_BOTTOM_LEVEL, this.numberChargingStationsBottomLevel);
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private void getJSONTree(FuseTreeNode currentNode, JSONArray currentParentArray) {
		/*JSONObject currentNodeJSON = null;
		if (currentNode instanceof Fuse) {
			currentNodeJSON = currentNode.toJSONObject();
		}
		if (currentNode instanceof ChargingStation) {
			currentNodeJSON = new JSONObject();
			currentNodeJSON.put(JSONKeys.JSON_KEY_INDEX_I, ((ChargingStation) currentNode).getIndexI());
			currentNodeJSON.put(JSONKeys.JSON_KEY_FUSE_TREE_NODE_TYPE, JSONKeys.JSON_KEY_FUSE_TREE_NODE_TYPE_CHARGING_STATION);
		}*/
		JSONObject currentNodeJSON = currentNode.toJSONObject();
		
		currentParentArray.add(currentNodeJSON);
		if (currentNode instanceof Fuse) {
			currentNodeJSON.put(JSONKeys.JSON_KEY_FUSE_TREE_NODE_TYPE, JSONKeys.JSON_KEY_FUSE_TREE_NODE_TYPE_FUSE);
			// Children
			currentParentArray = new JSONArray();
			currentNodeJSON.put(JSONKeys.JSON_KEY_FUSE_TREE_CHILDREN, currentParentArray);
			for (FuseTreeNode child : currentNode.getChildren()) {
				getJSONTree(child, currentParentArray);
			}
		}
	}
	
	/**
	 * 
	 * @param json
	 * @param limitNumberChargingStations Optionally, pass 0 for no limit
	 * @return
	 */
	public static FuseTree fromJSON(JSONObject json, int limitNumberChargingStations, List<ChargingStation> chargingStations) {
		JSONArray rootArray = (JSONArray) json.get(JSONKeys.JSON_KEY_FUSE_TREE_ROOT);
		JSONObject root = (JSONObject) rootArray.get(0);
		
		FuseTreeChargingStationCounter counter = new FuseTreeChargingStationCounter(limitNumberChargingStations);
		Fuse fuse = Fuse.fromJSON(root, counter, chargingStations);
		int numberChildrenBottomLevel = Integer.valueOf(json.get(JSONKeys.JSON_KEY_FUSE_TREE_NUMBER_CHARGING_STATIONS_BOTTOM_LEVEL).toString());
		
		FuseTree result = new FuseTree(fuse, numberChildrenBottomLevel);
		return result;
	}
	
	public static FuseTree fromJSON(JSONObject json, List<ChargingStation> chargingStations) {
		return fromJSON(json, 0, chargingStations);
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FuseTree other = (FuseTree) obj;
		if (numberChargingStationsBottomLevel != other.numberChargingStationsBottomLevel)
			return false;
		if (rootFuse == null) {
			if (other.rootFuse != null)
				return false;
		} else if (!rootFuse.equals(other.rootFuse))
			return false;
		return true;
	}
	
	
}
