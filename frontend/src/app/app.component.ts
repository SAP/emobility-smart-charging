import { Component, ViewChild, ElementRef } from '@angular/core';
import { websiteDataType, restApiResponseType, restApiResponseErrorType, chartAggregatedChargePlansType } from 'src/global';
import { Car, ChargingStationStore, FuseTree, ChargingStation, Fuse, CarAssignmentStore, FuseTreeNode, FuseTreeNodeUnion, OptimizeChargingProfilesRequest, OptimizeChargingProfilesResponse } from 'src/assets/server_types';
import { MatTable, MatTab, ErrorStateMatcher, MatSelect, MatSnackBar, MatInput } from "@angular/material";
import * as ObservableSlim from "observable-slim";
import { HttpClient } from '@angular/common/http';
import { error } from 'util';
import { ChartDataSets } from 'chart.js';
import { Label, Color } from 'ng2-charts';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css']
})
export class AppComponent {

    
    localStorageKey: string = "data"; 

    // Data structure persisted in local storage
    data: websiteDataType = {
        request: this.getInitialRequest(),
        settings: {
            requestJSONExpanded: false,
            responseExpanded: true
        }
    }

    // Response from server and potential error
    restApiResponse: restApiResponseType = {
        jsonContent: null,
        chartAggregatedChargePlans: null, 
        error: {
            name: "",
            message: ""
        }
    }



    constructor(private _snackBar: MatSnackBar,
        private httpClient: HttpClient) {

        let dataInLocalStorage = this.isPersistedInLocalStorage();
        if (dataInLocalStorage === true) {
            console.log("Initializing from local storage..."); 
            this.initFromLocalStorage();
        } 
        else {
            console.log("No data found in local storage, using initial values"); 
        }
        console.log(this.data); 

        this.applyObserverFunctions(this.data);
    }


    onClickResetData(): void {
        this.data.request = this.getInitialRequest(); 
    }

    onClickSendRequest(): void {
        //const  headers = new  HttpHeaders().set("X-CustomHttpHeader", "CUSTOM_VALUE");

        let port = (window.location.port === "4200") ? 8080 : window.location.port; // If in angular dev environment: Use 8080 (port of server in dev environment)

        let url = window.location.protocol + "//" + window.location.hostname + ":" + port + "/api/v1/OptimizeChargingProfiles"; 
        this.resetResponseError(); 
        this.restApiResponse.jsonContent = null; 

        this.httpClient
            .post<OptimizeChargingProfilesResponse>(url, this.data.request)
            .subscribe(response => {
                this.restApiResponse.jsonContent = response; 
                this.restApiResponse.chartAggregatedChargePlans = this.getChartAggregatedChargePlans(response); 
                console.log("Result from server:"); 
                console.log(response); 
            }, error => {
                console.log("An error occured."); 
                console.log(error); 
                this.setResponseError(error); 
            }); 

    }

    resetResponseError(): void {
        this.setResponseError({name: "",  message: ""}); 
    }

    setResponseError(error: restApiResponseErrorType) {
        this.restApiResponse.error = error; 
    }

    getChartAggregatedChargePlans(jsonContent: OptimizeChargingProfilesResponse): chartAggregatedChargePlansType {
        const timeslotLength = 15*60; 

        let tMin = Number.MAX_VALUE; // First available car timestamp minus 15 minutes 
        let tMax = -1; // Last available car timestamp plus 15 minutes 
        
        jsonContent.cars.forEach((car, index) => {
            if (tMin > car.timestampArrival) tMin = car.timestampArrival; 
            if (tMax < car.timestampDeparture) tMax = car.timestampDeparture; 
        }); 

        if (tMax === 0) {
            tMax = 24*3600; 
        }
      
        tMin = Math.max(0, tMin-timeslotLength); 
        tMax = Math.min(24*3600, tMax+timeslotLength); 


        // Create a step chart
        // Begin xValues with xMin
        const xValues: number[] = [tMin]; 
        const yValues: number[] = [0]; 
        
        for (let timeslot=0; timeslot<24*4; timeslot++) {
            // Each timeslot is 15 minutes
            // Each car has a charge plan with array of length 96
            // Each element is single phase charging in Ampere  
            let t = timeslot * timeslotLength; 

            if (t < tMin || t > tMax) continue; 

            let sumChargePlansW = jsonContent.cars.map(car => 
                car.currentPlan[timeslot] * (car.canLoadPhase1+car.canLoadPhase2+car.canLoadPhase3) * 230 // How much Watt (W) will this EV draw at 230V?
            ).reduce((a, b) => a+b, 0); 

            sumChargePlansW = Math.round(sumChargePlansW*1000) / 1000;  

            xValues.push(t); 
            yValues.push(sumChargePlansW); 
        }

        // Add a final point at tMax
        xValues.push(tMax); 
        yValues.push(0);         


        let chartData: ChartDataSets[] = [
            {data: yValues, label: "Charge plans", steppedLine: true} 
        ]; 
        let chartLabels: Label[] = xValues.map(n => this.toHH_MM(n)); 

        let result: chartAggregatedChargePlansType = {
            chartData: chartData, 
            chartLabels: chartLabels,
            chartOptions: { 
               // steppedLine: 'before',
                responsive: true,
            },
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


    isPersistedInLocalStorage(): boolean {
        return localStorage.getItem(this.localStorageKey) !== null; 
    }

    initFromLocalStorage(): void {
        let savedData = JSON.parse(localStorage.getItem(this.localStorageKey)); 
        this.data = savedData; 
    }

    persistToLocalStorage(): void {
        console.log("Persisting to local storage..."); 
        localStorage.setItem(this.localStorageKey, JSON.stringify(this.data)); 
        this.openSnackBar("Data and settings have been saved.", "Dismiss");
    }

    applyObserverFunctions(data: websiteDataType) {
        try {
            var self = this;
            this.data = ObservableSlim.create(data, false, function (changes) {
                self.persistToLocalStorage.call(self);
            });
        }
        catch (error) {
            console.log("Error loading data from local storage!");
            console.log(error);
        }
    }

    getNextFuseID(): number {
        let highestFuseID = -1;
        this.traverseFuses(this.data.request.state.fuseTree.rootFuse, function(fuse: Fuse) {
            if (fuse.id > highestFuseID)
                highestFuseID = fuse.id; 
        }); 
        return highestFuseID+1; 
    }
    getNextChargingStationID(): number {
        let highestID = -1;
        this.traverseFuses(this.data.request.state.fuseTree.rootFuse, function(fuse: Fuse) {
            for (let child of fuse.children) {
                if (child["@type"] === "ChargingStation" && child.id > highestID) {
                    highestID = child.id; 
                }
            }
        }); 
        return highestID+1; 
    }

    deleteFuseTreeNode(fuseTreeNode: FuseTreeNode) {
        console.log("Removing " + fuseTreeNode["@type"] + " (id=" + fuseTreeNode.id + ") from tree..."); 
        // Traverse tree until found
        this.traverseFuses(this.data.request.state.fuseTree.rootFuse, function(fuse: Fuse) {
            if (fuse.children.indexOf(fuseTreeNode as FuseTreeNodeUnion) !== -1) {
                fuse.children.splice(fuse.children.indexOf(fuseTreeNode as FuseTreeNodeUnion), 1); 
                return; 
            }
        });
    }

    traverseFuses(fuse, callbackFunction) {
        let fuses: Array<FuseTreeNode> = [fuse]; 
        while (fuses.length > 0) {
            let currentFuse = fuses.pop();
            if (!currentFuse.children) continue; 
            
            callbackFunction(currentFuse); 

            for (let child of currentFuse.children) {
                fuses.push(child); 
            }
        }
    }

    addFuseChild(fuse: Fuse) {
        let newFuse = this.buildFuse(this.getNextFuseID(), 32); 
        fuse.children.push(newFuse); 
    }
    addChargingStationChild(fuse: Fuse) {
        let newChargingStation = this.buildChargingStation(this.getNextChargingStationID()); 
        fuse.children.push(newChargingStation); 
    }


    addCar(chargingStation: ChargingStation): void {
        let newCar = this.buildCar(this.getNumberOfCars()); 
        this.data.request.state.cars.push(newCar); 
        this.data.request.state.carAssignments.push({
            carID: newCar.id,
            chargingStationID: chargingStation.id
        }); 
    }
    deleteCar(chargingStation: ChargingStation): void {
        let carAssignment = this.getCarAssignment(chargingStation.id); 
        let car = this.getCar(carAssignment.carID); 
        this.getCars().splice(this.getCars().indexOf(car), 1);
        this.getCarAssignments().splice(this.getCarAssignments().indexOf(carAssignment), 1); 
    }
    getNumberOfCars(): number {
        return this.data.request.state.cars.length; 
    }
    getCars(): Array<Car> {
        return this.data.request.state.cars; 
    }
    getCar(carID: number) {
        for (let car of this.data.request.state.cars) {
            if (car.id === carID) {
                return car; 
            }
        }
        return null; 
    }

    getCarAssignments(): Array<CarAssignmentStore> {
        return this.data.request.state.carAssignments; 
    } 
    getCarAssignment(chargingStationID: number): CarAssignmentStore | null {
        for (let carAssignment of this.data.request.state.carAssignments) {
            if (carAssignment.chargingStationID === chargingStationID) {
                return carAssignment; 
            }
        }
        return null; 
    }

    getAssignedCar(chargingStationID: number) {
        let carAssignment = this.getCarAssignment(chargingStationID); 
        if (carAssignment !== null) {
            return this.getCar(carAssignment.carID); 
        }
        return null; 
    }

    buildCar(id: number): Car {
        return {
            id: id,
            canLoadPhase1: 1,
            canLoadPhase2: 1,
            canLoadPhase3: 1,
            timestampArrival: 8*3600,
            carType: "BEV",
            maxCapacity: 100,
            minLoadingState: 50,
            startCapacity: 10,
            minCurrent: 18,
            minCurrentPerPhase: 6,
            maxCurrent: 96,
            maxCurrentPerPhase: 32
        }
    }

    getInitialRequest(): OptimizeChargingProfilesRequest {
        let fuseTree = this.getInitialFuseTree();
        return {
            event: {
                eventType: "Reoptimize"
            },
            state: {
                currentTimeSeconds: 12*3600,
                cars: [this.buildCar(0)],
                fuseTree: fuseTree,
                carAssignments: [{
                    carID: 0,
                    chargingStationID: 0
                }]
            }
        }
    }

    getInitialFuseTree(): FuseTree {

        let fuse0 = this.buildFuse(0, 144);
        let fuse1_0 = this.buildFuse(1, 77); 
        let fuse1_1 =  this.buildFuse(2, 77); 
        fuse0.children = [fuse1_0, fuse1_1]; 

        let chargingStation1 = this.buildChargingStation(0); 
        let chargingStation2 = this.buildChargingStation(1); 
        fuse1_0.children = [chargingStation1, chargingStation2]; 
        let chargingStation3 = this.buildChargingStation(2); 
        let chargingStation4 = this.buildChargingStation(3);
        fuse1_1.children = [chargingStation3, chargingStation4];  
        
        let fuseTree = {
            rootFuse: fuse0
        };
        return fuseTree; 
    }

    getChargingStationsFromFuseTree(fuseTree: FuseTree) {
        let chargingStations = []; 
        this.traverseFuses(fuseTree.rootFuse, (fuse: Fuse) => {
            for (let child of fuse.children) {
                if (child["@type"] === "ChargingStation") {
                    chargingStations.push(child); 
                }
            }
        }); 
        return chargingStations; 
    }

    buildFuse(id: number, fuseSize: number): Fuse {
        return {
            "@type": "Fuse",
            id: id,
            fusePhase1: fuseSize,
            fusePhase2: fuseSize,
            fusePhase3: fuseSize,
            children: []
        }
    }

    buildChargingStation(id: number): ChargingStation {
        return {
            "@type": "ChargingStation",
            id: id,
            fusePhase1: 32,
            fusePhase2: 32,
            fusePhase3: 32
        }
    }

    toHH_MM_SS(secondsAfterMidnight): string {
        var date = new Date(null);
        date.setSeconds(secondsAfterMidnight);
        var result = date.toISOString().substr(11, 8);
        return result; 
    }
    toHH_MM(secondsAfterMidnight): string {
        var date = new Date(null);
        date.setSeconds(secondsAfterMidnight);
        var result = date.toISOString().substr(11, 5);
        return result; 
    }

    getTimeHours() {
        return Math.floor(this.data.request.state.currentTimeSeconds / 3600); 
    }
    getTimeMinutes() {
        return Math.floor((this.data.request.state.currentTimeSeconds % 3600) / 60); 
    }
    getTimeSeconds() {
        return this.data.request.state.currentTimeSeconds % 60; 
    }

    @ViewChild("inputHours") inputHours: ElementRef
    @ViewChild("inputMinutes") inputMinutes: ElementRef
    @ViewChild("inputSeconds") inputSeconds: ElementRef
    refreshCurrentTimeSeconds() {
        this.data.request.state.currentTimeSeconds = parseInt(this.inputHours.nativeElement.value)*3600 + parseInt(this.inputMinutes.nativeElement.value)*60 + parseInt(this.inputSeconds.nativeElement.value); 
    }

    floor(n: number) {
        return Math.floor(n); 
    }

    openSnackBar(message: string, action: string) {
        this._snackBar.open(message, action, {
            duration: 1000,
        });
    }


}

