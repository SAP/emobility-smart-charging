import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatButtonModule } from '@angular/material/button';
import { FuseTreeComponentComponent } from './fuse-tree-component/fuse-tree-component.component';
import { MatIconModule } from "@angular/material"; 
import { MatSnackBarModule } from "@angular/material";
import { HttpClientModule } from  '@angular/common/http';
import { MatInputModule } from "@angular/material/input";
import { FormsModule } from '@angular/forms'

@NgModule({
    declarations: [
        AppComponent,
        FuseTreeComponentComponent
    ],
    imports: [
        BrowserModule,
        AppRoutingModule,
        BrowserAnimationsModule,
        MatExpansionModule,
        MatButtonModule,
        MatIconModule,
        MatSnackBarModule,
        HttpClientModule,
        MatInputModule,
        FormsModule
    ],
    providers: [],
    bootstrap: [AppComponent]
})
export class AppModule { }
