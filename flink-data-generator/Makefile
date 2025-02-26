# Colors and formatting
YELLOW := $(shell tput setaf 3)
GREEN := $(shell tput setaf 2)
RED := $(shell tput setaf 1)
BLUE := $(shell tput setaf 4)
BOLD := $(shell tput bold)
RESET := $(shell tput sgr0)

# Docker image configuration
IMAGE_NAME := gamussa/flink-data-generator
IMAGE_TAG := latest
DOCKER_REGISTRY ?= # Set your registry if needed, leave empty for local only

# Default target
.PHONY: all
all: help

.PHONY: help
help: ## Show this help message
	@echo '${YELLOW}📋 Flink Data Generator Commands:${RESET}'
	@echo '  make ${GREEN}<target>${RESET}'
	@echo ''
	@echo '${YELLOW}Targets:${RESET}'
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  ${GREEN}%-15s${RESET} %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# Build the application
.PHONY: build
build: ## Build the application
	@echo "${BLUE}🔨 Building Flink Data Generator...${RESET}"
	@cd .. && ./gradlew :flink-data-generator:clean :flink-data-generator:shadowJar
	@echo "${GREEN}✅ Build completed successfully!${RESET}"

# Build the Docker image
.PHONY: build-image
build-image: build ## Build Docker image locally
	@echo "${BLUE}🏗️  Building Docker image...${RESET}"
	@docker build -t $(IMAGE_NAME):$(IMAGE_TAG) .
	@echo "${GREEN}✅ Image built successfully!${RESET}"

# Run tests
.PHONY: test
test: ## Run tests
	@echo "${BLUE}🧪 Running tests...${RESET}"
	@cd .. && ./gradlew :flink-data-generator:test
	@echo "${GREEN}✅ Tests completed!${RESET}"

# Run the application locally
.PHONY: run
run: build ## Run the application locally
	@echo "${BLUE}🚀 Running Flink Data Generator...${RESET}"
	@cd .. && ./gradlew :flink-data-generator:run
	@echo "${GREEN}✅ Application completed!${RESET}"

# Tag the image with registry if provided
.PHONY: tag-image
tag-image: ## Tag image with registry prefix
	@if [ -n "$(DOCKER_REGISTRY)" ]; then \
		echo "${BLUE}🏷️  Tagging image for registry: $(DOCKER_REGISTRY)${RESET}"; \
		docker tag $(IMAGE_NAME):$(IMAGE_TAG) $(DOCKER_REGISTRY)/$(IMAGE_NAME):$(IMAGE_TAG); \
		echo "${GREEN}✅ Image tagged successfully!${RESET}"; \
	else \
		echo "${YELLOW}⚠️  No registry specified, skipping tagging${RESET}"; \
	fi

# Push the image to the registry
.PHONY: push-image
push-image: tag-image ## Push image to registry
	@if [ -n "$(DOCKER_REGISTRY)" ]; then \
		echo "${BLUE}⬆️  Pushing image to registry: $(DOCKER_REGISTRY)${RESET}"; \
		docker push $(DOCKER_REGISTRY)/$(IMAGE_NAME):$(IMAGE_TAG); \
		echo "${GREEN}✅ Image pushed successfully!${RESET}"; \
	else \
		echo "${RED}❌ No registry specified, cannot push${RESET}"; \
		exit 1; \
	fi

# Clean up
.PHONY: clean
clean: ## Clean up build artifacts and Docker images
	@echo "${BLUE}🧹 Cleaning up...${RESET}"
	@cd .. && ./gradlew :flink-data-generator:clean
	@echo "${YELLOW}🗑️  Removing Docker image...${RESET}"
	@docker rmi $(IMAGE_NAME):$(IMAGE_TAG) 2>/dev/null || true
	@if [ -n "$(DOCKER_REGISTRY)" ]; then \
		docker rmi $(DOCKER_REGISTRY)/$(IMAGE_NAME):$(IMAGE_TAG) 2>/dev/null || true; \
	fi
	@echo "${GREEN}✅ Cleanup completed!${RESET}"
