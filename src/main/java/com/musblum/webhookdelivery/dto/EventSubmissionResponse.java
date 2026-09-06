package com.musblum.webhookdelivery.dto;

import com.musblum.webhookdelivery.model.DeliveryStatus;
import com.musblum.webhookdelivery.model.WebhookDelivery;

import java.util.UUID;

public record EventSubmissionResponse(
        UUID eventId,
        UUID deliveryId,
        DeliveryStatus status
) {
    public static EventSubmissionResponse from(WebhookDelivery delivery) {
        return new EventSubmissionResponse(
                delivery.getEvent().getId(),
                delivery.getId(),
                delivery.getStatus()
        );
    }
}
