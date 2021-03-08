package com.sap.charging.server.api.v1.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OptimizerSettings {

    private double weightObjectiveFairShare;
    private double weightObjectivePeakShaving;
    private double weightObjectiveEnergyCosts;
    private double weightObjectiveLoadImbalance;

    public OptimizerSettings() {}

    @JsonCreator
    public OptimizerSettings(@JsonProperty("weightObjectiveFairShare") double weightObjectiveFairShare,
        @JsonProperty("weightObjectivePeakShaving") double weightObjectivePeakShaving,
        @JsonProperty("weightObjectiveEnergyCosts") double weightObjectiveEnergyCosts,
        @JsonProperty("weightObjectiveLoadImbalance") double weightObjectiveLoadImbalance) {

        if (weightObjectiveFairShare < 0 || weightObjectivePeakShaving < 0 || weightObjectiveEnergyCosts < 0
            || weightObjectiveLoadImbalance < 0) {
            throw new IllegalArgumentException("Optimizer objective weight settings should be >= 0");
        }

        this.weightObjectiveFairShare = weightObjectiveFairShare;
        this.weightObjectivePeakShaving = weightObjectivePeakShaving;
        this.weightObjectiveEnergyCosts = weightObjectiveEnergyCosts;
        this.weightObjectiveLoadImbalance = weightObjectiveLoadImbalance;
    }

    /**
     * Default settings are to only use fair share as the optimization goal.
     * 
     * @return
     */
    public static OptimizerSettings getDefaultOptimizerSettings() {
        OptimizerSettings settings = new OptimizerSettings();
        settings.setWeightObjectiveFairShare(1);
        settings.setWeightObjectiveEnergyCosts(0);
        settings.setWeightObjectiveLoadImbalance(0);
        settings.setWeightObjectivePeakShaving(0);
        return settings;
    }

    public double getWeightObjectiveFairShare() {
        return weightObjectiveFairShare;
    }

    public void setWeightObjectiveFairShare(double weightObjectiveFairShare) {
        this.weightObjectiveFairShare = weightObjectiveFairShare;
    }

    public double getWeightObjectivePeakShaving() {
        return weightObjectivePeakShaving;
    }

    public void setWeightObjectivePeakShaving(double weightObjectivePeakShaving) {
        this.weightObjectivePeakShaving = weightObjectivePeakShaving;
    }

    public double getWeightObjectiveEnergyCosts() {
        return weightObjectiveEnergyCosts;
    }

    public void setWeightObjectiveEnergyCosts(double weightObjectiveEnergyCosts) {
        this.weightObjectiveEnergyCosts = weightObjectiveEnergyCosts;
    }

    public double getWeightObjectiveLoadImbalance() {
        return weightObjectiveLoadImbalance;
    }

    public void setWeightObjectiveLoadImbalance(double weightObjectiveLoadImbalance) {
        this.weightObjectiveLoadImbalance = weightObjectiveLoadImbalance;
    }

    @Override
    public String toString() {
        return "OptimizerSettings [weightObjectiveFairShare=" + weightObjectiveFairShare
            + ", weightObjectivePeakShaving=" + weightObjectivePeakShaving + ", weightObjectiveEnergyCosts="
            + weightObjectiveEnergyCosts + ", weightObjectiveLoadImbalance=" + weightObjectiveLoadImbalance + "]";
    }

}
