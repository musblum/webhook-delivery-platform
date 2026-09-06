package com.musblum.webhookdelivery.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateEndpointRequest(
        @NotBlank  String url
) {

}
