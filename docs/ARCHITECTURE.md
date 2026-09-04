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