package com.sap.charging.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import com.sap.charging.model.ChargingStationFactory.Standard;
import com.sap.charging.model.EnergyUtil.Phase;

public class ChargingStationFactoryTest {

	@Test
	public void testEnums() {
		ChargingStationFactory.Standard.values();
		assertEquals(
				Standard.KeContact_P30,
				ChargingStationFactory.Standard.valueOf("KeContact_P30"));
	}
	

	@Test
	public void testManualBuild() {
		ChargingStationFactory factory = new ChargingStationFactory();
		
		ChargingStation result = factory
				.buildFromStandard(Standard.KeContact_P30)
				.setIndexI(0)
				.setPhaseMatching(Phase.PHASE_3, Phase.PHASE_2, Phase.PHASE_1)
				.build();
		
		
		assertEquals(0, result.getId());
		assertEquals(32, result.fusePhase1, 1e-8);
		assertEquals(32, result.fusePhase2, 1e-8);
		assertEquals(32, result.fusePhase3, 1e-8);
		assertEquals(32, result.getFusePhase(1), 1e-8);
		assertEquals(Phase.PHASE_3, result.getPhaseConsumed(Phase.PHASE_1));
		assertEquals(Phase.PHASE_2, result.getPhaseConsumed(Phase.PHASE_2));
		assertEquals(Phase.PHASE_1, result.getPhaseConsumed(Phase.PHASE_3));
	}

	@Test
	public void testFromJSON() throws ParseException {
		String json = "{" +
			"\"fuseTreeNodeType\": \"chargingStation\"," +
			"\"fusePhase1\": 32.0," +
			"\"name\": \"i4\"," +
			"\"fusePhase2\": 16.5," +
			"\"phaseMatching\": {"+
				"\"phase1\": \"phase2\"," +
				"\"phase2\": \"phase3\"," +
				"\"phase3\": \"phase1\"" + 
			"}, " +
			"\"fusePhase3\": 31.0," +
			"\"isBEVAllowed\": true," + 
			"\"isPHEVAllowed\": false," +
			"\"indexI\": 4" +
		"}";
		JSONObject jsonObject = (JSONObject) (new JSONParser()).parse(json);
		ChargingStation cs = ChargingStation.fromJSON(jsonObject);
		assertEquals(4, cs.getId());
		assertEquals(32, cs.fusePhase1, 1e-8);
		assertEquals(16.5, cs.fusePhase2, 1e-8);
		assertEquals(31, cs.fusePhase3, 1e-8);
		assertEquals(Phase.PHASE_2, cs.getPhaseConsumed(1));
		assertEquals(Phase.PHASE_3, cs.getPhaseConsumed(2));
		assertEquals(Phase.PHASE_1, cs.getPhaseConsumed(3));
		assertTrue(cs.isBEVAllowed);
		assertFalse(cs.isPHEVAllowed);
	}
	
}
