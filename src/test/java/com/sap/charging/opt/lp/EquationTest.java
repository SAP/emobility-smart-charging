package com.sap.charging.opt.lp;

import static org.junit.Assert.*;

import org.junit.jupiter.api.Test;

import com.sap.charging.opt.lp.Equation;
import com.sap.charging.opt.lp.Variable;
import com.sap.charging.opt.lp.Equation.ConstraintType;

public class EquationTest {
	
	@Test
	public void testEnums() {
		ConstraintType.values();
		assertEquals(ConstraintType.EQU, ConstraintType.valueOf("EQU"));
	}
	
	@Test
	public void test() {
		Variable variableX = VariableTest.getDefaultVariableX();
		
		Equation eq = new Equation("test", ConstraintType.LEQ);
		eq.cloneAndAddVariable(variableX, 5);
		eq.RHS = 10;
		
		Variable copiedVariable = eq.getVariables().get(0);
		assertNotEquals(variableX, copiedVariable);
		assertEquals(variableX.getValue(), copiedVariable.getValue(), 1e-8);
	}
	
	@Test
	public void testToString() {
		Variable variableX = VariableTest.getDefaultVariableX();
		
		Equation eq = new Equation("test", ConstraintType.LEQ);
		eq.cloneAndAddVariable(variableX, 5);
		eq.RHS = 10;

		assertEquals("+5.0*X_i5_n12 <= 10.0", eq.toString());
		assertFalse(eq.isObjectiveFunction());
	}
	
	@Test
	public void testToStringObj() {
		Variable variableX = VariableTest.getDefaultVariableX();
		
		Equation eq = new Equation("test", ConstraintType.OBJ);
		eq.cloneAndAddVariable(variableX, 5);

		assertTrue(eq.isObjectiveFunction());
		assertEquals("z=+5.0*X_i5_n12", eq.toString());
	}

}
