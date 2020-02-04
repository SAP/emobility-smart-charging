package com.sap.charging.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.charging.model.EnergyUtil.Phase;

public class EnergyUtilTest {
	
	
	@Test
	public void testPhases() {
		new EnergyUtil();
		Phase.values(); // superficial call for test coverage
		
		Phase result = Phase.valueOf("PHASE_1");
		assertEquals(Phase.PHASE_1, result);
		assertEquals(1, result.asInt());
		assertEquals("phase1", result.asStringConst());
		
		assertEquals(Phase.PHASE_1, Phase.getByInt(1));
		assertEquals(Phase.PHASE_2, Phase.getByInt(2));
		assertEquals(Phase.PHASE_3, Phase.getByInt(3));
		
		
		try {
			// Try getting an invalid phase
			Phase.getByInt(4);
		}
		catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}
	}

	@Test
	public void testGetIFromP() {
		double kW = 22.0;
		
		// 1 phase car
		double expected1 = 1000.0*kW/ (1*230.0);
		assertEquals(expected1, EnergyUtil.calculateIFromP(kW, 1), 1e-8);
		
		// 3 phase car
		double expected3 = 1000*kW / (3*230);
		assertEquals(expected3, EnergyUtil.calculateIFromP(kW, 3), 1e-8);
	}
	
	@Test
	public void testGetAmpereHours() {
		double current = 32;
		double ampereHours = EnergyUtil.getAmpereHours(1800, current);
		
		assertEquals(16, ampereHours, 1e-8);
	}

}
