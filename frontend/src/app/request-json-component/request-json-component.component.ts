import { Component, OnInit, Input } from '@angular/core';
import { OptimizeChargingProfilesRequest } from 'src/assets/server_types';

@Component({
  selector: 'app-request-json-component',
  templateUrl: './request-json-component.component.html',
  styleUrls: ['./request-json-component.component.css']
})
export class RequestJsonComponentComponent implements OnInit {

    @Input() restApiRequest: OptimizeChargingProfilesRequest; 

    constructor() { }

    ngOnInit() {
        
    }

}
