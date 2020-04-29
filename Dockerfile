############
## Server ##
############
FROM maven:alpine as build_server
RUN apk add --update make

# Copy from ChargingOptimizer
WORKDIR /workspace/app
COPY ./ ./


# Runs mvn clean install 
RUN make emobility_smart_charging-build


##############
## Frontend ##
##############
FROM node:lts-alpine as build_frontend
RUN apk add --update make

# Use build results so far
COPY --from=build_server /workspace/app /workspace/app
WORKDIR /workspace/app 

# npm install and build frontend (Angular 9)
RUN make emobility_smart_charging-build-frontend



##################
## Start server ##
##################
FROM tomcat:latest

COPY --from=build_frontend /workspace/app /workspace/app

WORKDIR /workspace/app

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:///dev/./urandom", "-jar", "target/emobility-smart-charging-0.0.1-SNAPSHOT.jar"]
