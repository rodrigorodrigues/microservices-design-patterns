# Add scala-address-service to docker-compose.yml Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the `scala-address-service` to the `docker/docker-compose.yml` file to enable containerized deployment alongside other microservices.

**Architecture:** The `scala-address-service` will be added as a new service named `address-api`. It will depend on `service-discovery`, `mongodb-datasource`, and `service-discovery-load-configuration`. It will use the `scala-address-service:0.0.1-SNAPSHOT` image and expose port `8085`.

**Tech Stack:** Docker Compose, Scala, Apache Pekko HTTP, Spring Boot (for Data), MongoDB, Consul.

---

### Task 1: Update docker/docker-compose.yml

**Files:**
- Modify: `docker/docker-compose.yml`

- [ ] **Step 1: Add the address-api service definition**

Insert the following block after the `person-api` service (around line 326):

```yaml
  address-api:
    image: scala-address-service:0.0.1-SNAPSHOT
    container_name: address-api
    environment:
      - SPRING_PROFILES_ACTIVE=consul,dev
      - CONSUL_URL=service-discovery:8500
      - SPRING_CONFIG_IMPORT=consul:service-discovery:8500
      - SERVER_PORT=8085
      - SPRING_CLOUD_KUBERNETES_ENABLED=false
      - SPRING_MONGODB_URI=mongodb://mongodb-datasource:27017/docker
      - SPRING_MONGODB_DATABASE=docker
      - MANAGEMENT_ENDPOINTS_WEB_CORS_ALLOW_CREDENTIALS=false
      - DEBUG=true
      - JAVA_OPTS=--enable-preview
      - SPRING_CLOUD_CONSUL_HOST=service-discovery
    depends_on:
      - service-discovery
      - mongodb-datasource
      - service-discovery-load-configuration
    ports:
      - 8085:8085
    volumes:
      - ./docker-entrypoint.sh:/docker-entrypoint.sh
    networks:
      net:
        aliases:
          - address-api
```

- [ ] **Step 2: Verify indentation and structure**

Ensure the new service is correctly aligned with other services and doesn't break the YAML structure.

- [ ] **Step 3: Commit**

```bash
git add docker/docker-compose.yml
git commit -m "chore: add scala-address-service to docker-compose"
```
