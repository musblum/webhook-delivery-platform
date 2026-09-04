# API Design

This document describes the public HTTP API for the Webhook Delivery Platform.

The API allows client applications to register webhook destinations, submit events for delivery, and inspect the status of events and deliveries.

## Base Path

All API endpoints use:

`/api/v1`

## Endpoints

### Register a Webhook Endpoint

`POST /api/v1/endpoints`

Registers a destination where webhook events can be delivered.

Example request:

```json
{
  "url": "https://warehouse.example.com/webhooks"
}
```

Example response:

```json
{
  "id": "ep_123",
  "url": "https://warehouse.example.com/webhooks"
}
```

### Submit an Event

`POST /api/v1/events`

Accepts an event that should eventually be delivered to a registered webhook endpoint.

Example request:

```json
{
  "endpointId": "ep_123",
  "eventType": "order.paid",
  "payload": {
    "orderId": 123
  }
}
```

Example response:

```json
{
  "id": "evt_456",
  "status": "ACCEPTED"
}
```

The event being accepted does not mean it has already been delivered.

It means the platform has accepted responsibility for processing it.

### Get an Event

`GET /api/v1/events/{id}`

Returns information about an event previously submitted to the platform.

Example response:

```json
{
  "id": "evt_456",
  "eventType": "order.paid",
  "status": "ACCEPTED"
}
```

### Get a Delivery

`GET /api/v1/deliveries/{id}`

Returns information about the delivery of an event to a specific webhook endpoint.

Example response:

```json
{
  "id": "del_789",
  "eventId": "evt_456",
  "endpointId": "ep_123",
  "status": "PENDING"
}
```

## Event vs Delivery

An event represents something that happened.

Example:

`Order #123 was paid.`

A delivery represents the platform's responsibility to send that event to a specific destination.

Example:

`Send event evt_456 to endpoint ep_123.`

One event may eventually have multiple deliveries if it needs to be sent to multiple destinations.