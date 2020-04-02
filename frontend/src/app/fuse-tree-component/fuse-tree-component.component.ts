import { Component, OnInit, Input, Inject } from '@angular/core';
import { FuseTreeNode, CarAssignmentStore, Car, Fuse, ChargingStation } from 'src/assets/server_types';
import { AppComponent } from '../app.component';
import { Utils } from '../utils/Utils';

import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { phaseMatchingType } from 'src/global';

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
        return this.appParent.getAssignedCarByFuseTreeNode(this.fuseTreeNode); 
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

    onChangeFuse() {
        this.data.fuseTreeNode["fusePhase2"] = this.data.fuseTreeNode["fusePhase1"];
        this.data.fuseTreeNode["fusePhase3"] = this.data.fuseTreeNode["fusePhase1"];
    }

    getPowerW() {
        return Utils.getFusePowerLimitW(this.data.fuseTreeNode);
    }


    onChangeChargingStationPhaseAssignment() {
        console.log(event); 
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

    refreshPhases() {
        this.data.car.minCurrent = this.data.car.minCurrentPerPhase * this.getSumUsedPhases(); 
        this.data.car.maxCurrent = this.data.car.maxCurrentPerPhase * this.getSumUsedPhases(); 
    }
    

    onClickClose(): void {
        this.dialogRef.close();
    }

}


