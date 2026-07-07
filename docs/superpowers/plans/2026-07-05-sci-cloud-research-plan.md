# SCI Cloud Research Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the apartment rental microservice project into a reproducible cloud experiment system suitable for a lower-tier SCI-style systems paper.

**Architecture:** Use Docker Compose, Nacos, Spring Cloud Gateway, Nginx, RocketMQ, Redis, MySQL, Elasticsearch/OpenSearch, and observability tooling on Alibaba Cloud. Avoid Kubernetes for the first paper version; prove scalability through multi-ECS Docker deployments and controlled scaling experiments.

**Tech Stack:** Alibaba Cloud ECS, VPC, SLB/Nginx, MSE Nacos or self-managed Nacos, ApsaraMQ for RocketMQ or self-managed RocketMQ, RDS MySQL or self-managed MySQL, Redis, Elasticsearch/OpenSearch, ARMS/Prometheus/Grafana, k6/JMeter, Spring Boot, Docker Compose.

---

### Task 1: Define Paper Contribution

**Files:**
- Create: `docs/research/problem-statement.md`

- [ ] Write the research question: how to improve throughput, p95/p99 latency, and consistency recovery for a large-scale rental transaction platform.
- [ ] Define baseline A: current synchronous Feign + single MySQL + Redis lock architecture.
- [ ] Define optimized B: Nacos discovery + Docker multi-node + RocketMQ event-driven Saga/Outbox + Redis cache + search engine + observability.
- [ ] Define measurable hypotheses: optimized B should improve QPS/TPS, p95 latency, error rate, and recovery time under 10M-scale data.

### Task 2: Alibaba Cloud Topology

**Files:**
- Create: `deploy/alicloud/topology.md`
- Create: `deploy/alicloud/security-groups.md`

- [ ] Use one VPC and one region.
- [ ] Prepare ECS-A for Gateway/User/House.
- [ ] Prepare ECS-B for Order/Payment/Contract.
- [ ] Prepare ECS-C for Notice/frontend/monitoring.
- [ ] Prepare middleware node or managed services for MySQL, Redis, RocketMQ, Nacos, and search.
- [ ] Restrict public access to Nginx/Gateway only; keep database and middleware on private IP.

### Task 3: Container Deployment Automation

**Files:**
- Create: `deploy/scripts/deploy.sh`
- Create: `deploy/scripts/restart.sh`
- Create: `deploy/scripts/health-check.sh`
- Modify: `docker-compose.app.yml`

- [ ] Split compose by server role if needed: app-a, app-b, app-c.
- [ ] Add health check endpoints per service.
- [ ] Add one-command build, pull, restart, and log commands.
- [ ] Record deployment time and service status for reproducibility.

### Task 4: Service Discovery and Gateway Load Balancing

**Files:**
- Modify: `apartment-gateway/src/main/resources/application.yml`
- Modify: all service `bootstrap.yml`

- [ ] Keep Gateway routes as `lb://service-name`.
- [ ] Ensure every service registers to Nacos.
- [ ] Run 1/2/4 instance tests for `apartment-order-service`.
- [ ] Export Nacos service list screenshots or API outputs as experiment evidence.

### Task 5: Data Scale Layer

**Files:**
- Create: `tools/data-generator/README.md`
- Create: `tools/data-generator/generate-houses.sql` or generator source file
- Create: `docs/research/dataset.md`

- [ ] Generate 10M houses, 30M images, 10M orders, and payment/account transaction data.
- [ ] Add stable distribution rules: city, district, rent price, landlord, tenant, house status.
- [ ] Record data-generation seed, row counts, import time, and storage size.

### Task 6: Search and Cache Optimization

**Files:**
- Modify: `apartment-house-service`
- Create: `docs/research/search-cache-experiment.md`

- [ ] Move house list search from MySQL LIKE/OFFSET to Elasticsearch/OpenSearch.
- [ ] Keep MySQL as source of truth.
- [ ] Use Redis for hot detail cache.
- [ ] Compare MySQL-only, Redis-only, search-engine, and search-engine-plus-cache.

### Task 7: Transaction Consistency Optimization

**Files:**
- Modify: `apartment-order-service`
- Modify: `apartment-payment-service`
- Modify: `apartment-contract-service`
- Create: SQL migration for `outbox_event` and `inbox_event`

- [ ] Add Outbox event table.
- [ ] Publish payment/order/contract events through RocketMQ.
- [ ] Add idempotent consumers with inbox table.
- [ ] Add compensation jobs for failed order-payment-contract flows.
- [ ] Compare synchronous Feign flow and event-driven flow.

### Task 8: Observability and Experiment Reports

**Files:**
- Create: `deploy/monitoring/prometheus.yml`
- Create: `docs/research/experiment-report-template.md`

- [ ] Collect JVM, HTTP, MySQL, Redis, RocketMQ, Gateway, and host metrics.
- [ ] Record QPS/TPS, p95/p99 latency, error rate, resource usage, MQ lag, DB slow queries, cache hit rate.
- [ ] Produce tables and charts for baseline, optimized, scaling, ablation, and fault recovery experiments.

### Task 9: Paper Output

**Files:**
- Create: `docs/research/paper-outline.md`

- [ ] Write abstract around scalability, event-driven consistency, and reproducibility.
- [ ] Include architecture diagram, deployment topology, dataset table, experiment curves, ablation table, and threat-to-validity section.
- [ ] Attach source code, scripts, and anonymized dataset generator for reproducibility.
