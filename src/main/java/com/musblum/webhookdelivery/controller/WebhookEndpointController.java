package com.musblum.webhookdelivery.controller;

import com.musblum.webhookdelivery.dto.CreateEndpointRequest;
import com.musblum.webhookdelivery.dto.EndpointResponse;
import com.musblum.webhookdelivery.model.WebhookEndpoint;
import com.musblum.webhookdelivery.service.WebhookEndpointService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/endpoints")
public class WebhookEndpointController {

    private final WebhookEndpointService endpointService;

    public WebhookEndpointController(WebhookEndpointService endpointService) {
        this.endpointService = endpointService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EndpointResponse createEndpoint(
            @Valid @RequestBody CreateEndpointRequest request
    ){

        WebhookEndpoint endpoint = endpointService.createEndpoint(request.url());
        return EndpointResponse.from(endpoint);
    }

}