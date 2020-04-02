import { Component, ViewChild, ElementRef } from '@angular/core';
import { websiteDataType, restApiResponseType, restApiResponseErrorType, chartChargePlansType, websiteSettingsType } from 'src/global';
import { Car, ChargingStationStore, FuseTree, ChargingStation, Fuse, CarAssignmentStore, FuseTreeNode, FuseTreeNodeUnion, OptimizeChargingProfilesRequest, OptimizeChargingProfilesResponse } from 'src/assets/server_types';
import { MatTable, MatTab, ErrorStateMatcher, MatSelect, MatSnackBar, MatInput, MatDialog } from "@angular/material";
import * as ObservableSlim from "observable-slim";
import { HttpClient } from '@angular/common/http';
import { ChartDataSets, ChartOptions } from 'chart.js';
import { Label, Color } from 'ng2-charts';

import * as ChartAnnotation from 'chartjs-plugin-annotation';
import * as Chart from 'chart.js';
import { ChartChargePlans } from './utils/ChartChargePlans';
import { FuseTreeCircuitDiagramDialog } from './fuse-tree-circuit-diagram/fuse-tree-circuit-diagram-dialog';

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
        settings: this.getInitialSettings()
    }

    originalApiRequest: OptimizeChargingProfilesRequest = null;

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
        private httpClient: HttpClient,
        public fuseTreeCircuitDiagramDialog: MatDialog) {

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

    ngOnInit() {
        // Register annotations in chart
        // https://stackoverflow.com/questions/51664741/chartjs-plugin-annotations-not-displayed-in-angular-5
        let namedChartAnnotation = ChartAnnotation;
        namedChartAnnotation["id"] = "annotation";
        Chart.pluginService.register(namedChartAnnotation);

    }

    onClickShowFuseTreeCircuitDiagram(): void {
        const dialogRef = this.fuseTreeCircuitDiagramDialog.open(FuseTreeCircuitDiagramDialog, {
            width: "95%",
            height: "95%",
            data: {
                state: this.data.request.state,
                appParent: this
            }
        });

        dialogRef.afterClosed().subscribe(result => {
            console.log('The dialog was closed');
        });
    }

    onClickResetData(): void {
        this.data.request = this.getInitialRequest();
        this.data.settings = this.getInitialSettings();
    }

    onClickSendRequest(): void {

        let port = (window.location.port === "4200") ? 8080 : window.location.port; // If in angular dev environment: Use 8080 (port of server in dev environment)

        let url = window.location.protocol + "//" + window.location.hostname + ":" + port + "/api/v1/OptimizeChargingProfiles";
        this.resetResponseError();
        this.restApiResponse.jsonContent = null;

        // Save original request for result chart (in case currentTimeSeconds and the fuseTree is changed)
        this.originalApiRequest = this.deepClone(this.data.request) as OptimizeChargingProfilesRequest;

        this.httpClient
            .post<OptimizeChargingProfilesResponse>(url, this.data.request)
            .subscribe(response => {
                this.restApiResponse.jsonContent = response;
                this.refreshChartChargePlans();
                console.log("Result from server:");
                console.log(response);
            }, error => {
                console.log("An error occured.");
                console.log(error);
                if (error.error) {
                    // For REST API exceptions, use message returned by server
                    // For semantic errors (example: try to optimize with car that hasn't arrived yet)
                    if (error.error.exceptionName) {
                        this.setResponseError({
                            name: error.error.exceptionName + " (Status: " + error.status + ")",
                            message: error.error.exceptionMessage
                        });
                    }
                    else {
                        // For syntax errors (example: JSON could not be constructed)
                        this.setResponseError({
                            name: error.error.status + " (Status: " + error.status + ")",
                            message: error.error.message
                        });
                    }
                }
                else {
                    // For network exceptions, use browser error 
                    this.setResponseError(error);
                }

            });
    }

    resetResponseError(): void {
        this.setResponseError({ name: "", message: "" });
    }

    setResponseError(error: restApiResponseErrorType) {
        this.restApiResponse.error = error;
    }

    refreshChartChargePlans() {
        console.log("Refreshing chart...");
        this.restApiResponse.chartAggregatedChargePlans = ChartChargePlans.getAggregatedChargePlans(this.originalApiRequest, this.restApiResponse.jsonContent, this.data.settings.chartChargePlans);
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
        this.traverseFuses(this.data.request.state.fuseTree.rootFuse, function (fuse: Fuse) {
            if (fuse.id > highestFuseID)
                highestFuseID = fuse.id;
        });
        return highestFuseID + 1;
    }
    getNextChargingStationID(): number {
        let highestID = -1;
        this.traverseFuses(this.data.request.state.fuseTree.rootFuse, function (fuse: Fuse) {
            for (let child of fuse.children) {
                if (child["@type"] === "ChargingStation" && child.id > highestID) {
                    highestID = child.id;
                }
            }
        });
        return highestID + 1;
    }

    deleteFuseTreeNode(fuseTreeNodeParam: FuseTreeNode) {
        console.log("Deleting " + fuseTreeNodeParam["@type"] + " (id=" + fuseTreeNodeParam.id + ") from tree...");
        // Traverse tree until found
        // fuseTreeNodeParam can be charging station or fuse
        this.traverseFuses(this.data.request.state.fuseTree.rootFuse, function (fuse: FuseTreeNode) {
            if (fuse.children.indexOf(fuseTreeNodeParam as FuseTreeNodeUnion) !== -1) {

                // Also delete all EVs, charging stations and carAssignments that are children of this fuseTreeNode
                this.deleteAssignedCars(fuseTreeNodeParam);

                fuse.children.splice(fuse.children.indexOf(fuseTreeNodeParam as FuseTreeNodeUnion), 1);

                return;
            }
        });
    }

    deleteAssignedCars(fuseTreeNodeParam: FuseTreeNode): void {
        console.log("Deleting all assigned cars under " + fuseTreeNodeParam["@type"] + " (id=" + fuseTreeNodeParam.id + ")");
        this.traverseFuses(fuseTreeNodeParam, function (fuseTreeNode: FuseTreeNode) {

            for (let child of fuseTreeNode.children) {
                console.log("Checking " + child["@type"] + " id=" + child.id + " for cars...");
                if (child["@type"] === "ChargingStation") {
                    if (this.isChargingStationAssigned(child) === true) {
                        this.deleteCar(child);
                    }
                }
            }

        });
    }

    traverseFuses(fuse, callbackFunction) {
        let fuses: Array<FuseTreeNode> = [fuse];
        while (fuses.length > 0) {
            let currentFuse = fuses.pop();
            if (!currentFuse.children) continue;

            callbackFunction.call(this, currentFuse);

            for (let child of currentFuse.children) {
                fuses.push(child);
            }
        }
    }

    getMaximumFuseTreeNodeDepth(fuseTreeNode: FuseTreeNode, depth: number): number {
        // Charging station has no fuse children
        if (fuseTreeNode.children === undefined || fuseTreeNode.children.length === 0) return depth;

        let maxDepth = 0;
        for (const child of fuseTreeNode.children) {
            const childDepth = this.getMaximumFuseTreeNodeDepth(child, depth + 1);
            if (childDepth > maxDepth) {
                maxDepth = childDepth;
            }
        }
        return maxDepth;
    }

    addFuseChild(fuse: Fuse) {
        let newFuse = this.buildFuse(this.getNextFuseID(), 32);
        fuse.children.push(newFuse);
    }
    addChargingStationChild(fuse: Fuse) {
        let newChargingStation = this.buildChargingStation(this.getNextChargingStationID());
        fuse.children.push(newChargingStation);
    }
    isChargingStationAssigned(chargingStation: ChargingStation) {
        return this.getCarAssignment(chargingStation.id) !== null;
    }

    getNextCarID(): number {
        let maxID = -1;
        this.data.request.state.cars.forEach((car) => {
            if (maxID < car.id) {
                maxID = car.id;
            }
        });
        return maxID + 1;
    }

    addCar(chargingStation: ChargingStation): void {
        let newCar = this.buildCar(this.getNextCarID());
        this.data.request.state.cars.push(newCar);
        this.data.request.state.carAssignments.push({
            carID: newCar.id,
            chargingStationID: chargingStation.id
        });
    }
    deleteCar(chargingStation: ChargingStation): void {
        let carAssignment = this.getCarAssignment(chargingStation.id);
        let car = this.getCar(carAssignment.carID);
        console.log("Deleting car with id=" + car.id + " and car assignment");
        this.getCars().splice(this.getCars().indexOf(car), 1);
        this.getCarAssignments().splice(this.getCarAssignments().indexOf(carAssignment), 1);
    }
    getNumberOfCars(): number {
        return this.data.request.state.cars.length;
    }
    getCars(): Array<Car> {
        return this.data.request.state.cars;
    }
    getCar(carID: number): Car | null {
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

    getAssignedCar(chargingStationID: number): Car | null {
        let carAssignment = this.getCarAssignment(chargingStationID);
        if (carAssignment !== null) {
            return this.getCar(carAssignment.carID);
        }
        return null;
    }
    getAssignedCarByFuseTreeNode(fuseTreeNode: FuseTreeNode): Car | null {
        if (fuseTreeNode["@type"] === "ChargingStation") {
            return this.getAssignedCar(fuseTreeNode.id);
        }
        else {
            return null;
        }
    }


    buildCar(id: number): Car {
        return {
            id: id,
            canLoadPhase1: 1,
            canLoadPhase2: 1,
            canLoadPhase3: 1,
            timestampArrival: 8 * 3600,
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

    getInitialSettings(): websiteSettingsType {
        return {
            requestJSONExpanded: false,
            responseExpanded: true,
            chartChargePlans: {
                showCurrentTime: true,
                showIndividualCarPlans: true,
                showInfrastructureLimit: true
            }
        };
    }

    getInitialRequest(): OptimizeChargingProfilesRequest {
        let fuseTree = this.getInitialFuseTree();
        return {
            event: {
                eventType: "Reoptimize"
            },
            state: {
                currentTimeSeconds: 12 * 3600,
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
        let fuse1_1 = this.buildFuse(2, 77);
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
            fusePhase3: 32,
            // Initial charge stations have no phase rotation
            phaseToGrid: {
                PHASE_1: "PHASE_1",
                PHASE_2: "PHASE_2",
                PHASE_3: "PHASE_3" 
            },
            phaseToChargingStation: {
                PHASE_1: "PHASE_1", 
                PHASE_2: "PHASE_2",
                PHASE_3: "PHASE_3"
            }
        }; 
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
        this.data.request.state.currentTimeSeconds = parseInt(this.inputHours.nativeElement.value) * 3600 + parseInt(this.inputMinutes.nativeElement.value) * 60 + parseInt(this.inputSeconds.nativeElement.value);
    }

    floor(n: number) {
        return Math.floor(n);
    }

    openSnackBar(message: string, action: string) {
        this._snackBar.open(message, action, {
            duration: 1000,
        });
    }

    deepClone(object: any): any {
        return JSON.parse(JSON.stringify(object));
    }

}

