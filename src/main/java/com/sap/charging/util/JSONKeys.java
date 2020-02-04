package com.sap.charging.util;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

import org.json.simple.JSONObject;

public class JSONKeys {
	
	/**************************
	 * PROBLEM INSTANCE KEYS
	 **************************/
	/**
	 * JSON key for problem instance
	 * Example: {"problemInstance": {}}
	 */
	public static final String JSON_KEY_PROBLEM_INSTANCE = "problemInstance";
	/**
	 * JSON key for energy price history
	 * Example: {"energyPriceHistory":{}}
	 */
	public static final String JSON_KEY_ENERGY_PRICE_HISTORY = "energyPriceHistory";
	/**
	 * JSON key for energy price history prices array
	 * Example: {"energyPrices": []}
	 */
	public static final String JSON_KEY_ENERGY_PRICES = "energyPrices";
	/**
	 * JSON key for energy price history date
	 * Example: {"date": "2017-12-31"}
	 */
	public static final String JSON_KEY_ENERGY_DATE = "date";
	/**
	 * JSON key for charging station
	 * Example: {"chargingStation": {}}
	 */
	public static final String JSON_KEY_CHARGING_STATION = "chargingStation";
	/**
	 * JSON key for charging stations
	 * Example: {"chargingStations": []}
	 */
	public static final String JSON_KEY_CHARGING_STATIONS = "chargingStations";
	/**
	 * JSON key for names
	 * Example: {"name": "i0"}
	 */
	public static final String JSON_KEY_NAME = "name"; 
	/**
	 * JSON key for model name
	 * Example: {"modelName": "Nissan Leaf 2016"}
	 */
	public static final String JSON_KEY_MODEL_NAME = "modelName";
	/**
	 * JSON key for car type: "BEV", "PHEV", "DIESEL", "PETROL"
	 * Example: {"carType": ""}
	 */
	public static final String JSON_KEY_CAR_TYPE = "carType";
	/**
	 * JSON key for fuse phase 1
	 * Example: {"fusePhase1": 32}
	 */
	public static final String JSON_KEY_FUSE_PHASE_1 = "fusePhase1";
	/**
	 * JSON key for fuse phase 2
	 * Example: {"fusePhase2": 32}
	 */
	public static final String JSON_KEY_FUSE_PHASE_2 = "fusePhase2";
	/**
	 * JSON key for fuse phase 3
	 * Example: {"fusePhase1": 32}
	 */
	public static final String JSON_KEY_FUSE_PHASE_3 = "fusePhase3";
	/**
	 * JSON key for phase matching
	 * Example: {"phaseMatching": {}}
	 */
	public static final String JSON_KEY_PHASE_MATCHING = "phaseMatching";
	/**
	 * JSON key/value for phase 1
	 * Example: "phase1"
	 */
	public static final String JSON_KEY_PHASE_1 = "phase1";
	/**
	 * JSON key/value for phase 2
	 * Example: "phase2"
	 */
	public static final String JSON_KEY_PHASE_2 = "phase2";
	/**
	 * JSON key/value for phase 3
	 * Example: "phase3"
	 */
	public static final String JSON_KEY_PHASE_3 = "phase3";
	/**
	 * JSON key for isBEVAllowed at a charging station
	 * Example: {"isBEVAllowed": true}
	 */
	public static final String JSON_KEY_IS_BEV_ALLOWED = "isBEVAllowed"; 
	/**
	 * JSON key for isPHEVAllowed at a charging station
	 * Example: {"isPHEVAllowed": true}
	 */
	public static final String JSON_KEY_IS_PHEV_ALLOWED = "isPHEVAllowed";	
	/**
	 * JSON key for output currents gathered during simulation
	 * Example: {"outputCurrents": []}
	 */
	public static final String JSON_KEY_CHARGING_STATION_OUTPUT_CURRENTS = "outputCurrents";
	
	
	
	/**
	 * JSON key for car
	 * Example: {"car": {}}
	 */
	public static final String JSON_KEY_CAR = "car";
	/**
	 * JSON key for cars
	 * Example: {"cars": []}
	 */
	public static final String JSON_KEY_CARS = "cars";
	/**
	 * JSON key for car Ah needed to charge at 400V
	 * Example: {"maxCapacity": 94}
	 */
	public static final String JSON_KEY_CAR_MAX_CAPACITY = "maxCapacity";
	/**
	 * JSON key for car current capacity Ah needed to charge at 400V
	 * Example: {"curCapacity": 45}
	 */
	public static final String JSON_KEY_CAR_CUR_CAPACITY = "curCapacity";
	/**
	 * JSON key for car charged capacity Ah (used as a dynamic field for realtime applications)
	 * Example: {"chargedCapacity": 49}
	 */
	public static final String JSON_KEY_CAR_CHARGED_CAPACITY = "chargedCapacity";
	/**
	 * JSON key for car minLoadingState Ah 
	 * Example: {"minLoadingState": 20}
	 */
	public static final String JSON_KEY_CAR_MIN_LOADING_STATE = "minLoadingState";
	/**
	 * JSON key for car min power IN TOTAL (in amps)
	 * Example: {"minPower": 5}
	 */
	public static final String JSON_KEY_CAR_MIN_CURRENT = "minCurrent";
	/**
	 * JSON key for car min power PER PHASE (in amps)
	 * Example: {"minPowerPerPhase": 1.67}
	 */
	public static final String JSON_KEY_CAR_MIN_CURRENT_PER_PHASE = "minCurrentPerPhase";
	/**
	 * JSON key for car max power IN TOTAL (in amps)
	 * Example: {"maxPower": 29}
	 */
	public static final String JSON_KEY_CAR_MAX_CURRENT = "maxCurrent";
	/**
	 * JSON key for car priority (as given by strategyAlgorithmic or MIP in future)
	 * Example: {"priority": 0.000123}
	 */
	public static final String JSON_KEY_CAR_PRIORITY = "priority";
	/**
	 * JSON key for car current plan
	 * Example: {"currentPlan": []}
	 */
	public static final String JSON_KEY_CURRENT_PLAN = "currentPlan";
	
	
	/**
	 * JSON key for car max power PER PHASE  (in amps)
	 * Example: {"maxPowerPerPhase": 29}
	 */
	public static final String JSON_KEY_CAR_MAX_CURRENT_PER_PHASE = "maxCurrentPerPhase";
	/**
	 * JSON key for car canLoadPhase1
	 * Example: {"canLoadPhase1": 1}
	 */
	public static final String JSON_KEY_CAR_CAN_LOAD_PHASE_1 = "canLoadPhase1";
	/**
	 * JSON key for car canLoadPhase2
	 * Example: {"canLoadPhase2": 0.5}
	 */
	public static final String JSON_KEY_CAR_CAN_LOAD_PHASE_2 = "canLoadPhase2";
	/**
	 * JSON key for car canLoadPhase3
	 * Example: {"canLoadPhase3": 0}
	 */
	public static final String JSON_KEY_CAR_CAN_LOAD_PHASE_3 = "canLoadPhase3";
	/**
	 * JSON key for first available timeslot (inclusive)
	 * Example: {"firstAvailableTimeslot": 3}
	 */
	public static final String JSON_KEY_CAR_FIRST_AVAILABLE_TIMESLOT = "firstAvailableTimeslot";
	/**
	 * JSON key for last available timeslot (inclusive)
	 * Example: {"lastAvailableTimeslot": 12}
	 */
	public static final String JSON_KEY_CAR_LAST_AVAILABLE_TIMESLOT = "lastAvailableTimeslot";
	/**
	 * JSON key for timestampArrival (in seconds)
	 * Example: {"timestampArrival": 12345}
	 */
	public static final String JSON_KEY_CAR_TIMESTAMP_ARRIVAL = "timestampArrival";
	/**
	 * JSON key for timestampDeparture (in seconds)
	 * Example: {"timestampDeparture": 54321}
	 */
	public static final String JSON_KEY_CAR_TIMESTAMP_DEPARTURE = "timestampDeparture";
	/**
	 * JSON key for predictedTimestampDeparture (in seconds)
	 * Example: {"predictedTimestampDeparture": 54321}
	 */
	public static final String JSON_KEY_CAR_PREDICTED_TIMESTAMP_DEPARTURE = "predictedTimestampDeparture";
	
	
	/**
	 * JSON key for immediate start
	 * Example: {"immediateStart": true}
	 */
	public static final String JSON_KEY_CAR_IMMEDIATE_START = "immediateStart";
	/** 
	 * JSON key for suspendable charging
	 * Example: {"suspendable": true}
	 */
	public static final String JSON_KEY_CAR_SUSPENDABLE = "suspendable";
	/**
	 * JSON key for variable power charging
	 * Example: {"canUseVariablePower": true}
	 */
	public static final String JSON_KEY_CAR_VARIABLE_POWER = "canUseVariablePower";
	/**
	 * JSON key for input currents during simulation
	 * Example: {"inputCurrents": []}
	 */
	public static final String JSON_KEY_CAR_INPUT_CURRENTS = "inputCurrents";
	
	/**
	 * JSON key for fuse tree
	 * Example: {"fuseTree": {}}
	 */
	public static final String JSON_KEY_FUSE_TREE = "fuseTree";
	/**
	 * JSON key for fuseTreeNodeType
	 * Example: {"fuseTreeNodeType": ""}
	 */
	public static final String JSON_KEY_FUSE_TREE_NODE_TYPE = "fuseTreeNodeType";
	/**
	 * JSON key for fuseTreeNodeType fuse
	 * Example: {"fuseTreeNodeType": "pre-fuse"}
	 */
	public static final String JSON_KEY_FUSE_TREE_NODE_TYPE_FUSE = "pre-fuse";
	/**
	 * JSON key for fuseTreeNodeType chargingStation
	 * Example: {"fuseTreeNodeType": "chargingStation"}
	 */
	public static final String JSON_KEY_FUSE_TREE_NODE_TYPE_CHARGING_STATION = "chargingStation";
	/**
	 * JSON key for fuseTree children
	 * Example: {"children": []}
	 */
	public static final String JSON_KEY_FUSE_TREE_CHILDREN = "children";
	/**
	 * JSON key for root node
	 * Example: {"root": {}}
	 */
	public static final String JSON_KEY_FUSE_TREE_ROOT = "root";
	/**
	 * JSON key for numberChildrenBottomLevel (per 800A fuse)
	 * Example: {"numberChildrenBottomLevel": 4}
	 */
	public static final String JSON_KEY_FUSE_TREE_NUMBER_CHARGING_STATIONS_BOTTOM_LEVEL = "numberChildrenBottomLevel";
	
	/** 
	 * JSON key for index i
	 * Example: {"indexI": 5}
	 */
	public static final String JSON_KEY_INDEX_I = "indexI";
	/**
	 * JSON key for index l
	 * Example: {"indexL": 3}
	 */
	public static final String JSON_KEY_INDEX_L = "indexL";
	
	/** 
	 * JSON key for index n
	 * Example: {"indexN": 3}
	 */
	public static final String JSON_KEY_INDEX_N = "indexN";
	
	
	/*********************
	 * SOLUTION KEYS
	 *********************/
	/**
	 * JSON key for solution
	 * Example: {"solution": {}}
	 */
	public static final String JSON_KEY_SOLUTION = "solution";
	
	/**
	 * JSON key for solution status.
	 * Example: {"solutionStatus": "optimal solution found"}
	 */
	public static final String JSON_KEY_SOLUTION_STATUS = "solutionStatus";
	/**
	 * JSON key for objective value.
	 * Example: {"objectiveValue": -10288.545}
	 */
	public static final String JSON_KEY_OBJECTIVE_VALUE = "objectiveValue";
	/**
	 * JSON key for time constructing the problem
	 * Example: {"timeProblemConstruction": 503}
	 */
	public static final String JSON_KEY_TIME_PROBLEM_CONSTRUCTION = "timeProblemConstruction";
	/**
	 * JSON key for time solving the problem
	 * Example: {"timeSolution": 4030}
	 */
	public static final String JSON_KEY_TIME_SOLUTION = "timeSolution";
	/**
	 * JSON key for response times (for real-time algorithm)
	 * Example: {"timeSolution_array": [10, 3, 500]}
	 */
	public static final String JSON_KEY_TIME_SOLUTION_ARRAY = "timeSolution_array"; 
	/**
	 * JSON key for method
	 * Example: {"method": "greedyLP"} 
	 */
	public static final String JSON_KEY_METHOD = "method";
	
	
	/**
	 * JSON key for variables array in solution
	 * Example: {"variables": []}
	 */
	public static final String JSON_KEY_VARIABLES = "variables";
	/**
	 * JSON key for variable name.
	 * Example: {"variableName": "X_i1_n0"}
	 */
	public static final String JSON_KEY_VARIABLE_NAME = "variableName";
	/**
	 * JSON key for variable value.
	 * Example: {"variableValue": 1}
	 */
	public static final String JSON_KEY_VARIABLE_VALUE = "variableValue";
	/**
	 * JSON key for value objective value (what coefficient does it have in the objective function?).
	 * Example: {"variableObjValue": 0}
	 */
	public static final String JSON_KEY_VARIABLE_OBJECTIVE_COEFFICIENT = "variableObjCoefficient";
	/**
	 * JSON key for variable isInteger.
	 * Example: {"isInteger": true}
	 */
	public static final String JSON_KEY_VARIABLE_IS_INTEGER = "isInteger";
	
	/**
	 * JSON key for csvResult
	 * ExamplE: {"csvResult": "step; current; currentPlanLimit\n 1; 32; 32"}
	 */
	public static final String JSON_KEY_CSV_RESULT = "csvResult";

	/**
	 * JSON key for JSONResult
	 * ExamplE: {"totalPowerUsageOverTime": [
	 *		"unit": ampere,
	 *		"startTime": 00:00:00,
	 *		"endTime": 23:59:59,
	 *		"calculatedData": []
	 * ]}
	 */
	public static final String JSON_KEY_JSON_RESULT = "totalEnergyUsageOverTime";

	/**
	 * JSON key for JSON_RESULT_UNIT
	 * ExamplE: {"calculatedData": [
	 *       {
	 *         "current": 0.0,
	 *         "step": 0
	 *       },
	 * ]}
	 */
	public static final String JSON_KEY_JSON_RESULT_DATA = "calculatedData";

	/**
	 * JSON key for JSON_RESULT_UNIT
	 * ExamplE: {"currentUnit": ampere}
	 */
	public static final String JSON_KEY_JSON_RESULT_CURRENT_UNIT = "currentUnit";

	/**
	 * JSON key for JSON_RESULT_UNIT
	 * ExamplE: {"stepUnit": seconds}
	 */
	public static final String JSON_KEY_JSON_RESULT_STEP_UNIT = "stepUnit";

	/**
	 * JSON key for JSON_RESULT_START_TIME. Format: HH:mm:ss
	 * ExamplE: {"startTime": 00:00:00}
	 */
	public static final String JSON_KEY_JSON_RESULT_START_TIME = "startTime";

	/**
	 * JSON key for JSON_RESULT_END_TIME. Format: HH:mm:ss
	 * ExamplE: {"endTime": 23:59:59}
	 */
	public static final String JSON_KEY_JSON_RESULT_END_TIME = "endTime";

	/**
	 * JSON key for JSON_KEY_JSON_RESULT_STEP
	 * ExamplE: {"step": 32}
	 */
	public static final String JSON_KEY_JSON_RESULT_STEP = "step";

	/**
	 * JSON key for JSON_KEY_JSON_RESULT_CURRENT
	 * ExamplE: {"current": 25}
	 */
	public static final String JSON_KEY_JSON_RESULT_CURRENT = "current";

	/**
	 * JSON key for JSON_KEY_JSON_RESULT_END_STEP
	 * ExamplE: {"endStep": 86399}
	 */
	public static final String JSON_KEY_JSON_RESULT_END_STEP = "endStep";

	/**
	 * JSON key for JSON_KEY_JSON_RESULT_START_STEP
	 * ExamplE: {"startStep": 0}
	 */
	public static final String JSON_KEY_JSON_RESULT_START_STEP = "startStep";

	/**
	 * JSON key for JSON_KEY_JSON_RESULT_INFO
	 * ExamplE: {"info": "Steps that are not displayed have the value of their predecessor"}
	 */
	public static final String JSON_KEY_JSON_RESULT_INFO = "info";


	/*********************
	 * STATE KEYS
	 *********************/
	/**
	 * JSON key for state container.
	 * Example: {"state": {}}
	 */
	public static final String JSON_KEY_STATE = "state";
	/**
	 * JSON key for state current timeslot
	 * Example: {"currentTimeslot": 5}
	 */
	public static final String JSON_KEY_STATE_CURRENT_TIMESLOT = "currentTimeslot";
	/**
	 * JSON key for state start time in seconds
	 * Example: {"startTimeSeconds": 900}
	 */
	public static final String JSON_KEY_STATE_START_TIME_SECONDS = "startTimeSeconds";
	/**
	 * JSON key for state current time in seconds
	 * Example: {"currentTimeSeconds": 900}
	 */
	public static final String JSON_KEY_STATE_CURRENT_TIME_SECONDS = "currentTimeSeconds";
	/**
	 * JSON key for current car assignments
	 * Example: {"currentCarAssignments": []}
	 */
	public static final String JSON_KEY_STATE_CURRENT_CAR_ASSIGNMENTS = "currentCarAssignments";
	/**
	 * JSON key for current unassigned cars
	 * Example: {"currentUnassignedCars": []}
	 */
	public static final String JSON_KEY_STATE_CURRENT_UNASSIGNED_CARS = "currentUnassignedCars";
	
	/**
	 * JSON key for current power assignments
	 * Example: {"currentPowerAssignments": []}
	 */
	public static final String JSON_KEY_STATE_CURRENT_POWER_ASSIGNMENTS = "currentPowerAssignments";
	
	
	
	/**
	 * JSON key for strategy name
	 * Example: {"strategyType": "ALGORITHMIC"}
	 */
	public static final String JSON_KEY_STRATEGY_TYPE = "strategyType";
	
	
	
	
	/*********************
	 * EVENT KEYS
	 *********************/
	
	/**
	 * JSON key for events
	 * Example: {"event": {}}
	 */
	public static final String JSON_KEY_EVENT = "event";
	/**
	 * JSON key for timestamp
	 * Example: {"timestampSeconds": 1234};
	 */
	public static final String JSON_KEY_TIMESTAMP_SECONDS = "timestampSeconds";
	/**
	 * JSON key for event type
	 * Example: {"eventType": "CarArrival"}
	 */
	public static final String JSON_KEY_EVENT_TYPE = "eventType";
	
	
	
	
	/***************************
	 * OCPP KEYS
	 ***************************/
	/**
	 * JSON key for OCPP data
	 * Example: {"ocppData": {}}
	 */
	public static final String JSON_KEY_OCPP_DATA = "ocppData";
	/**
	 * JSON key for OCPP version
	 * Example: {"ocppVersion": 1.6}
	 */
	public static final String JSON_KEY_OCPP_VERSION = "ocppVersion";
	/**
	 * JSON key for charging profile assignments
	 * Example: {"chargingProfileAssignments": []}
	 */
	public static final String JSON_KEY_CHARGING_PROFILE_ASSIGNMENTS = "chargingProfileAssignments";
	/**
	 * JSON key for OCPP charging profile
	 * Example: {"chargingProfile": {}}
	 */
	public static final String JSON_KEY_CHARGING_PROFILE = "chargingProfile";	
	
	
	/**
	 * Exports the CONSTANTS class as one JSON object using reflection. 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject exportConstants() {
		Field[] fields = JSONKeys.class.getFields();
		
		JSONObject result = new JSONObject(new LinkedHashMap<>());
		for (Field field : fields) {
			try {
				result.put(field.getName(), field.get(null));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
		return result;
		
	}
	
	
}
