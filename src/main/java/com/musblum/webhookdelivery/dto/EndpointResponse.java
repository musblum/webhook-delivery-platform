package com.musblum.webhookdelivery.dto;

import com.musblum.webhookdelivery.model.WebhookEndpoint;

import java.time.Instant;
import java.util.UUID;

public record EndpointResponse(
        UUID id,
        String url,
        Instant createdAt,
        Instant updatedAt
) {
    public static EndpointResponse from(WebhookEndpoint endpoint) {
        return new EndpointResponse(
                endpoint.getId(), endpoint.getUrl(), endpoint.getCreatedAt(), endpoint.getUpdatedAt()
        );
    }
}
