import { Component, OnInit, Input } from '@angular/core';
import { StateStore, FuseTreeNode, Car, ChargingStation, Phase } from 'src/assets/server_types';
import { AppComponent } from '../app.component';
import { Utils } from '../utils/Utils';
import { circuitDiagramSettingsType, phaseMatchingType } from 'src/global';
import * as panzoom from "panzoom"; 

@Component({
    selector: 'app-fuse-tree-circuit-diagram',
    templateUrl: './fuse-tree-circuit-diagram.component.html',
    styleUrls: ['./fuse-tree-circuit-diagram.component.css']
})
export class FuseTreeCircuitDiagramComponent implements OnInit {

    @Input() fuseTreeNode: FuseTreeNode; 
    @Input() appParent: AppComponent;
    @Input() phaseMatching: phaseMatchingType; // Can be null if visualizing complete circuit diagram. Used for single charging station edit dialog

    circuitDiagram: circuitDiagramSettingsType = {
        marginLeft: 50,
        marginTop: -1, // marginTop must be at least lengthWithoutPhases + 2*phaseMargin + radius of connector points
        marginBottom: -1, // marginBottom must be at least 2*chargeStation.height + lengthWithoutPhases + 

        rowHeight: -1, // Row Height = fuseHeight + 2*cable.lengthWithoutPhases + 3*cable.phaseMargin + buffer offset
        rowBuffer: 80, // Buffer between rows
        columnWidth: 190, // Column width = fuseWidth + buffer offset
        
        // margin between cables
        cable: {
            phaseMargin: 20,
            lengthWithoutPhases: 30,
            horizontalX2Offset: 10
        },
        connectorPoint: {
            radius: 2
        },

        // The fuse rectangle itself
        fuse: {
            width: 170,
            height: 40
        }, 

        // Charge stations
        chargeStation: {
            width: 170, 
            height: 40
        },

        // Legend (top right)
        legend: {
            width: 100,
            padding: 5,
            marginTop: 5,
            marginRight: 5
        }
    }


    /**
     * Generate SVG based on state:
     * Fuse tree and assigned cars
     */
    constructor() { 
        const c = this.circuitDiagram; 
        this.circuitDiagram.marginTop = c.cable.lengthWithoutPhases + 2*c.cable.phaseMargin + c.connectorPoint.radius; 
        //this.circuitDiagram.marginBottom = 2*c.chargeStation.height + c.cable.lengthWithoutPhases; 
        this.circuitDiagram.marginBottom = 0; 

        this.circuitDiagram.rowHeight = c.fuse.height + 2*c.cable.lengthWithoutPhases + 2*c.cable.phaseMargin + c.rowBuffer; 
        console.log("Using circuit diagram settings: "); 
        console.log(this.circuitDiagram); 
    }

    ngOnInit(): void {
        if (this.isExplicitPhaseMatching() === false) {
            this.setSVGPanAndZoom(); 
        }
    }


    /**
     * Without any scaling, what is maximum height that we want? 
     * maximumRowIndex * rowHeight + marginTop + marginBottom
     */
    getMaximumSVGHeight(): number {
        if (this.fuseTreeNode["@type"] === "ChargingStation") {
            // If we are showing a single charging station, no need to show a whole row
            const result = this.circuitDiagram.marginTop + // Includes cable leading to station
                this.circuitDiagram.chargeStation.height*2 + // rect for charge station and car
                this.circuitDiagram.cable.lengthWithoutPhases; // between station and car 
            //console.log("Init circuit diagram for a charging station with result height=" + result);
            return result;  
        }   

        const maximumRowIndex = this.getMaximumRowIndex(this.fuseTreeNode, 0); 
        const result = (maximumRowIndex+1) * this.circuitDiagram.rowHeight + this.circuitDiagram.marginTop + this.circuitDiagram.marginBottom;  
        //console.log("Init circuit diagram with maximumRowIndex=" + maximumRowIndex + ", each row=" + this.circuitDiagram.rowHeight  + ". result height=" + result); 
        return result; 
    }

    /**
     * Without any scaling, what is the maximum width we want?
     * maximumColumnIndex * columnWidth + marginLeft * 2
     */
    getMaximumSVGWidth(): number {
        const maximumColumnIndex = this.getMaximumColumnIndex(this.fuseTreeNode, 0); 
        const result = (maximumColumnIndex+1) * this.circuitDiagram.columnWidth + this.circuitDiagram.marginLeft*2; 
        //console.log("Init circuit diagram with maximumColumnIndex=" + maximumColumnIndex + ", each column=" + this.circuitDiagram.columnWidth  + ". result width=" + result); 
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
            if (foundInChildrenArray === true) {
                followingSiblings.push(sibling); 
            }
            
            if (sibling.id === fuseTreeNode.id && sibling["@type"] === fuseTreeNode["@type"]) {
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


    getSVGElement(): Element {
        return document.getElementsByClassName("svgCircuitDiagram")[0];
    }

    getScaledSVG(): { width: number, height: number } {
        const svgCircuitDiagram = this.getSVGElement();
        const maxSVGWidth = this.getMaximumSVGWidth(); 
        const maxSVGHeight = this.getMaximumSVGHeight(); 
        
        const svgAspectRatio = maxSVGWidth / maxSVGHeight; 
        // ratio > 1 ==> svg is wider than tall

        const parentElement = svgCircuitDiagram.parentElement.parentElement; // parentElement is <app-fuse-tree-circuit-diagram>, parent of that is <div>
        const computedStyle = getComputedStyle(parentElement);

        const maxElementWidth = parentElement.clientWidth - parseFloat(computedStyle.paddingLeft) - parseFloat(computedStyle.paddingRight);  
        const maxElementHeight =  parentElement.clientHeight;  // 0.75*window.innerHeight; // If viewing complete circuit diagram: use 0.75 
        
        console.log("Using maxElementHeight=" + maxElementHeight); 

        const elementAspectRatio = maxElementWidth / maxElementHeight; 
        // ratio > 1 ==> element can be wider than it is tall 

        if (svgAspectRatio > elementAspectRatio) {
            // SVG has higher width:height ratio than element ==> restrict height
            //console.log("here with ratio=" + svgAspectRatio + ", elementRatio=" + elementAspectRatio + ". Restricting height to: " + (maxElementWidth/svgAspectRatio)); 
            return {
                width: maxElementWidth, 
                height: Math.ceil(maxElementWidth / svgAspectRatio)
            }; 
        }
        else {
            // SVG has lower ratio than element ==> restrict width
            //console.log("here with ratio=" + svgAspectRatio + ", elementRatio=" + elementAspectRatio + ". Restricting width"); 
            return {
                width: Math.ceil(maxElementHeight * svgAspectRatio),
                height: maxElementHeight
            }; 
        }
    }

    getScaledSVGWidth(): number {
        return this.getScaledSVG().width; 
    }

    /**
     * Get tree depth: What is the deepest we need to go? 
     */
    getScaledSVGHeight(): number {
        return this.getScaledSVG().height; 
    }

    getLegendX(): number {
        return this.getMaximumSVGWidth() - this.circuitDiagram.legend.width - this.circuitDiagram.legend.marginRight; 
    }

    isExplicitPhaseMatching(): boolean {
        return this.phaseMatching !== null && this.phaseMatching !== undefined; 
    }

    setSVGPanAndZoom(): void {
        // panzoom wants first <g> element, not the root element
        const groupInSVG = document.querySelector(".svgCircuitDiagram > .groupFuseCircuitDiagram"); 
        console.log("Activating pan and zoom on element:");
        console.log(groupInSVG);  
        panzoom.default(groupInSVG as SVGElement);  
    }



}


/**
 * Single component of the fuse circuit diagram
 * Fuse (cables leading to and from)
 * Charging station (cables leading to) + car
 */
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

    @Input() phaseMatching: phaseMatchingType; // Can be null if visualizing complete circuit diagram. Used for single charging station edit dialog

    // Which row is this node being drawn in
    @Input() rowIndex: number; 
    // Which column is this node being drawn in
    @Input() columnIndex: number; 
    
    ngOnInit(): void {
        this.settings = this.fuseTreeCircuitDiagram.circuitDiagram; 
        //console.log("using explicit phaseMatching="); 
        //console.log(this.phaseMatching); 
    }

    isRootFuse(): boolean {
        return this.fuseTreeNode["@type"] === "Fuse" && this.fuseTreeNode.id === 0;
    }
    isChargingStation(): boolean {
        return this.fuseTreeNode["@type"] === "ChargingStation"; 
    }
    isExplicitPhaseMatching(): boolean {
        return this.fuseTreeCircuitDiagram.isExplicitPhaseMatching(); 
    }
    getPhaseMatching(): phaseMatchingType {
        if (this.isExplicitPhaseMatching() === true) {
            return this.phaseMatching; 
        }
        else {
            return (this.fuseTreeNode as ChargingStation).phaseToGrid as phaseMatchingType; 
        }
    }
    getCableLengthByPhase(phase: Phase): number {
        switch (phase) {
            case "PHASE_1": return 2*this.settings.cable.phaseMargin; 
            case "PHASE_2": return 1*this.settings.cable.phaseMargin; 
            case "PHASE_3": return 0*this.settings.cable.phaseMargin; 
        }
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

    getLeftCablePhase(): Phase {
        if (this.isChargingStation() === false) {
            return "PHASE_1"; 
        }
        else {
            return this.getPhaseMatching().PHASE_1; 
        }
    }
    getMiddleCablePhase(): Phase {
        if (this.isChargingStation() === false) {
            return "PHASE_2"; 
        }
        else {
            return this.getPhaseMatching().PHASE_2; 
        }
    }
    getRightCablePhase(): Phase {
        if (this.isChargingStation() === false) {
            return "PHASE_3"; 
        }
        else {
            return this.getPhaseMatching().PHASE_3; 
        }
    }

    /**
     * Top of the line going to the node
     * Left cable (phase 1 of charging station)
     */
    getLeftCableToNodeY1(): number {
        if (this.isRootFuse()) {
            return this.getNodeY() - this.settings.cable.lengthWithoutPhases;
        }
        else if (this.isChargingStation()) {
            return this.getNodeY() - this.settings.cable.lengthWithoutPhases - this.getCableLengthByPhase(this.getPhaseMatching().PHASE_1)
        }
        else {
            return this.getNodeY() - this.settings.cable.lengthWithoutPhases - 2*this.settings.cable.phaseMargin; 
        }
    }
    /**
     * Middle cable (phase 2 of charging station)
     */
    getMiddleCableToNodeY1(): number {
        if (this.isRootFuse()) {
            return this.getNodeY() - this.settings.cable.lengthWithoutPhases;
        }
        else if (this.isChargingStation()) {
            return this.getNodeY() - this.settings.cable.lengthWithoutPhases - this.getCableLengthByPhase(this.getPhaseMatching().PHASE_2)
        }
        else {
            return this.getNodeY() - this.settings.cable.lengthWithoutPhases - 1*this.settings.cable.phaseMargin; 
        }
    }
    getRightCableToNodeY1(): number {
        if (this.isRootFuse()) {
            return this.getNodeY() - this.settings.cable.lengthWithoutPhases;
        }
        else if (this.isChargingStation()) {
            return this.getNodeY() - this.settings.cable.lengthWithoutPhases - this.getCableLengthByPhase(this.getPhaseMatching().PHASE_3)
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



