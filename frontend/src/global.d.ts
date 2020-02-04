import { OptimizeChargingProfilesRequest } from "./assets/server_types";



declare type websiteDataType = {
    request: OptimizeChargingProfilesRequest,
    settings: websiteSettings
}


declare type websiteSettings = {
    requestJSONExpanded: boolean,
    responseExpanded: boolean
}