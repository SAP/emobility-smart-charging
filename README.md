# emobility-smart-charging

## Contents:
1. [Description](#description)
1. [Requirements](#requirements)
1. [Download and Installation](#download-and-installation)
1. [Getting Started](#getting-started)
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
After you have started the application it runs on `localhost:8080`. 
The frontend can be accessed via [/playground/index.html](http://localhost:8080/playground/index.html). 
The API documentation is implemented via Swagger and can be accessed via [/swagger-ui.html](http://localhost:8080/swagger-ui.html). 


#### Generate TypeScript mappings (optional)
If you plan to use the API in a project which uses TypeScript you can generate the expected types of API requests and responses using the following command: 
``` 
mvn typescript-generator:generate
```
This approach is used in the frontend. Type declarations are generated in the file `frontend/src/assets/server_types.d.ts`. 

### Getting Started
The purpose of this section is to get you started on using the charging optimizer API. 

The easiest way to understand the interface of the API is to tinker with the playground ([/playground/index.html](http://localhost:8080/playground/index.html)). The playground is a visual interface which lets you edit the input for the charging optimizer in a natural way. The playground translates your model into a JSON request which is the technical input to the charging optimizer. You can easily pick up how to assemble JSON requests for the optimizer by observing how your playground input is reflected in the generated request.

#### Understanding charging optimizer input
In the top part of the playground screen you can edit the following input parameters:
* **Current time**: This is the actual time of day assumed by the optimizer. The optimizer can only schedule charging sessions after the current time, not before. By default, the playground uses midday as current time. 

* **Charging infrastructure**: The charging infrastructure consists of a hierarchy of fuses reflecting the technical installation of the charging hardware. In real life, fuses are installed in a tree structure. There is typically one fuse per charging station, another fuse for a set of charging stations, and then further fuses for sets of fuses. By default, the playground contains a charging infrastructure with two levels of fuses to illustrate the concept of the tree structure.

* **Fuse**: Each fuse is characterized by the current at which the fuse cuts off the power supply. The charging optimizer assumes three-phase electrical circuits. Therefore, each fuse is defined by a triplet of current values, one per phase. The playground lets you add further fuses to the infrastructure by clicking the corresponding buttons. By default, the playground uses 32 Ampere per phase for new fuses.
* **Charging station**: Each charging station is characterized by the current at which the built-in fuse cuts off the power supply. The playground lets you add further charging stations by clicking the corresponding buttons. By default, the playground uses charging stations with 32 Ampere fuses.

* **Car**: In the playground, cars can be added to charging stations to express their arrival at the charging station. When you add cars via the corresponding button, semantically you create a charging demand. In the charging optimizer, the cars with their charging demands are the central items for the optimization process. The charging optimizer creates one charge plan per car. Therfore you need to have at least one car in your input for the charge optimizer to create a non-trivial output. The more cars you add to the input, the higher becomes the competition for the scarce resource of charging current. With more cars, the available charging capacity is divided and more cars are assigned only partial or no charging opportunities.
When you check out the generated JSON request you will notice the long list of parameters per car.

#### Understanding charging optimizer output
To trigger the charging optimizer us the button labelled **Optimize charge plans**. The resultung JSON response contains a list of charge plans, one per car. Note that the actual charge plan for the car is labelled **currentPlan** and consists of a list of 96 entries. Each entry corresponds to a 15 minute interval since midnight. The entered value specifies the charging current which the optimizer assigns to this car in the given interval.

### Known Issues
Please refer to the list of [issues](../../issues) on GitHub.


### How to obtain support
Please use the [GitHub issue tracker](../../issues) for any questions, bug reports, feature requests, etc.



### To-Do (upcoming changes) 
- Provide translation of charging profiles to the Open Charge Point Protocol (OCPP) 1.6 or 2.0 
- Provide parameters for standard EV models 
- Document API EV parameters in Swagger

### License

Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
This file is licensed under the Apache Software License, v.2 except as noted otherwise in the [LICENSE file](LICENSE).

Please note that Docker images can contain other software which may be licensed under different licenses. This License file is also included in the Docker image. For any usage of built Docker images please make sure to check the licenses of the artifacts contained in the images.




