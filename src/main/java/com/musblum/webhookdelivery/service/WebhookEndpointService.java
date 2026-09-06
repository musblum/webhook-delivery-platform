package com.musblum.webhookdelivery.service;

import com.musblum.webhookdelivery.model.WebhookEndpoint;
import com.musblum.webhookdelivery.repository.WebhookEndpointRepository;
import org.springframework.stereotype.Service;

@Service
public class WebhookEndpointService {

    private final WebhookEndpointRepository endpointRepository;

    public WebhookEndpointService(WebhookEndpointRepository endpointRepository) {
        this.endpointRepository = endpointRepository;
    }

    public WebhookEndpoint createEndpoint(String url) {
        WebhookEndpoint endpoint = new WebhookEndpoint(url);
        return endpointRepository.save(endpoint);
    }
}
