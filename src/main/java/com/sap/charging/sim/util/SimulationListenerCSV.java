package com.sap.charging.sim.util;

import com.sap.charging.realTime.State;

public class SimulationListenerCSV extends SimulationListenerOutputData {

	public SimulationListenerCSV() {
		super();
	}

	public String getCSVString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("step;current;currentPlanLimit\n");
		for (Row row : rows) {
			appendRow(sb, row);
		}
		
		return sb.toString();
	}

	private void appendRow(StringBuilder sb, Row row) {
		sb.append(row.step + ";" + row.current + ";" + row.currentPlanLimit);
		sb.append("\n");
	}

	@Override
	// Ignore
	public void callbackBeforeUpdate(State state) {}

}
