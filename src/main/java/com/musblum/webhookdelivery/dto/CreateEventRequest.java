package com.musblum.webhookdelivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

public record  CreateEventRequest(
        @NotNull UUID endpointId,
        @NotBlank String eventType,
        @NotNull JsonNode payload
        ) {
}
