# Local Docker Infra Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide local MySQL and ValKey services for running the Spring Boot app with the `local` profile.

**Architecture:** Add a root-level Docker Compose file dedicated to local dependencies. Keep deployment Compose files untouched, and align `secret/application-local.yml` with the local container credentials.

**Tech Stack:** Docker Compose, MySQL 8.4, ValKey 8, Spring Boot local profile.

---

### Task 1: Local Compose Services

**Files:**
- Create: `docker-compose.local.yml`
- Modify: `src/main/resources/secret/application-local.yml`

- [x] **Step 1: Add local dependency services**

Create `docker-compose.local.yml` with MySQL and ValKey services, exposed on the standard local ports.

- [x] **Step 2: Align local database credentials**

Set `secret.datasource.username` and `secret.datasource.password` in `src/main/resources/secret/application-local.yml` to match the Compose MySQL user.

- [ ] **Step 3: Validate Compose config**

Run: `docker compose -f docker-compose.local.yml config`
Expected: Compose renders `mysql` and `valkey` services without errors.

- [ ] **Step 4: Start local dependencies**

Run: `docker compose -f docker-compose.local.yml up -d`
Expected: MySQL and ValKey containers are running.

- [ ] **Step 5: Run the app with local profile**

Run: `./gradlew bootRun --args='--spring.profiles.active=local'`
Expected: The previous placeholder error is gone; app connects to local dependencies.
