PROJECT_NAME?=evse
NAME:=emobility_smart_charging
DOCKER_USER?=
DOCKER_PASSWORD?=
DOCKER_PORT?=8080
DOCKER_ECR_REGION?=eu-west-3
DOCKER_ECR_REGISTRY?=

.PHONY: $(NAME)-docker-start

default: $(NAME)-docker-start

$(NAME)-build:
	mvn clean install

$(NAME)-build-frontend:
	mvn typescript-generator:generate
	cd frontend && npm install && npm run build:playground

$(NAME)-start: $(NAME)-build
	java -jar target/$(NAME)-0.0.1-SNAPSHOT.jar

$(NAME)-docker-build:
	docker build -t $(PROJECT_NAME)_$(NAME) .

$(NAME)-docker-start: $(NAME)-docker-build
	docker run -d -p $(DOCKER_PORT):8080 $(PROJECT_NAME)_$(NAME)

clean-$(NAME)-image:
	-docker rmi $(PROJECT_NAME)_$(NAME)

clean-maven-image:
	-docker rmi maven:alpine

clean-node-image:
	-docker rmi node:lts-alpine

clean-tomcat-image:
	-docker rmi tomcat:latest

clean-images clean-$(NAME)-images: clean-maven-image clean-node-image clean-tomcat-image clean-$(NAME)-image

clean-$(NAME)-containers:
	-docker ps -a | awk '{ print $$1,$$2 }' | grep $(PROJECT_NAME)_$(NAME) | awk '{print $$1 }' | xargs -I {} docker rm --force {}

clean clean-$(NAME): clean-$(NAME)-containers clean-$(NAME)-images

$(NAME)-docker-tag:
	docker tag $(PROJECT_NAME)_$(NAME) $(DOCKER_USER)/$(PROJECT_NAME)_$(NAME)

$(NAME)-docker-push: $(NAME)-docker-build $(NAME)-docker-tag
	docker push $(DOCKER_USER)/$(PROJECT_NAME)_$(NAME)

$(NAME)-docker-tag-ecr:
	docker tag $(PROJECT_NAME)_$(NAME):latest $(DOCKER_ECR_REGISTRY)/$(NAME):latest

$(NAME)-docker-push-ecr: $(NAME)-docker-build $(NAME)-docker-tag-ecr
	aws ecr get-login-password --region $(DOCKER_ECR_REGION) | docker login --username AWS --password-stdin $(DOCKER_ECR_REGISTRY)/$(NAME)
	docker push $(DOCKER_ECR_REGISTRY)/$(NAME):latest

ifeq ($(OS),Windows_NT)
# FIXME
# $(NAME)-cf-push: set CF_DOCKER_PASSWORD=$(DOCKER_PASSWORD)
else
$(NAME)-cf-push: export CF_DOCKER_PASSWORD=$(DOCKER_PASSWORD)
endif
$(NAME)-cf-push: $(NAME)-docker-push
	cf push --docker-image $(DOCKER_USER)/$(PROJECT_NAME)_$(NAME) --docker-username $(DOCKER_USER)

ifeq ($(OS),Windows_NT)
# FIXME
# $(NAME)-cf-push-only: set CF_DOCKER_PASSWORD=$(DOCKER_PASSWORD)
else
$(NAME)-cf-push-only: export CF_DOCKER_PASSWORD=$(DOCKER_PASSWORD)
endif
$(NAME)-cf-push-only:
	cf push --docker-image $(DOCKER_USER)/$(PROJECT_NAME)_$(NAME) --docker-username $(DOCKER_USER)

dist-clean-images:
	docker image prune -a -f

dist-clean-volumes:
	docker volume prune -f

dist-clean: clean-$(NAME)-containers dist-clean-volumes dist-clean-images
