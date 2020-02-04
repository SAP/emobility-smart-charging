package com.sap.charging.ocpp;

import java.time.LocalDateTime;

import org.json.simple.JSONObject;

import com.sap.charging.dataGeneration.DataGeneratorFromFile;
import com.sap.charging.dataGeneration.DataGeneratorRandom;
import com.sap.charging.model.Car;
import com.sap.charging.ocpp.protocol.ChargingProfile;
import com.sap.charging.ocpp.protocol.ChargingSchedule;
import com.sap.charging.ocpp.protocol.ChargingSchedulePeriod;
import com.sap.charging.realTime.State;
import com.sap.charging.realTime.Strategy;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.model.CarAssignment;
import com.sap.charging.sim.Simulation;
import com.sap.charging.sim.event.Event;
import com.sap.charging.sim.event.EventCarArrival;
import com.sap.charging.sim.event.EventCarDeparture;
import com.sap.charging.sim.event.EventType;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.JSONKeys;
import com.sap.charging.util.TimeUtil;

public class OCPPEntrypoint {
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		
		Strategy strategyInit = new StrategyAlgorithmic();
		
		// Test case: Get a nice initial state
		DataGeneratorRandom data = new DataGeneratorRandom(0, false);
		data.generateEnergyPriceHistory(96)
			.generateCars(1)
			.generateChargingStations(2)
			.generateFuseTree(5, true);
		
		
		Simulation sim = new Simulation(data, strategyInit);
		sim.init();
		
		for (int i=0;i<40000;i++) {
			sim.simulateNextStep();
		}
		
		JSONObject state1 = sim.getState().toJSONObject();
		JSONObject problemInstance = sim.getState().getProblemInstanceJSON();
		
		JSONObject input = new JSONObject();
		
		
		/**
		 * Test case: A new car arrives
		 */
		
		input.put(JSONKeys.JSON_KEY_STATE, state1);
		input.put(JSONKeys.JSON_KEY_PROBLEM_INSTANCE, problemInstance);
		
		EventCarArrival event = new EventCarArrival(TimeUtil.getTimestampFromSeconds(40000), getRandomCar());
		input.put(JSONKeys.JSON_KEY_EVENT, event.toJSONObject());
		
		FileIO.writeFile("gen/input.json", input);
		
		
		OCPPEntrypoint ocppEntrypoint = new OCPPEntrypoint(input);
		JSONObject output = ocppEntrypoint.generateOutput();
		
		// Recheck updated status of output
		FileIO.writeFile("gen/output.json", output);
	}
	
	
	private final Strategy strategy;
	private final DataGeneratorFromFile data;
	private final State state;
	private final Event event;
	
	/**
	 * Input:
	 * 
	 * {
	 * 	problemInstance: {},
	 * 	state: {},
	 * 	event: {}
	 * }
	 */
	public OCPPEntrypoint(JSONObject input) {
		strategy = new StrategyAlgorithmic();
		
		this.data = new DataGeneratorFromFile(input);
		this.data.generateAll();
		this.state = State.fromPreviousData(input, data);
		this.event = EventType.fromJSON((JSONObject) input.get(JSONKeys.JSON_KEY_EVENT));
	}
	
	private OCPPData buildOCPPData() {
		
		ChargingProfileAssignment[] chargingProfileAssignments = new ChargingProfileAssignment[state.getCurrentCarAssignments().size()];
		
		for (int i=0;i<chargingProfileAssignments.length; i++) {
			CarAssignment carAssignment = state.getCurrentCarAssignments().get(i);
			
			// Each charging station receives one of these blocks
			LocalDateTime dateTimeMidnight = TimeUtil.getCurrentDayMidnight();
			ChargingSchedulePeriod[] chargingSchedulePeriods = new ChargingSchedulePeriod[96];
			for (int k=0;k<chargingSchedulePeriods.length;k++) {
				Car car = carAssignment.car;
				double limit = car.getCurrentPlan()[k];
				chargingSchedulePeriods[k] = new ChargingSchedulePeriod(k*15*60, limit, (int) car.sumUsedPhases);
			}
			
			ChargingSchedule chargingSchedule = new ChargingSchedule(dateTimeMidnight, chargingSchedulePeriods);
			ChargingProfile chargingProfile = new ChargingProfile(chargingSchedule);
			
			chargingProfileAssignments[i] = new ChargingProfileAssignment(carAssignment.chargingStation.getId(), chargingProfile);
		}
		
		OCPPData ocppData = new OCPPData(chargingProfileAssignments);
		
		return ocppData;
	}
	
	private void updateState() {
		
		if (event instanceof EventCarArrival) {
			Car car = ((EventCarArrival) event).car;
			data.getCars().add(car);
		}
		
		strategy.react(state, event);
		
		if (event instanceof EventCarDeparture) {
			Car car = ((EventCarDeparture) event).car;
			for (Car carInLoop : data.getCars()) {
				if (car.getId() == carInLoop.getId()) {
					data.getCars().remove(carInLoop);
					break;
				}
			}
		}
		
	}
	
	/**
	 * Output:
	 * problemInstance: {}, // updated
	 * state: {}, //updated
	 * ocppData: {
	 * 	"version": "1.6",
	 *  "chargingProfileAssignments": [{
	 *    "indexI": 0,
	 *    "chargingProfile": {...}
	 *  }]
	 * }
	 */
	@SuppressWarnings("unchecked")
	public JSONObject generateOutput() {
		updateState();
		
		OCPPData ocppData = buildOCPPData();
		
		JSONObject output = new JSONObject();
		output.put(JSONKeys.JSON_KEY_PROBLEM_INSTANCE, state.getProblemInstanceJSON());
		output.put(JSONKeys.JSON_KEY_STATE, state.toJSONObject());
		
		output.put(JSONKeys.JSON_KEY_OCPP_DATA, ocppData.toJSONObject());
		
		return output;
	}

	
	
	
	
	private static Car getRandomCar() {
		DataGeneratorRandom data = new DataGeneratorRandom(0, false);
		data.setMinCarId(1);
		data.generateEnergyPriceHistory(96)
			.generateCars(1);
		return data.getCars().get(0);
	}
	
	
	
	
	
	
	
	
	
	
}
