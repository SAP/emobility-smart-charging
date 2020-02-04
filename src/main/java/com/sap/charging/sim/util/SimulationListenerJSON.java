package com.sap.charging.sim.util;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.charging.realTime.State;
import com.sap.charging.util.JSONKeys;


public class SimulationListenerJSON extends SimulationListenerOutputData
{

    public SimulationListenerJSON()
    {
        super();
    }

    @SuppressWarnings("unchecked")
	public JSONObject getJSONData()
    {
        JSONObject info = new JSONObject();
        info.put(JSONKeys.JSON_KEY_JSON_RESULT_CURRENT_UNIT, "ampere");
        info.put(JSONKeys.JSON_KEY_JSON_RESULT_START_TIME, "00:00:00");
        info.put(JSONKeys.JSON_KEY_JSON_RESULT_END_TIME, "23:59:59");
        info.put(JSONKeys.JSON_KEY_JSON_RESULT_STEP_UNIT, "seconds");
        info.put(JSONKeys.JSON_KEY_JSON_RESULT_INFO, "Steps that are not displayed have the value of their predecessor");
        info.put(JSONKeys.JSON_KEY_JSON_RESULT_START_STEP, 0);
        info.put(JSONKeys.JSON_KEY_JSON_RESULT_END_STEP, rows.size() - 1);
        JSONArray data = new JSONArray();

        double lastSavedRowCurrent = Double.MAX_VALUE;
        for (Row row : rows)
        {
            if(row.current != lastSavedRowCurrent)
            {
                JSONObject rowData = new JSONObject();

                rowData.put(JSONKeys.JSON_KEY_JSON_RESULT_STEP, row.step);
                rowData.put(JSONKeys.JSON_KEY_JSON_RESULT_CURRENT, row.current);

                data.add(rowData);
                lastSavedRowCurrent = row.current;
            }
        }

        info.put(JSONKeys.JSON_KEY_JSON_RESULT_DATA, data);

        return info;
    }

    @Override
    // Ignore
    public void callbackBeforeUpdate(State state)
    {
    }

}
