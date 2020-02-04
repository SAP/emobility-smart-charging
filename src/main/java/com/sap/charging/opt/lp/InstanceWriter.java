package com.sap.charging.opt.lp;

import java.util.ArrayList;

import org.apache.commons.collections4.map.MultiKeyMap;

public class InstanceWriter {

	
	
	
	/**
	 * Reference to (CPLEX) LP file format: 
	 * https://www.ibm.com/support/knowledgecenter/SSSA5P_12.5.1/ilog.odms.cplex.help/CPLEX/FileFormats/topics/LP.html
	 * http://www.gurobi.com/documentation/7.5/refman/lp_format.html
	 * @param variablesX X_{i,n}
	 */
	public String generateLPFile(
			Equation objectiveFunction,
			ArrayList<Equation> allRestrictions, 
			MultiKeyMap<Integer,Variable> variablesX,
			MultiKeyMap<Integer,Variable> variablesU,
			MultiKeyMap<Integer,Variable> variablesUPrime,
			MultiKeyMap<Integer,Variable> variablesV) {
		LPStringBuilder builder = LPStringBuilder.builder();
		
		/**
		 * Add objective function (spaces are important).
		 * Example:
		 * MINIMIZE
		 *   5.1 x - 3.2 y - 3.4 z
		 */
		builder.appendString("MINIMIZE").appendLineEnding();
		builder.appendSpaces(2);
		for (Variable power : objectiveFunction.getVariables()) {
			builder.appendString(power.getCoefficient() >= 0 ? "+" : "-")
				   .appendSpaces(1)
				   .appendDouble(Math.abs(power.getCoefficient()))
				   .appendSpaces(1)
				   .appendString(power.getNameWithIndices())
				   .appendSpaces(1);
		}
		builder.appendLineEnding();
		
		/**
		 * Add SUBJECT TO field: restrictions
		 * Example:
		 * SUBJECT TO 
		 *   R1_1: 
		 */
		builder.appendString("SUBJECT TO").appendLineEnding();
		for (Equation restriction : allRestrictions) {
			builder.appendSpaces(2)
				   .appendString(restriction.getNameWithIndices())
				   .appendString(":").appendSpaces(1);

			for (Variable variable : restriction.getVariables()) {
				builder.appendString(variable.getCoefficient() >= 0 ? "+" : "-")
				       .appendSpaces(1);
				
				if (Math.abs(variable.getCoefficient()) != 1.0) {
					builder.appendDouble(Math.abs(variable.getCoefficient()))
					       .appendSpaces(1);
				}
				builder.appendString(variable.getNameWithIndices())
					   .appendSpaces(1);
				
			}
			
			// Add constraint type
			builder.appendSpaces(1)
				   .appendString(restriction.constraintType.lpString);
			
			// Add RHS
			builder.appendSpaces(1)
			       .appendString("" + restriction.RHS);
			
			builder.appendLineEnding();
		}
		
		
		/**
		 * Add BINARY field: Which variable are variable integers
		 * Example:
		 * BINARY
		 *   X_i0_n0 Xi1_n0
		 */
		builder.appendString("BINARY")
		       .appendLineEnding()
		       .appendSpaces(2);
		for (Variable variableX : variablesX.values()) {
			builder.appendString(variableX.getNameWithIndices()) 
			       .appendSpaces(1);
		}
		for (Variable variableU : variablesU.values()) {
			builder.appendString(variableU.getNameWithIndices())
				   .appendSpaces(1);
		}
		for (Variable variableUPrime : variablesUPrime.values()) {
			builder.appendString(variableUPrime.getNameWithIndices())
				   .appendSpaces(1);
		}
		for (Variable variableV : variablesV.values()) {
			builder.appendString(variableV.getNameWithIndices())
				   .appendSpaces(1);
		}
		
		return builder.build();
	}
	
	
	
	
}
