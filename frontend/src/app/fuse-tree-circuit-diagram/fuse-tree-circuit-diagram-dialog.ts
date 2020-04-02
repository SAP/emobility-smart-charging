import { Component, Inject, Input } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { Car, StateStore } from 'src/assets/server_types';
import { AppComponent } from '../app.component';
import { Utils } from '../utils/Utils';

@Component({
    selector: 'fuse-tree-circuit-diagram-dialog',
    templateUrl: 'fuse-tree-circuit-diagram-dialog.html',
})
export class FuseTreeCircuitDiagramDialog {

    constructor(
        public dialogRef: MatDialogRef<FuseTreeCircuitDiagramDialog>,
        @Inject(MAT_DIALOG_DATA) public data: {
            state: StateStore,
            appParent: AppComponent
        }) { }


    onClickClose(): void {
        this.dialogRef.close();
    }

    onClickSave(): void {
        const svgCircuitDiagram: Element = document.getElementsByClassName("svgCircuitDiagram")[0];
        Utils.createFileDownload("fuseTreeCircuitDiagram.svg", svgCircuitDiagram.outerHTML); 
    }

}