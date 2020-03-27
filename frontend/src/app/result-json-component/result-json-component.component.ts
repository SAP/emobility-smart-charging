import { Component, OnInit, Input } from '@angular/core';
import { restApiResponseType } from 'src/global';


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
