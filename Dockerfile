############
## Server ##
############
FROM maven:alpine as build_server
RUN apk add --update make

# Copy from ChargingOptimizer
WORKDIR /workspace/app
COPY ./frontend ./frontend
COPY ./src ./src
COPY ./pom.xml ./pom.xml
COPY ./Makefile ./Makefile

# Runs mvn clean install 
RUN make emobility_smart_charging-build


##############
## Frontend ##
##############
FROM node:lts-alpine as build_frontend

# Use build results so far
COPY --from=build_server /workspace/app/frontend /workspace/app/frontend
WORKDIR /workspace/app 

# npm install and build frontend (Angular 9)
# Separate steps so that Docker can cache npm install
RUN cd frontend && npm install 
RUN cd frontend && npm run build:prod:playground


##################
## Start server ##
##################
FROM openjdk:8-jre-alpine


COPY --from=build_server /workspace/app/target/*.jar /workspace/app/target/
COPY --from=build_frontend /workspace/app/public /workspace/app/public

WORKDIR /workspace/app

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:///dev/./urandom", "-jar", "target/emobility-smart-charging-0.0.1-SNAPSHOT.jar"]




