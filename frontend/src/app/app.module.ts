import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';  

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from "@angular/material/checkbox"; 
import { FuseTreeComponentComponent, EditFuseTreeComponentDialog, EditCarDialog } from './fuse-tree-component/fuse-tree-component.component';
import { MatIconModule } from "@angular/material"; 
import { MatSnackBarModule } from "@angular/material";
import { HttpClientModule } from  '@angular/common/http';
import { MatInputModule } from "@angular/material/input";
import { FormsModule } from '@angular/forms';
import { ResultJsonComponentComponent } from './result-json-component/result-json-component.component'
import { MatDialogModule } from "@angular/material/dialog"; 
import {MatSelectModule} from '@angular/material/select'

import { NgxJsonViewModule } from 'ng-json-view';
import { RequestJsonComponentComponent } from './request-json-component/request-json-component.component';
import { ChartsModule } from 'ng2-charts';
import { FuseTreeCircuitDiagramComponent, FuseCircuitDiagram } from './fuse-tree-circuit-diagram/fuse-tree-circuit-diagram.component';

@NgModule({
    declarations: [
        AppComponent,
        FuseTreeComponentComponent,
        ResultJsonComponentComponent,
        RequestJsonComponentComponent,
        EditFuseTreeComponentDialog,
        EditCarDialog,
        FuseTreeCircuitDiagramComponent,
        FuseCircuitDiagram
    ],
    imports: [
        CommonModule, 
        BrowserModule,
        AppRoutingModule,
        BrowserAnimationsModule,
        MatDialogModule, 
        ChartsModule, 
        MatExpansionModule,
        MatSelectModule,
        MatButtonModule,
        MatCheckboxModule, 
        MatIconModule,
        MatSnackBarModule,
        HttpClientModule,
        MatInputModule,
        FormsModule,
        NgxJsonViewModule
    ],
    providers: [],
    bootstrap: [AppComponent]
})
export class AppModule { }
