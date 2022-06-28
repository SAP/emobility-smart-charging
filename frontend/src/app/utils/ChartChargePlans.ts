import { OptimizeChargingProfilesRequest, OptimizeChargingProfilesResponse } from 'src/assets/server_types';
import { chartChargePlansType, chartXAxisRange as chartXAxisRangeType, chartChargePlansSettingsType, websiteSettingsType, chartIndividualChargePlanType } from 'src/global';
import { ChartDataSets, ChartOptions } from 'chart.js';
import { Label } from 'ng2-charts';
import { Utils } from './Utils';


export class ChartChargePlans {

    static timeslotLength: number = 15*60; 

    static getXAxisRange(jsonContent: OptimizeChargingProfilesResponse): chartXAxisRangeType {
        let tMin = Number.MAX_VALUE; // First available car timestamp
        let tMax = -1; // Last available car timestamp 
        
        jsonContent.cars.forEach((car, index) => {
            if (tMin > car.timestampArrival) tMin = car.timestampArrival; 

            if (car.timestampDeparture == 0) {
                // If any car has a timeStamp departure of 0, set tMax = end of day
                tMax = 24*3600; 
            }
            if (tMax < car.timestampDeparture) {
                tMax = car.timestampDeparture; 
            }
        }); 
      
        tMin = Math.max(0, tMin-7*ChartChargePlans.timeslotLength); 
        tMax = Math.min(24*3600, tMax+ChartChargePlans.timeslotLength); 
        
        return {tMin: tMin, tMax: tMax}; 
    }



    static getAggregatedChargePlansData(jsonContent: OptimizeChargingProfilesResponse, xAxisRange: chartXAxisRangeType): {xValues: number[], yValues: number[]} {
        const tMin = xAxisRange.tMin; 
        const tMax = xAxisRange.tMax; 

        // Create values for a step chart
        // A single line: Sum of all charge plans per timeslot
        // Begin xValues with xMin
        const xValues: number[] = [tMin]; 
        const yValues: number[] = [0]; 
        
        for (let timeslot=0; timeslot<24*4; timeslot++) {
            // Each timeslot is 15 minutes
            // Each car has a charge plan with array of length 96
            // Each element is single phase charging in Ampere  
            let t = timeslot * ChartChargePlans.timeslotLength; 
    
            if (t < tMin || t > tMax) continue; 
    
            let sumChargePlansW = jsonContent.cars.map(car => 
                car.currentPlan[timeslot] * (car.canLoadPhase1+car.canLoadPhase2+car.canLoadPhase3) * 230 // How much Watt (W) will this EV draw at 230V?
            ).reduce((a, b) => a+b, 0); 
    
            sumChargePlansW = Utils.roundToNDecimals(sumChargePlansW, 3); 
    
            xValues.push(t); 
            yValues.push(sumChargePlansW); 
        }
    
        // Add a final point at tMax
        xValues.push(tMax); 
        yValues.push(0);         

        return {
            xValues: xValues,
            yValues: yValues
        }; 
    }

    static getIndividualChargePlansData(jsonContent: OptimizeChargingProfilesResponse, xAxisRange: chartXAxisRangeType): 
        {xValues: number[], individualChargePlans: chartIndividualChargePlanType[]} {

        const tMin = xAxisRange.tMin; 
        const tMax = xAxisRange.tMax; 

        // Create values for a step chart
        // A single line: One charge plan
        // Begin xValues with xMin
        const xValues: number[] = [tMin]; 
        const individualChargePlans: chartIndividualChargePlanType[] = []; 

        // Initialize charge plans by looping over cars once
        jsonContent.cars.forEach((car) => {
            individualChargePlans.push({
                carID: car.id,
                carName: car.name, 
                carModelName: car.modelName, 
                yValues: [0]
            })
        }); 

        for (let timeslot=0;timeslot<24*4;timeslot++) {

            // Each timeslot is 15 minutes
            // Each car has a charge plan with array of length 96
            // Each element is single phase charging in Ampere  
            let t = timeslot * ChartChargePlans.timeslotLength; 
    
            if (t < tMin || t > tMax) continue; 
    
            // Add one line per car 
            // Cumulative, example at timeslot=0: 1st car has 22kW. 2nd car also has 22kW, but should show at 44kW (so that the cars stack)
            let cumulativePowerW = 0; 
            jsonContent.cars.forEach((car, index) => {
                let powerW = car.currentPlan[timeslot] * (car.canLoadPhase1+car.canLoadPhase2+car.canLoadPhase3) * 230; 
                individualChargePlans[index].yValues.push(powerW + cumulativePowerW); 

                cumulativePowerW += powerW; 
            }); 

            xValues.push(t); 
        }

        // Add a final point at tMax
        xValues.push(tMax); 
        jsonContent.cars.forEach((car, index) => {
            individualChargePlans[index].yValues.push(0); 
        }); 

        return {
            xValues: xValues,
            individualChargePlans: individualChargePlans
        }; 
    }

    static getAggregatedChargePlans(originalRequest: OptimizeChargingProfilesRequest, 
        jsonContent: OptimizeChargingProfilesResponse, 
        chartSettings: chartChargePlansSettingsType): chartChargePlansType {

        const xAxisRange = this.getXAxisRange(jsonContent); 
        let chartData: ChartDataSets[] = [];  
        let xValues: number[]; 
        
        if (chartSettings.showIndividualCarPlans === false) {
            const chargePlansData = this.getAggregatedChargePlansData(jsonContent, xAxisRange); 
            xValues = chargePlansData.xValues; 
            chartData.push({data: chargePlansData.yValues, label: "Charge plans", steppedLine: true}); 
        }
        else {
            const chargePlansData = this.getIndividualChargePlansData(jsonContent, xAxisRange); 
            xValues = chargePlansData.xValues; 
            
            chargePlansData.individualChargePlans.forEach((individualChargePlan) => {
                chartData.push({data: individualChargePlan.yValues, label: "Car id=" + individualChargePlan.carID, steppedLine: true})
            });
            

        }
        
        let chartLabels: Label[] = xValues.map(n => Utils.toHH_MM(n)); 
    
        let result: chartChargePlansType = {
            chartData: chartData, 
            chartLabels: chartLabels,
            chartOptions: { 
                responsive: true,
                scales: {
                    xAxes: [{
                        scaleLabel: {
                            display: true,
                            labelString: 'Time'
                        }
                    }],
                    yAxes: [{
                        // Add buffer so that infrastructure annotation label is not cut off
                        ticks: { suggestedMax: Utils.getInfrastructureLimitW(originalRequest.state.fuseTree)*1.1 },
                        scaleLabel: {
                            display: true,
                            labelString: 'Power (W)'
                        }
                    }]
                },
                annotation: this.getChartAnnotations(originalRequest, jsonContent, chartSettings)
            } as ChartOptions,
            chartColors: [{
                borderColor: 'black',
                backgroundColor: 'rgba(255,255,0,0.28)',
            }],
            chartLegend: true, 
            chartPlugins: [],
            chartType: "line"
        }; 
        console.log("Visualizing chart with data:"); 
        console.log(result); 
        return result; 
    }

    static getChartAnnotations(originalRequest: OptimizeChargingProfilesRequest, 
        jsonContent: OptimizeChargingProfilesResponse, 
        chartSettings: chartChargePlansSettingsType) {

        const maxInfrastructurePowerW = Utils.getInfrastructureLimitW(originalRequest.state.fuseTree); 

        // Use time=now (vertical line)
        const timeNow = originalRequest.state.currentTimeSeconds;  

        const annotations = []; 

        if (chartSettings.showInfrastructureLimit === true) {
            annotations.push({
                // Infrastructure limit horizontal line
                type: 'line',
                mode: 'horizontal',
                scaleID: 'y-axis-0',
                value: maxInfrastructurePowerW,
                borderColor: 'red',
                borderWidth: 2,
                borderDash: [5, 5],
                label: {
                    enabled: true,
                    content: "Infrastructure limit (root fuse): " + maxInfrastructurePowerW + "W",
                    yAdjust: -17
                }
            }); 
        }

        if (chartSettings.showCurrentTime === true) {
            annotations.push({
                // Current time vertical line
                type: 'line',
                mode: 'vertical',
                scaleID: 'x-axis-0',
                value: Utils.toHH_MM(timeNow),
                borderColor: 'blue',
                borderWidth: 2,
                borderDash: [5, 5],
                label: {
                    enabled: true,
                    content: "Current time",
                    xAdjust: -45
                }
            }); 
        }

        // https://codepen.io/jordanwillis/pen/qrXJLW
        return {
            annotations: annotations
        }
    }

}
