import { Component, OnInit, Input, Inject } from '@angular/core';
import { FuseTreeNode, CarAssignmentStore, Car, Fuse, ChargingStation } from 'src/assets/server_types';
import { AppComponent } from '../app.component';
import { Utils } from '../utils/Utils';

import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { phaseMatchingType } from 'src/global';
import { MatCheckboxChange } from '@angular/material';

@Component({
    selector: 'app-fuse-tree-component',
    templateUrl: './fuse-tree-component.component.html',
    styleUrls: ['./fuse-tree-component.component.css']
})
export class FuseTreeComponentComponent implements OnInit {

    @Input() fuseTreeNode: FuseTreeNode;
    @Input() depth: number;
    @Input() appParent: AppComponent;


    constructor(public editFuseTreeComponentDialog: MatDialog,
        public editCarDialog: MatDialog) {

    }

    getAssignedCar(): Car | null {
        const result = this.appParent.getAssignedCarByFuseTreeNode(this.fuseTreeNode); 
        return result;  
    }

    getFuseTreeNodeLabel(fuseTreeNode: FuseTreeNode): string {
        return Utils.getFuseTreeNodeLabel(fuseTreeNode); 
    }


    onClickDeleteFuseTreeNode() {
        this.appParent.deleteFuseTreeNode(this.fuseTreeNode);
    }

    onClickAddFuseChild() {
        this.appParent.addFuseChild(this.fuseTreeNode as Fuse);
    }
    onClickAddChargingStationChild() {
        this.appParent.addChargingStationChild(this.fuseTreeNode as Fuse);
    }
    onClickAddCar() {
        this.appParent.addCar(this.fuseTreeNode as ChargingStation);
    }
    onClickDeleteCar() {
        this.appParent.deleteCar(this.fuseTreeNode as ChargingStation);
    }

    isTimestampArrivalValid(): boolean {
        return this.appParent.data.request.state.currentTimeSeconds >= this.getAssignedCar().timestampArrival;
    }

    toHH_MM(timestamp: number): string {
        return Utils.toHH_MM(timestamp);
    }

    getCarPowerLabel(): string {
        return Utils.getCarLabel(this.getAssignedCar()); 
    }

    ngOnInit() {

    }

    onClickEditFuseTreeNode(fuseTreeNode: FuseTreeNode): void {
        const dialogRef = this.editFuseTreeComponentDialog.open(EditFuseTreeComponentDialog, {
            width: "75%",
            data: {
                fuseTreeNode: fuseTreeNode,
                appParent: this.appParent
            } 
        });

        dialogRef.afterClosed().subscribe(result => {
            console.log('The dialog was closed');
        });
    }
    onClickEditCar(car: Car): void {
        const dialogRef = this.editCarDialog.open(EditCarDialog, {
            width: "75%",
            data: {
                car: car,
                appParent: this.appParent 
            }
        });

        dialogRef.afterClosed().subscribe(result => {
            console.log('The dialog was closed');
        });
    }

}

@Component({
    selector: 'edit-fuse-tree-component-dialog',
    templateUrl: './edit-fuse-tree-component-dialog.html',
})
export class EditFuseTreeComponentDialog {

    phaseMatchings: Array<{label: string, value: phaseMatchingType}> = [{
        label: "Phase 1 ⟷ Phase 1;Phase 2 ⟷ Phase 2; Phase 3 ⟷ Phase 3",
        value: {
            PHASE_1: "PHASE_1",
            PHASE_2: "PHASE_2",
            PHASE_3: "PHASE_3"
        }
    }, {
        label: "Phase 1 ⟷ Phase 2;Phase 2 ⟷ Phase 3;Phase 3 ⟷ Phase 1",
        value: {
            PHASE_1: "PHASE_2",
            PHASE_2: "PHASE_3",
            PHASE_3: "PHASE_1"
        }
    }, {
        label: "Phase 1 ⟷ Phase 3;Phase 2 ⟷ Phase 1;Phase 3 ⟷ Phase 2",
        value: {
            PHASE_1: "PHASE_3",
            PHASE_2: "PHASE_1",
            PHASE_3: "PHASE_2"
        }
    }];

    constructor(
        public dialogRef: MatDialogRef<EditFuseTreeComponentDialog>,
        @Inject(MAT_DIALOG_DATA) public data: {
            fuseTreeNode: FuseTreeNode,
            appParent: AppComponent
        }) { }


    getPowerW(): number {
        return Utils.getFusePowerLimitW(this.data.fuseTreeNode);
    }

    getChargingStationPhaseMatching(): phaseMatchingType {
        return this.data.fuseTreeNode["phaseToGrid"] as phaseMatchingType; 
    }
    getChargingStationPhaseMatchingToChargingStation(): phaseMatchingType {
        return this.data.fuseTreeNode["phaseToChargingStation"] as phaseMatchingType; 
    }

    onChangeChargingStationPhaseMatching(newPhaseMatching: phaseMatchingType): void {
        // Set charging station to new phase matching
        // Example with 231 matching: 
        // phaseMatchingToGrid: { PHASE_1: PHASE_2, PHASE_2: PHASE_3, PHASE_3: PHASE_1 }
        // --> I'm at the charging station and using its first phase. I'm drawing power from grid's second phase. 
        // phaseMatchingToChargingStation: { PHASE_2: PHASE_1, PHASE_3: PHASE_2, PHASE_1: PHASE_3 }
        // --> I'm at the grid and looking at its first phase. Charging station is drawing on its 3rd phase. 

        const phaseMatching = this.getChargingStationPhaseMatching(); 
        phaseMatching.PHASE_1 = newPhaseMatching.PHASE_1; 
        phaseMatching.PHASE_2 = newPhaseMatching.PHASE_2; 
        phaseMatching.PHASE_3 = newPhaseMatching.PHASE_3; 

        const phaseMatchingToChargingStation = this.getChargingStationPhaseMatchingToChargingStation(); 
        phaseMatchingToChargingStation[newPhaseMatching.PHASE_1] = "PHASE_1"; 
        phaseMatchingToChargingStation[newPhaseMatching.PHASE_2] = "PHASE_2"; 
        phaseMatchingToChargingStation[newPhaseMatching.PHASE_3] = "PHASE_3"; 
        
    }
    isRadioButtonPhaseMatchingChecked(phaseMatching: phaseMatchingType): boolean {
        return phaseMatching.PHASE_1 === this.getChargingStationPhaseMatching().PHASE_1 &&
            phaseMatching.PHASE_2 === this.getChargingStationPhaseMatching().PHASE_2 &&
            phaseMatching.PHASE_3 === this.getChargingStationPhaseMatching().PHASE_3; 
    }


    onClickClose(): void {
        this.dialogRef.close();
    }

}




@Component({
    selector: 'edit-car-dialog',
    templateUrl: 'edit-car-dialog.html',
})
export class EditCarDialog {

    constructor(
        public dialogRef: MatDialogRef<EditCarDialog>,
        @Inject(MAT_DIALOG_DATA) public data: {
            car: Car,
            appParent: AppComponent
        }) { }

    getEVPowerW() {
        return this.data.car.maxCurrent*230; 
    }

    getSoC() {
        return this.data.car.startCapacity / this.data.car.maxCapacity; 
    }

    getMinSoC() {
        return this.data.car.minLoadingState / this.data.car.maxCapacity; 
    }
    
    getSumUsedPhases() {
        return this.data.car.canLoadPhase1+this.data.car.canLoadPhase2+this.data.car.canLoadPhase3; 
    }

    onChangeCanLoadPhase1(event: MatCheckboxChange): void {
        this.data.car.canLoadPhase1 = event.checked ? 1 : 0; 
        this.refreshTotalCurrentBasedOnPhases(); 
    }

    onChangeCanLoadPhase2(event: MatCheckboxChange): void {
        this.data.car.canLoadPhase2 = event.checked ? 1 : 0; 
        this.refreshTotalCurrentBasedOnPhases(); 
    }
    onChangeCanLoadPhase3(event: MatCheckboxChange): void {
        this.data.car.canLoadPhase3 = event.checked ? 1 : 0; 
        this.refreshTotalCurrentBasedOnPhases(); 
    }

    refreshTotalCurrentBasedOnPhases() {
        this.data.car.minCurrent = this.data.car.minCurrentPerPhase * this.getSumUsedPhases(); 
        this.data.car.maxCurrent = this.data.car.maxCurrentPerPhase * this.getSumUsedPhases(); 
    }
    

    onClickClose(): void {
        this.dialogRef.close();
    }

}


