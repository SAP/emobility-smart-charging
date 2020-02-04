import { Component, OnInit, Input } from '@angular/core';
import { FuseTreeNode, CarAssignmentStore, Car, Fuse, ChargingStation } from 'src/assets/server_types';
import { AppComponent } from '../app.component';

@Component({
    selector: 'app-fuse-tree-component',
    templateUrl: './fuse-tree-component.component.html',
    styleUrls: ['./fuse-tree-component.component.css']
})
export class FuseTreeComponentComponent implements OnInit {

    @Input() fuseTreeNode: FuseTreeNode; 
    @Input() depth: number;
    @Input() appParent: AppComponent; 


    constructor() { 

    }

    getAssignedCar() {
        if (this.fuseTreeNode["@type"] === "ChargingStation") {
            return this.appParent.getAssignedCar(this.fuseTreeNode.id); 
        }
        else {
            return null; 
        }
        
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

    ngOnInit() {
        
    }

}
