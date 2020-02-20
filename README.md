# emobility-smart-charging
[![Build Status](https://travis-ci.cloud.sap.corp/D059373/emobility-smart-charging.svg?token=RXEqMfJB84WgppxwEaKh&branch=master)](https://travis-ci.cloud.sap.corp/D059373/emobility-smart-charging)


## Contents:
1. [Description](#description)
1. [Requirements](#requirements)
1. [Download and Installation](#download-and-installation)
1. [Known Issues](#known-issues)
1. [How to obtain support](#how-to-obtain-support)
1. [To-Do (upcoming changes)](#to-do-upcoming-changes)
1. [License](#license)


### Description
This repository is an implementation of smart charging for electric vehicles (EVs). It contains a charging optimizer which schedules EVs for charging throughout the day. 
The optimization algorithm addresses the following goals: 
- Main goal: Ensure all EVs are charged at the end of the day while respecting infrastructure constraints
- Secondary goal: Minimize peak load (avoid peaks in power consumption)
- Secondary goal: Minimize electricity prices (charge EVs at times when electricity is cheap if prices vary)

Refer to [1] for a detailed explanation of the algorithm. 

On a technical note, this repository contains the following components: 
- The algorithm for charging optimization (implemented in Java)
- A server with a REST API for accessing the algorithm (implemented with Spring Boot)
- A frontend "playground" application to test REST API input parameters and check results (implemented with Angular 8)
- A Dockerfile to containerize the components described above


[1] O. Frendo, N. Gaertner, and H. Stuckenschmidt, "Real-Time Smart Charging Based on Precomputed Schedules", IEEE Transactions on Smart Grid, vol. 10, no. 6, pp. 6921 – 6932, 2019.


### Requirements
The application may be run either with or without Docker. 

#### With Docker 
The application can be containerized using [Docker](https://docs.docker.com/install/) and the `Dockerfile` in this repository. If the application is run via Docker the other requirements may be ignored. 

#### Without Docker
The server requires Java and the dependency management tool [Maven](https://maven.apache.org/). 
The minimum required Java version is Java 8. 

Enter `java -version` and `mvn -version` in your command line to test your installation. 

The frontend is optional. The server and its REST API will work without the frontend. 
The requirements for the frontend are [Node.js](https://nodejs.org/en/) and its package manager NPM. 

Enter `node --version` and `npm --version` in your command line to test your installation. 



### Download and Installation
#### With Docker
The simplest way to run this application is to use Docker and the `Dockerfile` in this repository. 
This will compile the server and the frontend. 

First, build the Docker image (this may take a few minutes). 
[Parameters](https://docs.docker.com/engine/reference/commandline/build/): 
- `-t` Tag the image with a name
``` 
docker build -t emobility_smart_charging .
```

Next, start the application by running the container (the server runs on port 8080). 
[Parameters](https://docs.docker.com/engine/reference/run/): 
- `-d` Detached mode: Run container in the background
- `-p` Publish a container's port to the host: Change the first port in `8080:8080` to adjust which port you want the application to run on
```
docker run -d -p 8080:8080 emobility_smart_charging
```

#### Without Docker
This section is relevant if the application should be run without Docker, for example for development purposes. 

First, compile the server: 
```
mvn clean install
```

(Optional) Prepare and compile the frontend: 
```
cd frontend/
npm install
npm run build:playground
```

Start the server (from the root directory of the repository): 
```
java -jar target/emobility-smart-charging-0.0.1-SNAPSHOT.jar
```

#### Accessing the application
After you have started the application it can be accessed via [localhost:8080](http://localhost:8080/). 
The frontend can be accessed via [/playground/index.html](http://localhost:8080/playground/index.html). 
The API documentation is implemented via Swagger and can be accessed via [/swagger-ui.html](http://localhost:8080/swagger-ui.html). 


#### Generate TypeScript mappings (optional)
If you plan to use the API in a project which uses TypeScript you can generate the expected types of API requests and responses using the following command: 
``` 
mvn typescript-generator:generate
```
This approach is used in the frontend. Type declarations are generated in the file `frontend/src/assets/server_types.d.ts`. 


### Known Issues
Please refer to the list of [issues](../../issues) on GitHub.


### How to obtain support
Please use the [GitHub issue tracker](../../issues) for any questions, bug reports, feature requests, etc.



### To-Do (upcoming changes) 
- Provide translation of charging profiles to the Open Charge Point Protocol (OCPP) 1.6 or 2.0 
- Provide standard car model parameters

### License

Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
This file is licensed under the Apache Software License, v.2 except as noted otherwise in the [LICENSE file](LICENSE).

Please note that Docker images can contain other software which may be licensed under different licenses. This License file is also included in the Docker image. For any usage of built Docker images please make sure to check the licenses of the artifacts contained in the images.




