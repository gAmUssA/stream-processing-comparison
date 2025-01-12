# Colors
YELLOW := $(shell tput setaf 3)
GREEN := $(shell tput setaf 2)
RED := $(shell tput setaf 1)
BLUE := $(shell tput setaf 4)
BOLD := $(shell tput bold)
RESET := $(shell tput sgr0)

.PHONY: help docker-up docker-down build clean kafka-ready

help: ## Show this help message
	@echo '${YELLOW}Usage:${RESET}'
	@echo '  make ${GREEN}<target>${RESET}'
	@echo ''
	@echo '${YELLOW}Targets:${RESET}'
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  ${GREEN}%-15s${RESET} %s\n", $$1, $$2}' $(MAKEFILE_LIST)

build: ## Build all applications
	@echo "${BLUE}🔨 Building applications...${RESET}"
	@./gradlew clean build
	@echo "${GREEN}✅ Build complete!${RESET}"

docker-up: build ## Start all containers
	@echo "${BLUE}🐳 Starting Docker containers...${RESET}"
	@docker compose up -d --build
	@$(MAKE) kafka-ready
	@echo "${GREEN}✅ All services are up!${RESET}"

docker-down: ## Stop all containers
	@echo "${BLUE}🔽 Stopping Docker containers...${RESET}"
	@docker compose down
	@echo "${GREEN}✅ All services stopped!${RESET}"

kafka-ready: ## Wait for Kafka to be ready
	@echo "${YELLOW}⏳ Waiting for Kafka to be ready...${RESET}"
	@until docker compose exec kafka kafka-topics.sh --bootstrap-server kafka:9092 --list > /dev/null 2>&1; do \
		echo "${YELLOW}⌛ Waiting for Kafka...${RESET}"; \
		sleep 2; \
	done
	@echo "${GREEN}✅ Kafka is ready!${RESET}"

clean: docker-down ## Clean up everything
	@echo "${BLUE}🧹 Cleaning up...${RESET}"
	@./gradlew clean
	@docker system prune -f
	@echo "${GREEN}✅ Clean up complete!${RESET}"
