import { Component, ViewChild, ElementRef } from '@angular/core';
import { websiteDataType } from 'src/global';
import { Car, ChargingStationStore, FuseTree, ChargingStation, Fuse, CarAssignmentStore, FuseTreeNode, FuseTreeNodeUnion, OptimizeChargingProfilesRequest, OptimizeChargingProfilesResponse } from 'src/assets/server_types';
import { MatTable, MatTab, ErrorStateMatcher, MatSelect, MatSnackBar, MatInput } from "@angular/material";
import * as ObservableSlim from "observable-slim";
import { HttpClient } from '@angular/common/http';
import { error } from 'util';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css']
})
export class AppComponent {

    local_storage_key: string = "data"; 

    // Values: Persisted in local storage
    data: websiteDataType = {
        request: this.getInitialRequest(),
        settings: {
            requestJSONExpanded: false,
            responseExpanded: true
        }
    }

    exceptionName: string = ""; 
    exceptionMessage: string = ""; 

    responseJSON: string = ""; 

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
        this.responseJSON = ""; 

        this.httpClient
            .post<OptimizeChargingProfilesResponse>(url, this.data.request)
            .subscribe(response => {
                this.responseJSON = JSON.stringify(response, null, 4); 
                console.log("Result from server:"); 
                console.log(response); 
            }, error => {
                console.log("An error occured."); 
                console.log(error.error); 
                this.setResponseError(error.error); 
            }); 

    }

    resetResponseError(): void {
        this.setResponseError({exceptionName: "", exceptionMessage: ""}); 
    }

    setResponseError(error: {exceptionName: string, exceptionMessage: string}) {
        this.exceptionName = error.exceptionName; 
        this.exceptionMessage = error.exceptionMessage; 
    }

    isPersistedInLocalStorage(): boolean {
        return localStorage.getItem(this.local_storage_key) !== null; 
    }

    initFromLocalStorage(): void {
        let savedData = JSON.parse(localStorage.getItem(this.local_storage_key)); 
        this.data = savedData; 
    }

    persistToLocalStorage(): void {
        console.log("Persisting to local storage..."); 
        localStorage.setItem(this.local_storage_key, JSON.stringify(this.data)); 
        this.openSnackBar("Data has been saved.", "Dismiss");
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

    getRequestJSON(): string {
        return JSON.stringify(this.data.request, null, 4); 
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

    @ViewChild("inputHours", { static: false }) inputHours: ElementRef
    @ViewChild("inputMinutes", { static: false }) inputMinutes: ElementRef
    @ViewChild("inputSeconds", { static: false }) inputSeconds: ElementRef
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

