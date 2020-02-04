package com.sap.charging.server.api.v1.store;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.realTime.State;
import com.sap.charging.server.api.v1.exception.MissingParameterException;
import com.sap.charging.server.api.v1.exception.UnknownCarException;
import com.sap.charging.server.api.v1.exception.UnknownChargingStationException;
import com.sap.charging.server.api.v1.exception.UnknownEventTypeException;
import com.sap.charging.sim.event.Event;
import com.sap.charging.sim.event.EventCarArrival;
import com.sap.charging.sim.event.EventCarDeparture;
import com.sap.charging.sim.event.EventCarFinished;
import com.sap.charging.sim.event.EventEnergyPriceChange;
import com.sap.charging.sim.event.EventReoptimize;
import com.sap.charging.sim.event.EventType;
import com.sap.charging.util.TimeUtil;

public class EventStore {

	public final Integer carID;
	public final Integer chargingStationID;
	public final EnergyPriceHistory energyPriceHistory;
	public final EventType eventType;

	@JsonCreator
	public EventStore(@JsonProperty("carID") Integer carID,
			@JsonProperty("chargingStationID") Integer chargingStationID,
			@JsonProperty("energyPriceHistory") EnergyPriceHistory energyPriceHistory,
			@JsonProperty(value = "eventType", required = true) EventType eventType) {
		this.carID = carID;
		this.chargingStationID = chargingStationID;
		this.energyPriceHistory = energyPriceHistory;
		this.eventType = eventType;
	}

	public Event toEvent(State state) {
		LocalTime timestamp = TimeUtil.getTimestampFromSeconds(state.currentTimeSeconds);
		Event event = null;
		switch (eventType) {
		case CarArrival:
			Car car = this.checkAndGetCar(state);
			event = new EventCarArrival(timestamp, car);
			break;
		case CarDeparture:
			car = this.checkAndGetCar(state);
			event = new EventCarDeparture(timestamp, car);
			break;
		case CarFinished:
			car = this.checkAndGetCar(state);
			ChargingStation chargingStation = this.checkAndGetChargingStation(state);
			event = new EventCarFinished(timestamp, car, chargingStation);
			break;
		case EnergyPriceChange:
			if (this.energyPriceHistory == null) {
				throw new IllegalArgumentException("EnergyPriceHistory must be passed when using this event type.");
			}
			event = new EventEnergyPriceChange(timestamp, this.energyPriceHistory);
			break;
		case Reoptimize:
			event = new EventReoptimize(timestamp);
			break;
		default:
			throw new UnknownEventTypeException("eventType=" + eventType + " not implemented!");
		}
		return event;
	}

	private Car checkAndGetCar(State state) {
		if (this.carID == null) {
			throw new MissingParameterException("event.carID");
		}
		Car car = state.getCar(this.carID);
		if (car == null) {
			throw new UnknownCarException(this.carID);
		}
		return car;
	}

	private ChargingStation checkAndGetChargingStation(State state) {
		if (this.chargingStationID == null) {
			throw new MissingParameterException("event.chargingStationID");
		}
		ChargingStation chargingStation = state.getChargingStation(this.chargingStationID);
		if (chargingStation == null) {
			throw new UnknownChargingStationException(this.chargingStationID);
		}
		return chargingStation;
	}

}
