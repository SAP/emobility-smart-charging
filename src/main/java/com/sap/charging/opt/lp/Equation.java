package com.sap.charging.opt.lp;

import java.util.ArrayList;

public class Equation extends Indexable {
	
	/**
	 * Different constraint types and their strings in .mps and .lp CPLEX file formats. 
	 */
	public enum ConstraintType {
		
		OBJ("N", ""),
		EQU("E", "="),
		GEQ("G", ">="),
		LEQ("L", "<=");
		
		final String mpsString;
		final String lpString;
		ConstraintType(String mpsString, String lpString) {
			this.mpsString = mpsString;
			this.lpString = lpString;
		}
	}
	
	
	
	public Equation(String name, ConstraintType constraintType) {
		super(name);
		this.constraintType = constraintType;
	}
	
	/**
	 * LHS variables
	 */
	private ArrayList<Variable> variables = new ArrayList<>();
	
	/**
	 *  Constraint type: like in MPS file format
	 *  E equality
	 *  L less than or equal
	 *  G greater than or equal
	 *  N free row (first is used as objective function)
	 *  http://www.gurobi.com/documentation/7.5/refman/mps_format.html
	 */
	public final ConstraintType constraintType;
	
	/**
	 * RHS Constant
	 */
	public double RHS = Double.MIN_VALUE;
	
	/**
	 * Objective function does not need a constraintType and RHS
	 */
	public boolean isObjectiveFunction() {
		return constraintType == ConstraintType.OBJ;
	}
	
	public void cloneAndAddVariable(Variable variable, double coefficient) {
		Variable clonedVariable = variable.clone();
		clonedVariable.setCoefficient(coefficient);
		variables.add(clonedVariable);
	}
	
	public ArrayList<Variable> getVariables() {
		return variables;
	}
	
	public String toString() {
		String result = "";
		
		if (isObjectiveFunction() == true) {
			result += "z=";
		}
		
		for (Variable v : variables) {
			result += v.toString(); 
		}
		
		if (isObjectiveFunction() == false) {
			result += " " + constraintType.lpString + " " + RHS;
		}
		return result;
	}

	public boolean hasVariables() {
		return getVariables().size() > 0;
	}

}
