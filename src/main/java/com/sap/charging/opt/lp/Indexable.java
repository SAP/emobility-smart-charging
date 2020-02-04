package com.sap.charging.opt.lp;

import java.util.HashMap;
import java.util.LinkedHashMap;

public abstract class Indexable {
	
	/**
	 * For example, P_{i,j,k} has three indices.
	 * This HashMap stores them
	 */
	protected HashMap<String,Integer> indices;
	
	private final String name;
	
	public Indexable(String name) {
		this.name = name;
		this.indices = new LinkedHashMap<>();
	}
	
	/**
	 * Returns only the name of the variable (e.g. "P" or "X")
	 * @return
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Returns the name of the variable with its indices (e.g. "P_i1_j2_k3" or "X_i1_n2")
	 * Example: 
	 * @return
	 */
	public String getNameWithIndices() {
		if (this.indices.size() == 0)
			return this.name;
		
		String result = this.name + "_";
		for (String index : indices.keySet()) {
			result += index + getIndex(index) + "_";
		}
		result = result.substring(0,result.length()-1); // remove last "_"
		return result;
	}
	
	/**
	 * Returns the name of the variable with its indices in a long form
	 * (e.g. P_{i=1,j=2,k=3} or X_{i=1,n=2}
	 */
	public String getNameWithIndicesLong() {
		if (this.indices.size() == 0)
			return this.name;
		
		String result = this.name + "_{";
		for (String index : indices.keySet()) {
			result += index + "=" + getIndex(index) + ","; 
		}
		result = result.substring(0,result.length()-1); // remove last ","
		result += "}";
		return result;
	}
	
	
	
	public Indexable setIndex(String indexName, int value) {
		this.indices.put(indexName, value);
		return this;
	}
	
	public HashMap<String, Integer> getIndices() {
		return indices;
	}
	public int getIndex(String indexName) {
		if (this.indices.containsKey(indexName)) {
			return this.indices.get(indexName);
		}
		else {
			return -1;
		}
		
	}
	

}



