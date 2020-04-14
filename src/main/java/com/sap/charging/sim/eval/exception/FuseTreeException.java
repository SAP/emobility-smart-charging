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
	
	public double getSumConsumedByPhase(Phase phase) {
		switch (phase) {
		case PHASE_1: return sumConsumed[0]; 
		case PHASE_2: return sumConsumed[1]; 
		case PHASE_3: return sumConsumed[2]; 
		default: throw new NullPointerException("Unknown phase"); 
		}
	}
	
	/**
	 * How much are we over the top for a given phase?
	 * 
	 * Returns consumed - fuseSize
	 * @param phase
	 * @return
	 */
	public double getDeltaByPhase(Phase phase) {
		double sumConsumed = this.getSumConsumedByPhase(phase); 
		double fuseSize = this.fuse.getFusePhase(phase); 
		return sumConsumed - fuseSize; 
	}
	
	/**
	 * Which phase has the highest delta, i.e., on which phase should we reduce current on?
	 * Which phase should we reduce current on?
	 * @return
	 */
	public Phase getPhaseWithHighestDelta() {
		double phase1Delta = this.getDeltaByPhase(Phase.PHASE_1); 
		double phase2Delta = this.getDeltaByPhase(Phase.PHASE_2); 
		double phase3Delta = this.getDeltaByPhase(Phase.PHASE_3); 
		
		if (phase1Delta >= phase2Delta && phase1Delta >= phase3Delta) {
			return Phase.PHASE_1; 
		}
		if (phase2Delta >= phase1Delta && phase2Delta >= phase3Delta) {
			return Phase.PHASE_2;
		}
		if (phase3Delta >= phase1Delta && phase3Delta >= phase2Delta) {
			return Phase.PHASE_3; 
		}
		throw new RuntimeException("Error finding phase with highest delta"); 
	}
	
	
    public double getHighestSumConsumed() {
        return Arrays.stream(sumConsumed).max().getAsDouble();
    }
	
    
}
