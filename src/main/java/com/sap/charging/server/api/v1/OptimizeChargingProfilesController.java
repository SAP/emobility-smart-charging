package com.sap.charging.server.api.v1; 

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.sap.charging.realTime.State;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureForecast;
import com.sap.charging.sim.Simulation;
import com.sap.charging.sim.event.Event;
import com.sap.charging.util.Loggable;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@Api(value = "emobility-smart-charging REST API")
public class OptimizeChargingProfilesController implements Loggable {

	@Override
	public int getVerbosity() {
		return Simulation.verbosity;
	}

	@CrossOrigin(origins = "*")	
	@ApiOperation(value = "Optimize Charging Profiles")
    @PostMapping(path="/api/v1/OptimizeChargingProfiles", produces=MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> optimizeChargingProfiles( @ApiParam @RequestBody OptimizeChargingProfilesRequest request) {
    	
    	Simulation.verbosity = request.verbosity;
    	    	
    	//ObjectMapper mapper = new ObjectMapper(); // jackson's objectmapper
    	    	
    	log(1, "Received /api/v1/OptimizeChargingProfiles with body: " + request.toString());
    	
    	// Request values
    	Object response = null; 
    	HttpStatus httpStatus = HttpStatus.ACCEPTED; 
    	try {
    		//OptimizeChargingProfilesRequest request = mapper.convertValue(payload, OptimizeChargingProfilesRequest.class);
        	State state = request.state.toState(); 
    		Event event = request.event.toEvent(state); 
        	
        	// React to events
        	StrategyAlgorithmic strategy = new StrategyAlgorithmic(CarDepartureForecast.getDefaultCarDepartureForecast()); 
        	strategy.objectiveEnergyCosts.setWeight(0);
        	strategy.objectiveFairShare.setWeight(1);
        	strategy.setReoptimizeOnStillAvailableAfterExpectedDepartureTimeslot(true);
        	strategy.setRescheduleCarsWith0A(false);
        	strategy.react(state, event); 
        	
        	
        	// Create response
        	response = new OptimizeChargingProfilesResponse(state.cars);
    	}
    	catch (Exception e) {
    		//e.printStackTrace();
    		System.err.println(e.getClass().getName()); 
    		System.err.println(e.getMessage()); 
    		response = new ErrorResponse(e); 
    		httpStatus = HttpStatus.INTERNAL_SERVER_ERROR; 
    		e.printStackTrace();
    	}
    	
    	
    	
    	//String result = "End point working! Loaded " + state.cars.size() + " cars and " + state.nChargingStations + " charging stations. Charging profile produced: " + Arrays.toString(state.getCar(0).getCurrentPlan()); 
    	return new ResponseEntity<>(response, httpStatus); 
    }

}





