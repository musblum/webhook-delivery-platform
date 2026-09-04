# ADR-001: Use Java as the Primary Language

## Status

Accepted

## Context

The Webhook Delivery Platform is intended to be a backend and distributed-systems project.

The developer already has experience building backend applications with Java, Spring Boot, PostgreSQL, and REST APIs through the Steam Backlog Manager project.

C++ is already represented separately through the game-engine project.

The primary language should allow the project to focus on learning advanced backend concepts without also requiring the developer to learn an entirely new application ecosystem.

## Decision

Java will be the primary programming language for the Webhook Delivery Platform.

Spring Boot will be used as the main application framework.

## Alternatives Considered

### C++

C++ could be used to build the system and would provide more control over memory and lower-level behavior.

However, using C++ would introduce additional complexity that is not central to the main goal of this project: learning backend and distributed-systems engineering.

### Python

Python would allow rapid development and has a strong backend ecosystem.

However, Java better builds upon the developer's existing backend experience and is the stronger language for the intended portfolio direction.

## Consequences

Using Java means:

- existing Java knowledge can be reused
- Spring Boot can be used for backend services
- more project time can be spent learning distributed-systems concepts
- the project strengthens the backend portion of the developer's portfolio
- C++ remains represented by the separate game-engine project