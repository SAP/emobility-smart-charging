package com.sap.charging.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.dataGeneration.common.DefaultDataGenerator;
import com.sap.charging.model.ChargingStationFactory.Standard;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.sim.Simulation;
import com.sap.charging.util.Callback;

public class FuseTreeTest {

	private FuseTree fuseTree;
	private DataGenerator data;
	
	private static ChargingStation buildDefaultChargingStation() {
		return ChargingStationFactory.builder()
				.buildFromStandard(Standard.KeContact_P30)
				.setPhaseMatching(Phase.PHASE_2, 
						Phase.PHASE_2, Phase.PHASE_3)
				.build();
	}
	
	@BeforeEach
	public void setup() {
		Simulation.verbosity = 0; 
		data = DefaultDataGenerator.getDefaultDataGenerator();
		fuseTree = data.getFuseTree();
	}
	
	@Test
	public void testToString() {
		String result = fuseTree.toString();
		assertTrue(result.contains("l0"));
		assertTrue(result.contains(CONSTANTS.FUSE_LEVEL_0_SIZE + ""));
		assertTrue(result.contains(CONSTANTS.FUSE_LEVEL_1_SIZE + ""));
		assertTrue(result.contains(CONSTANTS.FUSE_LEVEL_2_SIZE + ""));
	}

	@Test
	public void testSubtree() {
		Fuse root = fuseTree.getRootFuse();
		Fuse first800Fuse = (Fuse) root.getChildren().get(0)  // 1250A
									   .getChildren().get(0); //  800A
		
		FuseTree subTree = fuseTree.getSubtree(first800Fuse);
		
		assertEquals(DefaultDataGenerator.nBottomLevelChargingStations,
					 subTree.getRootFuse().getChildren().size());
	}
	
	private int nChargingStationsCounted = 0;
	private int nFusesCounted;
	
	private void addChargingStationCounted() {
		nChargingStationsCounted++;
	}
	private void addNFusesCounted() {
		nFusesCounted++;
	}

	@Test 
	public void testTraverseTree() {
		fuseTree.traverseTree(new Callback<FuseTreeNode>() {
			@Override
			public void callback(FuseTreeNode item) {
				if (item instanceof Fuse) 
					addNFusesCounted();
				if (item instanceof ChargingStation)
					addChargingStationCounted();
			}
		});
		
		assertEquals(DefaultDataGenerator.nChargingStations,
					 nChargingStationsCounted);
		assertEquals(1+1*3+3*2, 
				     nFusesCounted);
	}
	
	@Test 
	public void testAllParentsSet() {
	  AtomicInteger nodeCounter = new AtomicInteger(0);
	  AtomicInteger parentCounter = new AtomicInteger(0);
	  fuseTree.traverseTree(node -> {
	    nodeCounter.incrementAndGet();
	    if(node.getParent()!=null){
	      parentCounter.incrementAndGet();
	    }
	    if(node.hasChildren()){
	      node.getChildren().forEach(child -> Assert.assertEquals(node, child.getParent()));
	    }
	  });
	  
	  assertEquals(nodeCounter.get()-1, parentCounter.get());
	}
	
	@Test
	public void testToJSON() {
		Fuse root = new Fuse(0, 32);
		Fuse f1 = new Fuse(1, 32);
		root.addChild(f1);
		
		ChargingStation cs1 = buildDefaultChargingStation();
		f1.addChild(cs1);
		ChargingStation cs2 = buildDefaultChargingStation();
		f1.addChild(cs2);
		
		Fuse f2 = new Fuse(2, 32);
		root.addChild(f2);
		
		FuseTree fuseTree = new FuseTree(root, 20);

		JSONObject json1 = fuseTree.toJSONObject();
		String jsonString1 = json1.toString();
		
		FuseTree clone = FuseTree.fromJSON(json1, new ArrayList<ChargingStation>());
		String jsonString2 = clone.toJSONObject().toString();
		
		assertEquals(jsonString1, jsonString2);
	}
	
	
	@Test
	public void testFromJSON() throws ParseException {
		String json = "{\"root\":[{\"indexL\":0,\"fuseTreeNodeType\":\"pre-fuse\",\"children\":[{\"indexL\":1,\"fuseTreeNodeType\":\"pre-fuse\",\"children\":[{\"fuseTreeNodeType\":\"chargingStation\",\"fusePhase1\":32.0,\"name\":\"i0\",\"fusePhase3\":32.0,\"phaseMatching\":{\"phase1\":\"phase2\",\"phase2\":\"phase2\",\"phase3\":\"phase3\"},\"fusePhase2\":32.0,\"isBEVAllowed\":true,\"isPHEVAllowed\":true,\"indexI\":0},{\"fuseTreeNodeType\":\"chargingStation\",\"fusePhase1\":32.0,\"name\":\"i0\",\"fusePhase3\":32.0,\"phaseMatching\":{\"phase1\":\"phase2\",\"phase2\":\"phase2\",\"phase3\":\"phase3\"},\"fusePhase2\":32.0,\"isBEVAllowed\":true,\"isPHEVAllowed\":true,\"indexI\":0}],\"fusePhase1\":32.0,\"name\":\"l1\",\"fusePhase3\":32.0,\"fusePhase2\":32.0},{\"indexL\":2,\"fuseTreeNodeType\":\"pre-fuse\",\"children\":[],\"fusePhase1\":32.0,\"name\":\"l2\",\"fusePhase3\":32.0,\"fusePhase2\":32.0}],\"fusePhase1\":32.0,\"name\":\"l0\",\"fusePhase3\":32.0,\"fusePhase2\":32.0}],\"numberChildrenBottomLevel\":20}";
		
		JSONObject jsonObject = (JSONObject) (new JSONParser()).parse(json);
		FuseTree fuseTree = FuseTree.fromJSON(jsonObject, new ArrayList<ChargingStation>());
		
		assertEquals(20, fuseTree.getNumberChargingStationsBottomLevel());
	}
	
	
	
	
}
