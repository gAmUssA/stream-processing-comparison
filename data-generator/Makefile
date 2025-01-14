# Colors and formatting
YELLOW := $(shell tput setaf 3)
GREEN := $(shell tput setaf 2)
RED := $(shell tput setaf 1)
BLUE := $(shell tput setaf 4)
BOLD := $(shell tput bold)
RESET := $(shell tput sgr0)

# Docker image configuration
IMAGE_NAME := gamussa/flight-data-generator
IMAGE_TAG := latest
DOCKER_REGISTRY ?= # Set your registry if needed, leave empty for local only

# Default target
.PHONY: all
all: help

.PHONY: help
help: ## Show this help message
	@echo '${YELLOW}📋 Data Generator Commands:${RESET}'
	@echo '  make ${GREEN}<target>${RESET}'
	@echo ''
	@echo '${YELLOW}Targets:${RESET}'
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  ${GREEN}%-15s${RESET} %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# Build the Docker image
.PHONY: build-image
build-image: ## Build Docker image locally
	@echo "${BLUE}🔨 Building Java application...${RESET}"
	@cd .. && ./gradlew :data-generator:clean :data-generator:generateAvroJava :data-generator:jar
	@echo "${BLUE}🏗️  Building Docker image...${RESET}"
	@docker build -t $(IMAGE_NAME):$(IMAGE_TAG) .
	@echo "${GREEN}✅ Image built successfully!${RESET}"

# Generate Java classes from Avro schemas
.PHONY: generate-avro
generate-avro: ## Generate Java classes from Avro schemas
	@echo "${BLUE}📝 Generating Avro classes...${RESET}"
	@cd .. && ./gradlew :data-generator:generateAvroJava
	@echo "${GREEN}✅ Avro classes generated!${RESET}"

# Run tests
.PHONY: test
test: ## Run tests
	@echo "${BLUE}🧪 Running tests...${RESET}"
	@cd .. && ./gradlew :data-generator:test
	@echo "${GREEN}✅ Tests completed!${RESET}"

# Tag the image with registry if provided
.PHONY: tag-image
tag-image: ## Tag image with registry prefix
	@if [ ! -z "$(DOCKER_REGISTRY)" ]; then \
		echo "${BLUE}🏷️  Tagging image for registry...${RESET}"; \
		docker tag $(IMAGE_NAME):$(IMAGE_TAG) $(DOCKER_REGISTRY)/$(IMAGE_NAME):$(IMAGE_TAG); \
		echo "${GREEN}✅ Image tagged!${RESET}"; \
	fi

# Push the image to registry if provided
.PHONY: push-image
push-image: tag-image ## Push image to registry
	@if [ ! -z "$(DOCKER_REGISTRY)" ]; then \
		echo "${BLUE}⬆️  Pushing image to registry...${RESET}"; \
		docker push $(DOCKER_REGISTRY)/$(IMAGE_NAME):$(IMAGE_TAG); \
		echo "${GREEN}✅ Image pushed!${RESET}"; \
	else \
		echo "${RED}❌ No registry specified${RESET}"; \
	fi

# Run the container locally
.PHONY: run
run: ## Run container locally
	@echo "${BLUE}🚀 Starting container...${RESET}"
	@docker run --rm $(IMAGE_NAME):$(IMAGE_TAG)
	@echo "${GREEN}✅ Container finished!${RESET}"

# Clean up local images
.PHONY: clean
clean: ## Remove local images
	@echo "${BLUE}🧹 Cleaning up images...${RESET}"
	@docker rmi $(IMAGE_NAME):$(IMAGE_TAG) 2>/dev/null || true
	@if [ ! -z "$(DOCKER_REGISTRY)" ]; then \
		docker rmi $(DOCKER_REGISTRY)/$(IMAGE_NAME):$(IMAGE_TAG) 2>/dev/null || true; \
	fi
	@echo "${GREEN}✅ Clean up complete!${RESET}"
