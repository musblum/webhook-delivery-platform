# ADR-003: Keep Version 1 Backend-Only

## Status

Accepted

## Context

The purpose of the Webhook Delivery Platform is to demonstrate advanced backend and distributed-systems engineering.

The project's main learning goals include reliable event processing, asynchronous work, worker coordination, failure recovery, security, observability, performance, and cloud deployment.

A frontend dashboard could make the project more visually appealing, but building and maintaining one would consume time that could otherwise be spent on the backend systems that define the project.

## Decision

Version 1 of the Webhook Delivery Platform will not include a custom frontend application.

The system will expose HTTP APIs for interaction.

Documentation, API tools, logs, and monitoring dashboards may be used to demonstrate and inspect the system.

## Alternatives Considered

### Build a React Frontend

A React frontend could provide a polished user interface for registering endpoints, viewing events, and checking delivery status.

However, it is not required to prove the core engineering concepts this project is intended to demonstrate.

## Consequences

Keeping version 1 backend-only means:

- more development time can be spent on backend reliability and distributed-systems concepts
- the project remains focused on its primary engineering goals
- the API must be documented clearly
- the system can still be demonstrated using API requests and monitoring tools
- a custom frontend may be reconsidered after version 1 is complete