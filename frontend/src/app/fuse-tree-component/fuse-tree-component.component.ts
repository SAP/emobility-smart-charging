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

    getFuseTreeNodeLabel(fuseTreeNode: FuseTreeNode): string {
        let label: string; 
        if (fuseTreeNode["fusePhase1"] === fuseTreeNode["fusePhase2"] &&
            fuseTreeNode["fusePhase2"] === fuseTreeNode["fusePhase3"]) {

            label = fuseTreeNode["fusePhase1"] + "A~3"; 
        }
        else {
            label = fuseTreeNode["fusePhase1"] + "A/" + fuseTreeNode["fusePhase2"] + "A/" + fuseTreeNode["fusePhase3"] + "A"; 
        }
        const powerKW = this.appParent.getFusePowerLimitW(fuseTreeNode) / 1000; 
        label += " (" + this.appParent.roundToNDecimals(powerKW, 3) + "kW)"; 

        return label; 
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
