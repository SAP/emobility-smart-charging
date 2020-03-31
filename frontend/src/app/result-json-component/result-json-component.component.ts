import { Component, OnInit, Input } from '@angular/core';
import { restApiResponseType } from 'src/global';
import { ChartDataSets, ChartOptions } from 'chart.js';
import { Color, Label } from 'ng2-charts';

@Component({
  selector: 'app-result-json-component',
  templateUrl: './result-json-component.component.html',
  styleUrls: ['./result-json-component.component.css']
})
export class ResultJsonComponentComponent implements OnInit {

    @Input() restApiResponse: restApiResponseType; 

    constructor() { }


    ngOnInit() {
    }

}
