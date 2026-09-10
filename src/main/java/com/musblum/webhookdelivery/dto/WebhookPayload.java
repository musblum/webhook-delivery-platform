package com.musblum.webhookdelivery.dto;

import tools.jackson.databind.JsonNode;

import java.util.UUID;

public record WebhookPayload(
        UUID eventId,
        String eventType,
        JsonNode payload
) {
}
