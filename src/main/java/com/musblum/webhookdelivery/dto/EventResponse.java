package com.musblum.webhookdelivery.dto;

import com.musblum.webhookdelivery.model.WebhookEvent;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String eventType,
        JsonNode payload,
        Instant createdAt
) {
    public static EventResponse from(WebhookEvent event) {
        return new EventResponse(
                event.getId(), event.getEventType(), event.getPayload(), event.getCreatedAt()
        );
    }
}
