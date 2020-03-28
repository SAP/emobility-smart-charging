############
## Server ##
############
FROM maven:alpine as build_server

# Copy from ChargingOptimizer
WORKDIR /workspace/app
COPY ./ ./

RUN mvn clean install -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn


##############
## Frontend ##
##############
FROM node:12.2.0 as build_frontend

#Generate TypeScript interfaces of Java request classes (mvn clean install already does this)
#mvn typescript-generator:generate

# Use build results so far
COPY --from=build_server /workspace/app /workspace/app
WORKDIR /workspace/app/frontend 

RUN npm install

# Build frontend (Angular 8)
RUN npm run build:playground


##################
## Start server ##
##################
FROM tomcat:latest

COPY --from=build_frontend /workspace/app /workspace/app

WORKDIR /workspace/app

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:///dev/./urandom", "-jar", "target/emobility-smart-charging-0.0.1-SNAPSHOT.jar"]


