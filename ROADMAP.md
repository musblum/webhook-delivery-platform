# Webhook Delivery Platform Roadmap

This roadmap defines the planned development order for the Webhook Delivery Platform.

The project is intentionally built in stages. Each milestone introduces new backend or distributed-systems concepts only after the previous milestone is understood and working.

The roadmap should not be changed casually. Bugs and necessary design corrections may interrupt a milestone, but unrelated features should be added to a backlog rather than changing the development order.

---

## Current Status

**Current Milestone:** M0 — Engineering Specification  
**Current Branch:** `docs/project-specification`  
**Application Code:** Not started  
**Game Engine:** Paused

---

# M0 — Engineering Specification

## Goal

Define what the system does, why it exists, and how we plan to build it before writing application code.

## Deliverables

- [x] Create GitHub repository
- [x] Create `docs/` structure
- [x] Define the problem in `README.md`
- [x] Document the initial architecture
- [x] ADR-001: Use Java as the primary language
- [x] Complete project roadmap
- [x] Define initial API
- [x] Define initial database model
- [x] Define project requirements
- [x] Add remaining initial ADRs

## Definition of Done

Another developer should be able to read the documentation and understand:

- what problem the project solves
- what the first version of the system will do
- which major technologies were chosen
- why those technologies were chosen
- what order the system will be built in

---

# M1 — Project Foundation

## Goal

Create the Java/Spring Boot application and development environment.

## Planned Work

- Java
- Spring Boot
- Maven
- PostgreSQL
- Flyway database migrations
- application configuration
- health endpoint
- unit testing
- integration testing with Testcontainers
- GitHub Actions build/test workflow
- local development environment

## Learning Focus

Before using unfamiliar tools such as Docker, Flyway, or Testcontainers, learn:

1. what problem the tool solves
2. how the problem would look without it
3. how the tool works
4. why it belongs in this project

---

# M2 — Durable Event Ingestion

## Goal

Allow another application to register a webhook destination and submit events to our platform.

## Planned API

- `POST /api/v1/endpoints`
- `POST /api/v1/events`
- `GET /api/v1/events/{id}`
- `GET /api/v1/deliveries/{id}`

## Initial Data

The system will begin storing:

- webhook endpoints
- events
- deliveries

## Learning Focus

- durable storage
- database relationships
- request validation
- event lifecycle
- delivery status

At the end of this milestone, the system can accept and remember events, but it does not yet reliably deliver them.

---

# M3 — Reliable Work Dispatch

## Goal

Separate accepting an event from performing the delivery work.

This milestone introduces the first major distributed-systems concepts.

## Planned Concepts

- asynchronous work
- queues
- Redis/Valkey
- dispatcher
- transactional outbox
- duplicate messages

## Learning Rule

These concepts must be explained and understood before implementation.

---

# M4 — Distributed Workers

## Goal

Create independent worker processes that perform webhook deliveries.

## Planned Concepts

- workers
- consumers
- concurrency
- horizontal scaling
- acknowledgements
- HTTP delivery attempts

The system should eventually support multiple workers processing different deliveries at the same time.

---

# M5 — Retries and Dead-Letter Handling

## Goal

Make temporary destination failures survivable.

## Planned Concepts

- retries
- retry policies
- exponential backoff
- jitter
- retryable vs non-retryable failures
- maximum attempts
- dead-letter handling
- manual replay

The system should not immediately lose an event because a destination temporarily fails.

---

# M6 — Worker Failure Recovery

## Goal

Recover work when one of our own worker processes crashes.

## Planned Concepts

- worker leases
- stale work
- crash recovery
- redelivery
- idempotency
- at-least-once delivery

We will intentionally crash workers and verify that another worker can recover unfinished work.

---

# M7 — Security

## Goal

Protect both the platform and webhook receivers.

## Planned Work

- API authentication
- webhook signatures
- HMAC
- request size limits
- connection timeouts
- redirect rules
- endpoint validation
- SSRF protection
- production HTTPS requirements

Security features will be introduced with explanations of the attacks or failures they prevent.

---

# M8 — Observability

## Goal

Make the system measurable and diagnosable.

## Planned Concepts

- structured logging
- metrics
- monitoring
- Micrometer
- Prometheus
- Grafana
- CloudWatch later in AWS

## Example Metrics

- events received
- successful deliveries
- failed deliveries
- retries
- dead-lettered deliveries
- delivery latency
- queue depth
- queue lag
- active workers

---

# M9 — Load Testing and Performance Engineering

## Goal

Measure how the system behaves under meaningful load and improve real bottlenecks.

## Planned Work

- create load tests
- establish performance baseline
- measure throughput
- measure p50, p95, and p99 latency
- test multiple worker counts
- identify bottlenecks
- optimize based on measurements
- benchmark again

Performance claims used on the resume must come from real measurements.

---

# M10 — AWS Deployment

## Goal

Deploy the working system to AWS.

AWS is intentionally delayed until the system works correctly locally.

## Planned Infrastructure

- ECS
- Fargate
- ECR
- RDS PostgreSQL
- ElastiCache / Valkey
- Application Load Balancer
- Secrets Manager
- CloudWatch
- AWS CDK

Each AWS service must be understood before it is added.

---

# M11 — CI/CD

## Goal

Automate testing, packaging, and deployment.

## Planned Work

Pull requests:

- compile
- run unit tests
- run integration tests
- package application

Deployment:

- build Docker images
- push images to ECR
- deploy through a controlled GitHub Actions workflow

Production deployment will not initially happen automatically after every commit.

---

# M12 — Portfolio Polish

## Goal

Turn the completed engineering project into a strong portfolio showcase.

## Planned Work

- final README
- final architecture diagrams
- demo instructions
- failure demonstrations
- benchmark results
- AWS architecture
- design tradeoffs
- delivery guarantees
- security explanation
- observability screenshots
- short project demo
- final resume bullets

---

# Scope Control

New ideas must be placed into one of the following categories.

## A — Blocking Bug

A correctness problem that prevents the current milestone from working.

**Action:** Fix immediately.

## B — Required for Current Milestone

Something necessary to complete the milestone correctly.

**Action:** Add it to the current milestone.

## C — Useful Improvement

A good idea that is not required for the current milestone.

**Action:** Add it to the backlog. Do not interrupt the roadmap.

## D — Architecture Change

A discovery that may require changing a major technical decision.

**Action:**

1. stop implementation
2. explain the problem
3. evaluate alternatives
4. create or update an ADR
5. update the roadmap only if the change is justified

---

# Explicitly Out of Scope for V1

The following are not part of the initial project:

- React frontend
- mobile application
- Kubernetes
- Kafka
- GraphQL
- machine learning
- unnecessary microservices
- billing system
- social login
- custom network protocols
- multiple implementation languages
- exactly-once delivery claims
- fancy administration dashboard

These can only be reconsidered after V1 is complete.

---

# Development Rule

For every major unfamiliar concept:

**Problem → Simple approach → Why it fails → New concept → How it works → Tradeoffs → Why we chose it → Implementation**

The project should never advance faster than the developer's understanding of the system.