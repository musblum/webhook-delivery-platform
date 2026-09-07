# Architecture

This document describes how the Webhook Delivery Platform is structured and how its components work together.

The architecture will grow as the project progresses. New components will only be added when the problem they solve has been introduced and understood.

## Initial Architecture

At the beginning of the project, the system has three main participants:

```text
Client Application
        |
        | HTTP request
        v
+-------------------+
|      Our API      |
|   Spring Boot     |
+-------------------+
        |
        | save data
        v
+-------------------+
|    PostgreSQL     |
+-------------------+
```

## Reliable Work Dispatch

Event ingestion and delivery processing are separated so the API does not need to perform webhook delivery before responding to the client.

```text
Client
  |
  | POST /api/v1/events
  v
Spring Boot API
  |
  | one PostgreSQL transaction
  v
Event + Delivery + Outbox Message
  |
  | later
  v
Outbox Dispatcher
  |
  v
Valkey Stream
webhook-deliveries