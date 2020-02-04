package com.sap.charging.sim.util;

import com.sap.charging.realTime.State;

public interface SimulationListener {

	void callbackAfterUpdate(State state);

	void callbackBeforeUpdate(State state);

}
