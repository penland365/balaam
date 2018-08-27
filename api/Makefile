VERSION      := $(shell sbt -no-colors version | awk -F " " 'END {if(NR=4) print $$2}')
DOCKERFILE   := docker/Dockerfile
DOCKER_REPO  := penland365/balaam
DOCKER_IMAGE := $(DOCKER_REPO):v$(VERSION)

build/docker: | build/packageXzTarball
	docker build --no-cache -f $(DOCKERFILE) --build-arg VERSION=$(VERSION) -t $(DOCKER_IMAGE) . && touch build/docker

build:
	mkdir -p build/

build/packageXzTarball: | build
	sbt clean compile universal:packageXzTarball && touch build/packageXzTarball

clean:
	sbt clean && rm -rf build/

dockerfile:
	@echo $(DOCKERFILE)
	@cat $(DOCKERFILE)

dockerimage:
	@echo $(DOCKER_IMAGE)

dockerrepo:
	@echo $(DOCKER_REPO)

install: | build/docker
	docker push $(DOCKER_IMAGE)

tasks:
	@echo "build/docker - builds the Dockerimage"
	@echo "build - creates the build directory"
	@echo "build/packageXzTarball - creates the Java Tarball that is a Fat Jar"

version:
	@echo $(VERSION)

.PHONY: clean dockerimage dockerrepo dockerfile tasks version
