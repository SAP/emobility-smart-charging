import { OptimizeChargingProfilesRequest, Car, OptimizeChargingProfilesResponse } from "./assets/server_types";
import { ChartDataSets, ChartType } from "chart.js";
import { Label, Color } from "ng2-charts";


declare type websiteDataType = {
    request: OptimizeChargingProfilesRequest,
    settings: websiteSettings
}


declare type websiteSettings = {
    requestJSONExpanded: boolean,
    responseExpanded: boolean
}


declare type restApiResponseErrorType = {
    name: string, 
    message: string
}

declare type restApiResponseType = {
    jsonContent: OptimizeChargingProfilesResponse | null, 
    chartAggregatedChargePlans: chartAggregatedChargePlansType | null, 
    error: restApiResponseErrorType
}







// Chart data types
// https://www.npmjs.com/package/ng2-charts


// Used for graph in result to show sum of all charge plans over time
declare type chartAggregatedChargePlansType = {
    chartData: ChartDataSets[],
    chartLabels: Label[],
    chartOptions: {
        responsive: boolean,
        steppedLine?: any
    },
    chartColors: Color[],
    chartLegend: boolean, 
    chartPlugins: any[],
    chartType: ChartType
}





