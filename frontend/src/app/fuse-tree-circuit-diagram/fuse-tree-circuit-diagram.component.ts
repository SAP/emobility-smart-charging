import { Component, OnInit, Input } from '@angular/core';
import { StateStore, FuseTreeNode, Car } from 'src/assets/server_types';
import { AppComponent } from '../app.component';
import { Utils } from '../utils/Utils';
import { circuitDiagramSettingsType } from 'src/global';

@Component({
    selector: 'app-fuse-tree-circuit-diagram',
    templateUrl: './fuse-tree-circuit-diagram.component.html',
    styleUrls: ['./fuse-tree-circuit-diagram.component.css']
})
export class FuseTreeCircuitDiagramComponent implements OnInit {

    @Input() state: StateStore
    @Input() appParent: AppComponent;

    currentRowIndex: number = 0; 

    circuitDiagram: circuitDiagramSettingsType = {
        marginLeft: 50,
        marginTop: 50,
        rowHeight: 40+2*30+2*20 + 80, // Row Height = fuseHeight + 2*cable.lengthWithoutPhases + 3*cable.phaseMargin + buffer offset
        rowBuffer: 80, // Buffer between rows
        columnWidth: 170, // Column width = fuseWidth + buffer offset
        
        // margin between cables
        cable: {
            phaseMargin: 20,
            lengthWithoutPhases: 30,
            horizontalX2Offset: 10
        },

        // The fuse rectangle itself
        fuse: {
            width: 150,
            height: 40
        }, 

        // Charge stations
        chargeStation: {
            width: 150, 
            height: 40
        }

    }


    /**
     * Generate SVG based on state:
     * Fuse tree and assigned cars
     */
    constructor() { }

    ngOnInit(): void {
    }


    /**
     * Without any scaling, what is maximum height that we want? 
     * maximumRowIndex * rowHeight + marginTop * 2
     */
    getMaximumSVGHeight(): number {
        const maximumRowIndex = this.getMaximumRowIndex(this.state.fuseTree.rootFuse, 0); 
        const result = (maximumRowIndex+1) * this.circuitDiagram.rowHeight + this.circuitDiagram.marginTop*2;  
        console.log("Init circuit diagram with maximumRowIndex=" + maximumRowIndex + ", each row=" + this.circuitDiagram.rowHeight  + ". result height=" + result); 
        return result; 
    }

    /**
     * Without any scaling, what is the maximum width we want?
     * maximumColumnIndex * columnWidth + marginLeft * 2
     */
    getMaximumSVGWidth(): number {
        const maximumColumnIndex = this.getMaximumColumnIndex(this.state.fuseTree.rootFuse, 0); 
        const result = (maximumColumnIndex+1) * this.circuitDiagram.columnWidth + this.circuitDiagram.marginLeft*2; 
        console.log("Init circuit diagram with maximumColumnIndex=" + maximumColumnIndex + ", each column=" + this.circuitDiagram.columnWidth  + ". result width=" + result); 
        return result; 
    }

    /**
     * 
     * Compute via tree depth, what is the deepest rowIndex we need to go 
     * 
     * From example below: maximum row index is 7
     **/
    getMaximumRowIndex(fuseTreeNode: FuseTreeNode, depth: number): number {
        if (fuseTreeNode["@type"]==="ChargingStation") return depth; 
        if (fuseTreeNode.children.length===0) return depth; 

        let maxRowIndex = 0;
        for (const child of fuseTreeNode.children) {
            // How much will rowIndex increase due to following siblings?
            const sumFollowingSiblingDepth = this.getSumFollowingSiblingDepth(child, fuseTreeNode); 

            // How do children impact this?
            const childMaxRowIndex = this.getMaximumRowIndex(child, depth+1); 

            if (sumFollowingSiblingDepth + childMaxRowIndex > maxRowIndex) {
                maxRowIndex = sumFollowingSiblingDepth + childMaxRowIndex; 
            }
        }
        return maxRowIndex;         
    }   

    /**
     * From example below: maximum col index is 5
     */
    getMaximumColumnIndex(fuseTreeNode: FuseTreeNode, depth: number): number {
        if (fuseTreeNode["@type"]==="ChargingStation") return depth; 
        if (fuseTreeNode.children.length===0) return depth; 

        let maxColumnIndex = 0; 
        fuseTreeNode.children.forEach((child: FuseTreeNode, childIndex: number) => {
            const columnIndex = childIndex + this.getMaximumColumnIndex(child, depth+1); 
            if (columnIndex > maxColumnIndex) {
                maxColumnIndex = columnIndex; 
            }
        }); 
        return maxColumnIndex; 
    }

    /**
     * Of the siblings after this element in the parent's children array: What is the sum of their maximum depth?
     * 
     * Example: 
     * 
     * Fuse (rowIndex=0) <-- Result = 0
     * ---- Fuse (rowIndex=1) <-- Result = 3
     *      ---- Fuse (rowIndex=5) <--- Result = 1
     *           ---- Fuse (rowIndex=7)
     *      ---- Fuse (rowIndex=5) 
     *           ---- Fuse (rowIndex=6)
     * ---- Fuse (rowIndex=1) <-- Result = 2
     *      ---- ChargeStation (RowIndex=4) <-- Result = 0
     *      ---- Fuse (RowIndex=4) <-- Result = 0
     * ---- Fuse (rowIndex=1) <-- Result = 0
     *      ---- Fuse (rowIndex=2) <-- Result = 0
     *           ---- Fuse (rowIndex=3) <--- Result = 0
     */
    getSumFollowingSiblingDepth(fuseTreeNode: FuseTreeNode, fuseTreeNodeParent: FuseTreeNode | null): number {
        if (fuseTreeNodeParent === null) return 0; // Root fuse has no parent

        const followingSiblings: Array<FuseTreeNode> = []; 

        // Find the siblings that follow this fuseTreeNode
        let foundInChildrenArray: boolean = false; 
        for (const sibling of fuseTreeNodeParent.children) {
            if (foundInChildrenArray) {
                followingSiblings.push(sibling); 
            }
            
            if (sibling.id === fuseTreeNode.id) {
                foundInChildrenArray = true; 
            }   
        }

        let sumFollowingSiblingDepth = 0; 
        for (const followingSibling of followingSiblings) {
            const siblingMaximumDepth = this.appParent.getMaximumFuseTreeNodeDepth(followingSibling, 0); 
            sumFollowingSiblingDepth += siblingMaximumDepth; 
        }

        // console.log("getSumFollowingSiblingDepth: Returning " + sumFollowingSiblingDepth + " for " + this.fuseTreeNode["@type"] + ", id=" + this.fuseTreeNode.id);

        return sumFollowingSiblingDepth;  
    }


    /**
     * Get tree depth: What is the deepest we need to go? 
     */
    getScaledSVGHeight(): number {
        const svgCircuitDiagram = document.getElementsByClassName("svgCircuitDiagram");
        const maxHeight = this.getMaximumSVGHeight(); 
        const maxWidth = this.getMaximumSVGWidth(); 
        if (svgCircuitDiagram.length === 0) {
            return maxHeight;
        }

        const width = svgCircuitDiagram[0].clientWidth;
        if (width > maxWidth) {
            // If element is wide enough, return constant height for svg
            return maxHeight;
        }
        else {
            // If element is not wide enough, slowly scale down height for svg
            const widthPercent = width / maxWidth;
            return Math.ceil(widthPercent * maxHeight);
        }
    }

    /**
     * For each generation of children, increment the row index once (so that all children are on the same row)
     */
    incrementCurrentRowIndex(): void {
        this.currentRowIndex++; 
    }
    getCurrentRowIndex(): number {
        return this.currentRowIndex; 
    }

}

@Component({
    selector: '[fuse-circuit-diagram]',
    templateUrl: 'fuse-circuit-diagram.html',
})
export class FuseCircuitDiagram {

    @Input() appParent: AppComponent;
    @Input() fuseTreeCircuitDiagram: FuseTreeCircuitDiagramComponent;
    settings: circuitDiagramSettingsType; 

    // Current fuseTreeNode to be drawn
    @Input() fuseTreeNode: FuseTreeNode;
    @Input() fuseTreeNodeParent: FuseTreeNode | null; // Parent is null for the root fuse

    // Which row is this node being drawn in
    @Input() rowIndex: number; 
    // Which column is this node being drawn in
    @Input() columnIndex: number; 
    
    ngOnInit(): void {
        this.settings = this.fuseTreeCircuitDiagram.circuitDiagram; 
    }

    isRootFuse(): boolean {
        return this.fuseTreeNode["@type"] === "Fuse" && this.fuseTreeNode.id === 0;
    }
    isChargingStation(): boolean {
        return this.fuseTreeNode["@type"] === "ChargingStation"; 
    }
    
    getFuseTreeNodeTitleLabel(): string {
        return this.fuseTreeNode['@type'] + " (id=" + this.fuseTreeNode.id + ")"; 
    }
    getFuseTreeNodeValueLabel(): string {
        return Utils.getFuseTreeNodeLabel(this.fuseTreeNode); 
    }
    getAssignedTitleLabel(): string {
        return "Car (id=" + this.getAssignedCar().id + ")"; 
    }
    getAssignedValueLabel(): string {
        return Utils.getCarLabel(this.getAssignedCar()); 
    }
    

    getNodeWidth(): number {
        return this.isChargingStation() ? this.settings.chargeStation.width : this.settings.fuse.width; 
    }
    getNodeHeight(): number {
        return this.isChargingStation() ? this.settings.chargeStation.height : this.settings.fuse.height; 
    }


    getNodeX(): number {
        return this.settings.marginLeft + this.columnIndex*this.settings.columnWidth;
    }
    getNodeCenterX(): number {
        return this.getNodeX() + this.getNodeWidth() / 2;
    }
    getNodeY(): number {
        return this.settings.marginTop + this.rowIndex*this.settings.rowHeight;
    }
    getNodeCenterY(): number {
        return this.getNodeY() + this.getNodeHeight() / 2; 
    }
    getNodeBottomY(): number {
        return this.getNodeY() + this.getNodeHeight(); 
    }

    getLeftCableX(): number {
        return this.getNodeCenterX() - this.settings.cable.phaseMargin;
    }
    getMiddleCableX(): number {
        return this.getNodeCenterX();
    }
    getRightCableX(): number {
        return this.getNodeCenterX() + this.settings.cable.phaseMargin;
    }

    /**
     * Top of the line going to the node
     */
    getLeftCableToNodeY1(): number {
        if (this.isRootFuse()) {
            return this.getNodeY() - this.settings.cable.lengthWithoutPhases;
        }
        else {
            return this.getNodeY() - this.settings.cable.lengthWithoutPhases - 2*this.settings.cable.phaseMargin; 
        }
    }
    getMiddleCableToNodeY1(): number {
        if (this.isRootFuse()) {
            return this.getNodeY() - this.settings.cable.lengthWithoutPhases;
        }
        else {
            return this.getNodeY() - this.settings.cable.lengthWithoutPhases - 1*this.settings.cable.phaseMargin; 
        }
    }
    getRightCableToNodeY1(): number {
        if (this.isRootFuse()) {
            return this.getNodeY() - this.settings.cable.lengthWithoutPhases;
        }
        else {
            return this.getNodeY() - this.settings.cable.lengthWithoutPhases - 0*this.settings.cable.phaseMargin; 
        }
    }


    /**
     * Bottom of the line going away from the node
     * Must reach children
     */
    getCableFromNodeY2(): number {
        let childRowIndexOffset = this.getSumFollowingSiblingDepth(); 
        return this.getNodeBottomY() + this.settings.cable.lengthWithoutPhases + this.settings.rowBuffer + childRowIndexOffset*this.settings.rowHeight; 
    }
    getLeftCableFromNodeY2(): number {
        if (this.isChargingStation()) {
            return this.getNodeBottomY() + this.settings.cable.lengthWithoutPhases; 
        }
        else {
            return this.getCableFromNodeY2(); 
        }
    }
    getMiddleCableFromNodeY2(): number {
        if (this.isChargingStation()) {
            return this.getNodeBottomY() + this.settings.cable.lengthWithoutPhases; 
        }
        else {
            return  this.getCableFromNodeY2() + 1*this.settings.cable.phaseMargin; 
        }
    }
    getRightCableFromNodeY2(): number {
        if (this.isChargingStation()) {
            return this.getNodeBottomY() + this.settings.cable.lengthWithoutPhases; 
        }
        else {
            return this.getCableFromNodeY2() + 2*this.settings.cable.phaseMargin; 
        }
    }

    getAssignedCar(): Car | null {
       return this.appParent.getAssignedCarByFuseTreeNode(this.fuseTreeNode); 
    }


    getSumFollowingSiblingDepth(): number {
        return this.fuseTreeCircuitDiagram.getSumFollowingSiblingDepth(this.fuseTreeNode, this.fuseTreeNodeParent); 
    }

}



