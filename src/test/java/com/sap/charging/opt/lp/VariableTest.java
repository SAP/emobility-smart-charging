package com.sap.charging.opt.lp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VariableTest {

	public static Variable getDefaultVariableX() {
		Variable variableX = new Variable("X", true);
		variableX.setIndex("i", 5);
		variableX.setIndex("n", 12);
		variableX.setValue(1);
		variableX.setCoefficient(123.45);
		return variableX;
	}
	
	@BeforeEach
	public void setup() {
		InstanceLP.verbosity = 0; 
	}
	
	@Test
	public void testEmptyVariable() {
		Variable variableEmpty = new Variable("X", true);
		assertEquals("X", variableEmpty.getName());
		assertEquals("X", variableEmpty.getNameWithIndices());
		assertEquals("X", variableEmpty.getNameWithIndicesLong());
	}
	
	@Test
	public void getInvalidIndex() {
		Variable variableX = getDefaultVariableX();
		assertEquals(-1, variableX.getIndex("invalidIndex"));
	}
	
	@Test
	public void testClone() {
		Variable variableX = getDefaultVariableX();
		Variable clone = variableX.clone();
		
		// Check that references are different
		assertNotEquals(variableX, clone);
		// Use identityHashCode because toString() and .hashCode()
		// are both overriden. Purpose is to check that the reference
		// is not the same. 
		assertNotEquals(System.identityHashCode(variableX.getIndices()), 
				System.identityHashCode(clone.getIndices()));

		assertEquals(variableX.getIndex("i"), clone.getIndex("i"));
		assertEquals(variableX.getIndex("n"), clone.getIndex("n"));
		assertEquals(variableX.getValue(), clone.getValue(), 1e-8);
	}
	
	@Test 
	public void testToString() {
		Variable variableX = getDefaultVariableX();
		assertEquals("+123.45*X_i5_n12", variableX.toString());
		
		variableX.setCoefficient(-1 * variableX.getCoefficient());
		assertEquals("-123.45*X_i5_n12", variableX.toString());
	}
	
	@Test
	public void testToJSONVariableX() {
		Variable variableX = getDefaultVariableX();
		
		JSONObject json = variableX.toJSONObject();

		assertEquals(true, (boolean) json.get("isInteger"));
		assertEquals("X_i5_n12", json.get("variableName"));
		assertEquals(1, (double) json.get("variableValue"), 1e-8);
		assertEquals(123.45, (double) json.get("variableObjCoefficient"), 1e-8);
	}
	
	@Test
	public void testFromJSONVariableX() throws ParseException {
		JSONParser parser = new JSONParser();
		
		String json = "{" + 
				"\"isInteger\": true, " + 
				"\"variableName\": \"X_i5_n12\", " + 
				"\"variableValue\": 1," + 
				"\"variableObjCoefficient\": 54.321" +
			"}";
		JSONObject jsonResult = (JSONObject) parser.parse(json);
		Variable variableX = Variable.fromJSON(jsonResult);
		
		assertEquals("X_i5_n12", variableX.getNameWithIndices());
		assertEquals(true, variableX.isInteger);
		assertEquals(true, variableX.hasValue());
		assertEquals(1, variableX.getValue(), 1e-8);
		assertEquals(54.321, variableX.getCoefficient(), 1e-8);
	}
	
	@Test	
	public void testFromJSONNoValue() throws ParseException {
		JSONParser parser = new JSONParser();
		
		String json = "{" + 
				"\"isInteger\": true, " + 
				"\"variableName\": \"X_i5_n12\"" +
			"}";
		JSONObject jsonResult = (JSONObject) parser.parse(json);
		Variable variableX = Variable.fromJSON(jsonResult);
		
		assertEquals("X_i5_n12", variableX.getNameWithIndices());
		assertEquals(true, variableX.isInteger);
		assertEquals(false, variableX.hasValue());
		assertEquals(Double.NEGATIVE_INFINITY, variableX.getValue(), 1e-8);
	}
	
	@Test
	public void testFromJSONVariableP() throws ParseException {
		JSONParser parser = new JSONParser();
		
		String json = "{" + 
				"\"variableName\": \"P_i0_j1_k12\", " + 
				"\"variableValue\": 16.5" + 
			"}";
		JSONObject jsonResult = (JSONObject) parser.parse(json);
		Variable variableP = Variable.fromJSON(jsonResult);
		
		assertEquals(0, variableP.getIndex("i"));
		assertEquals(1, variableP.getIndex("j"));
		assertEquals(12,variableP.getIndex("k"));
		assertEquals(16.5, variableP.getValue(), 1e-8);
		
		assertEquals("P", variableP.getName());
		assertEquals("P_i0_j1_k12", variableP.getNameWithIndices());
		assertEquals("P_{i=0,j=1,k=12}", variableP.getNameWithIndicesLong());
		assertFalse(variableP.isInteger);
	}
	

}
