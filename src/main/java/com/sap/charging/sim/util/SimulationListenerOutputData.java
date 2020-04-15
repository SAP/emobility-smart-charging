package com.sap.charging.sim.util;

import java.util.ArrayList;

import com.sap.charging.realTime.State;

public class SimulationListenerOutputData implements SimulationListener
{

    protected class Row {
        final int step;
        final double current;
        final double currentPlanLimit;
        //final double power;


        Row(int second, double current, double currentPlanLimit) {
            this.step = second;
            this.current = current;
            this.currentPlanLimit = currentPlanLimit;
            //this.power = power;
        }
    }

    protected ArrayList<Row> rows;

    public SimulationListenerOutputData()
    {
        this.rows = new ArrayList<>();
    }

    @Override
    /**
     * Records per second the current amount of aggregated energy
     * in kW
     */
    public void callbackAfterUpdate(State state)
    {
        double aggregatedCurrent = state.getCurrentPowerAssignments().stream()
                .mapToDouble(p -> p.getPhase1()+p.getPhase2()+p.getPhase3())
                .sum();
        //double aggregatedPower = EnergyUtil.calculatePFromI(aggregatedCurrent, 1);
        double aggregatedCurrentPlanLimit = state.getCurrentPowerAssignments().stream()
                .map(powerAssignment -> powerAssignment.car)
                .mapToDouble(car -> car.getCurrentPlan() != null ?
                        car.getCurrentPlan()[state.currentTimeslot] * car.sumUsedPhases :
                        car.maxCurrent)
                .sum();

        Row row = new Row(state.currentTimeSeconds, aggregatedCurrent, aggregatedCurrentPlanLimit);
        this.rows.add(row);
    }

    @Override
    // Ignore
    public void callbackBeforeUpdate(State state) {}

}
