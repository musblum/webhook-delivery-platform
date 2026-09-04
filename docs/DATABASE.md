# Database Design

This document describes the initial data model for the Webhook Delivery Platform.

The database design will evolve as later milestones introduce additional reliability and distributed-systems requirements.

## Initial Tables

### Webhook Endpoints

Stores destinations where webhook events can be delivered.

| Column | Purpose |
|---|---|
| `id` | Unique identifier for the endpoint |
| `url` | Destination URL |
| `created_at` | When the endpoint was created |
| `updated_at` | When the endpoint was last updated |

### Events

Stores events submitted to the platform.

An event represents something that happened, such as an order being paid.

| Column | Purpose |
|---|---|
| `id` | Unique identifier for the event |
| `event_type` | Type of event, such as `order.paid` |
| `payload` | Data associated with the event |
| `created_at` | When the event was accepted |

### Deliveries

Stores the work of delivering an event to a webhook endpoint.

| Column | Purpose |
|---|---|
| `id` | Unique identifier for the delivery |
| `event_id` | Event being delivered |
| `endpoint_id` | Destination receiving the event |
| `status` | Current delivery state |
| `created_at` | When the delivery was created |
| `updated_at` | When the delivery was last updated |

## Relationships

A delivery belongs to one event and one webhook endpoint.

```text
Event
  |
  v
Delivery
  |
  v
Webhook Endpoint
```

For example:

```text
Event:
evt_456
"Order #123 was paid"

Delivery:
del_789
event_id = evt_456
endpoint_id = ep_123
status = PENDING

Webhook Endpoint:
ep_123
https://warehouse.example.com/webhooks
```

The event describes what happened.

The endpoint describes where webhooks can be sent.

The delivery connects the two and tracks whether the event has been successfully delivered.