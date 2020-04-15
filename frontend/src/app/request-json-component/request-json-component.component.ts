import { Component, OnInit, Input } from '@angular/core';
import { OptimizeChargingProfilesRequest } from 'src/assets/server_types';
import { Utils } from '../utils/Utils';

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

    onClickDownloadApiRequest(): void {
        const content = JSON.stringify(this.restApiRequest, null, 4); 
        Utils.createFileDownload("apiRequest.json", content); 
    }

}
