package com.sap.charging.sim.eval.exception;

import java.util.Arrays;

import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.model.FuseTreeNode;
import com.sap.charging.util.Util;

public class FuseTreeException extends ValidationException {
	
	private static final long serialVersionUID = -5045003629623235621L;

	private final FuseTreeNode fuse;
	private final double[] sumConsumed;
	public final int timeslot;
	
	public FuseTreeException(FuseTreeNode fuse, double[] sumConsumed, int timeslot) {
		super("Fuse indexL=" + fuse.getId() + " broken at k=" + timeslot + ": " + 
				"p1: " + Util.formatDouble(sumConsumed[0]) + "A/" + Util.formatDouble(fuse.getFusePhase(Phase.PHASE_1)) + "A (delta=" + Util.formatDouble(sumConsumed[0]-fuse.getFusePhase(Phase.PHASE_1)) + "A), " +
				"p2: " + Util.formatDouble(sumConsumed[1]) + "A/" + Util.formatDouble(fuse.getFusePhase(Phase.PHASE_2)) + "A (delta=" + Util.formatDouble(sumConsumed[1]-fuse.getFusePhase(Phase.PHASE_2)) + "A), " +
				"p3: " + Util.formatDouble(sumConsumed[2]) + "A/" + Util.formatDouble(fuse.getFusePhase(Phase.PHASE_3)) + "A (delta=" + Util.formatDouble(sumConsumed[2]-fuse.getFusePhase(Phase.PHASE_3)) + "A)");
		this.fuse = fuse;
		this.sumConsumed = sumConsumed;
		this.timeslot = timeslot;
	}
	
	public FuseTreeNode getFuse() {
		return this.fuse;
	}
	public double[] getSumConsumed() {
		return sumConsumed;
	}
    public double getHighestSumConsumed() {
        return Arrays.stream(sumConsumed).max().getAsDouble();
    }
	
    
}
