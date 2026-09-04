# ADR-002: Use PostgreSQL as the Primary Database

## Status

Accepted

## Context

The Webhook Delivery Platform needs to store several types of related data, including webhook endpoints, events, deliveries, and later delivery attempts.

These records are connected to each other. For example, a delivery belongs to an event and also refers to a webhook endpoint.

The platform also needs reliable storage because accepted events and delivery state should not be lost or become inconsistent when failures occur.

## Decision

PostgreSQL will be used as the primary database for the Webhook Delivery Platform.

## Alternatives Considered

### MongoDB

MongoDB stores data primarily as flexible document-style records.

It can model relationships, but the project's data is naturally relational and has several important connections between records.

## Why PostgreSQL

PostgreSQL is a strong fit because:

- the system contains multiple related entities
- foreign keys can help enforce valid relationships
- the data has a clear structured model
- the system will rely on strong consistency for important delivery state
- database transactions will become important as the reliability requirements grow

## Consequences

Using PostgreSQL means:

- events, deliveries, and endpoints can be modeled as related tables
- the database can enforce important relationships
- the project can build on existing PostgreSQL experience
- later milestones can use PostgreSQL transactions to keep related changes consistent