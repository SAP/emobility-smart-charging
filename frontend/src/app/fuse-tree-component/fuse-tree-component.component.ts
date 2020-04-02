import { Component, OnInit, Input, Inject } from '@angular/core';
import { FuseTreeNode, CarAssignmentStore, Car, Fuse, ChargingStation } from 'src/assets/server_types';
import { AppComponent } from '../app.component';
import { Utils } from '../utils/Utils';

import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

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
            data: fuseTreeNode
        });

        dialogRef.afterClosed().subscribe(result => {
            console.log('The dialog was closed');
        });
    }
    onClickEditCar(car: Car): void {
        const dialogRef = this.editCarDialog.open(EditCarDialog, {
            width: "75%",
            data: car
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

    constructor(
        public dialogRef: MatDialogRef<EditFuseTreeComponentDialog>,
        @Inject(MAT_DIALOG_DATA) public data: FuseTreeNode) { }

    onChangeFuse() {
        this.data["fusePhase2"] = this.data["fusePhase1"];
        this.data["fusePhase3"] = this.data["fusePhase1"];
    }

    getPowerW() {
        return Utils.getFusePowerLimitW(this.data);
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
        @Inject(MAT_DIALOG_DATA) public data: Car) { }

    getEVPowerW() {
        return this.data.maxCurrent*230; 
    }

    getSoC() {
        return this.data.startCapacity / this.data.maxCapacity; 
    }

    getMinSoC() {
        return this.data.minLoadingState / this.data.maxCapacity; 
    }
    
    getSumUsedPhases() {
        return this.data.canLoadPhase1+this.data.canLoadPhase2+this.data.canLoadPhase3; 
    }

    refreshPhases() {
        this.data.minCurrent = this.data.minCurrentPerPhase * this.getSumUsedPhases(); 
        this.data.maxCurrent = this.data.maxCurrentPerPhase * this.getSumUsedPhases(); 
    }
    

    onClickClose(): void {
        this.dialogRef.close();
    }

}


