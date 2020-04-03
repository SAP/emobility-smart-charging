import { OptimizeChargingProfilesRequest, Car, OptimizeChargingProfilesResponse, Phase } from "./assets/server_types";
import { ChartDataSets, ChartType, ChartOptions } from "chart.js";
import { Label, Color } from "ng2-charts";


declare type websiteDataType = {
    request: OptimizeChargingProfilesRequest,
    settings: websiteSettingsType
}


declare type websiteSettingsType = {
    requestJSONExpanded: boolean,
    responseExpanded: boolean,
    chartChargePlans: chartChargePlansSettingsType
}


declare type restApiResponseErrorType = {
    name: string, 
    message: string
}

declare type restApiResponseType = {
    jsonContent: OptimizeChargingProfilesResponse | null, 
    chartAggregatedChargePlans: chartChargePlansType | null, 
    error: restApiResponseErrorType
}

// not explicitly exported by server classes
declare type phaseMatchingType = { PHASE_1: Phase, PHASE_2: Phase, PHASE_3: Phase }; 




// Chart data types
// https://www.npmjs.com/package/ng2-charts


// Used for graph in result to show sum of all charge plans over time
declare type chartChargePlansType = {
    chartData: ChartDataSets[],
    chartLabels: Label[],
    chartOptions: ChartOptions, 
    chartColors: Color[],
    chartLegend: boolean, 
    chartPlugins: any[],
    chartType: ChartType
}

declare type chartXAxisRange = {
    tMin: number, 
    tMax: number
}

declare type chartChargePlansSettingsType = {
    showInfrastructureLimit: boolean,
    showCurrentTime: boolean,
    showIndividualCarPlans: boolean
}

declare type chartIndividualChargePlanType = {
    carID: number,
    carName: string,
    carModelName: string, 
    yValues: number[]
}



// SVG Circuit diagram types
declare type circuitDiagramSettingsType = {
    marginLeft: number,
    marginTop: number,
    marginBottom: number, 
    
    rowHeight: number,
    rowBuffer: number, 
    columnWidth: number,

    // margin between cables
    cable: {
        phaseMargin: number,
        lengthWithoutPhases: number,
        horizontalX2Offset: number
    },
    connectorPoint: {
        radius: number
    }; 

    // The fuse rectangle itself
    fuse: {
        width: number,
        height: number
    },

    chargeStation: {
        width: number, 
        height: number
    },

    legend: {
        width: number, 
        // Height is computed from 2*phaseMargin + 2*padding
        padding: number,
        marginTop: number, 
        marginRight: number
    }

}; 
