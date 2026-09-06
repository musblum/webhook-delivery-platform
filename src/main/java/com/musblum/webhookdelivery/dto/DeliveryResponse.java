package com.musblum.webhookdelivery.dto;

import com.musblum.webhookdelivery.model.DeliveryStatus;
import com.musblum.webhookdelivery.model.WebhookDelivery;
import com.musblum.webhookdelivery.model.WebhookEndpoint;
import com.musblum.webhookdelivery.model.WebhookEvent;

import java.time.Instant;
import java.util.UUID;

public record DeliveryResponse(
        UUID id,
        UUID eventId,
        UUID endpointId,
        DeliveryStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static DeliveryResponse from(WebhookDelivery delivery) {
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getEvent().getId(),
                delivery.getEndpoint().getId(),
                delivery.getStatus(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt()
        );
    }
}
