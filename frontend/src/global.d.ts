import { OptimizeChargingProfilesRequest, Car, OptimizeChargingProfilesResponse } from "./assets/server_types";


declare type websiteDataType = {
    request: OptimizeChargingProfilesRequest,
    settings: websiteSettings
}


declare type websiteSettings = {
    requestJSONExpanded: boolean,
    responseExpanded: boolean
}


declare type restApiResponseErrorType = {
    name: string, 
    message: string
}

declare type restApiResponseType = {
    jsonContent: OptimizeChargingProfilesResponse, 
    error: restApiResponseErrorType
}