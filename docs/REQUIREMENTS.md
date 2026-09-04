# Project Requirements

This document defines the requirements for version 1 of the Webhook Delivery Platform.

The requirements describe what the completed system must be capable of doing. Features that are not required for version 1 should not interrupt the project roadmap.

## Functional Requirements

The system must allow a client application to:

- register a webhook endpoint
- submit an event for delivery
- retrieve information about a submitted event
- retrieve the status of a delivery

The system must:

- persist accepted events in PostgreSQL
- create delivery work for submitted events
- deliver webhook events using HTTP
- support multiple worker processes
- retry temporary delivery failures
- stop retrying after a configured maximum number of attempts
- record individual delivery attempts
- preserve failed deliveries for later inspection
- allow failed deliveries to be replayed
- recover unfinished work when a worker crashes
- sign outgoing webhooks so receivers can verify their authenticity

## Reliability Requirements

Once the platform accepts an event, the event must not be silently lost because:

- the application restarts
- a worker crashes
- a webhook destination is temporarily unavailable
- a delivery attempt times out
- a message is processed more than once

The system will provide **at-least-once delivery**, meaning a webhook may occasionally be delivered more than once, but the system should avoid losing accepted deliveries.

The system will not claim exactly-once delivery.

## Security Requirements

The system must:

- authenticate requests to the platform
- validate registered webhook URLs
- prevent obvious SSRF attacks
- apply reasonable request size limits
- use connection and request timeouts
- sign webhook deliveries using HMAC
- require secure communication in production

## Observability Requirements

The system must provide enough information to understand its behavior in production.

This includes:

- structured application logs
- delivery success and failure metrics
- retry metrics
- dead-letter metrics
- delivery latency metrics
- queue-related metrics
- health information

## Testing Requirements

The project must include:

- unit tests for important business logic
- integration tests using real containerized dependencies
- end-to-end tests for the webhook delivery flow
- controlled failure tests
- worker crash recovery tests
- load tests

## Deployment Requirements

The completed system must:

- run locally in a reproducible development environment
- run as containerized services
- be deployable to AWS
- store production data in managed PostgreSQL
- use managed Redis/Valkey-compatible infrastructure
- keep secrets outside of application source code
- support automated build and test workflows

## Performance Requirements

The system must be load tested before performance claims are made.

Performance work must follow this process:

1. measure a baseline
2. identify a bottleneck
3. make a targeted improvement
4. measure again

Any throughput or latency numbers used in the README or resume must come from real measurements.

## Version 1 Non-Goals

Version 1 will not include:

- a React frontend
- a mobile application
- Kubernetes
- Kafka
- GraphQL
- machine learning
- billing
- social login
- unnecessary microservices
- exactly-once delivery guarantees
- a custom network protocol
- multiple primary implementation languages