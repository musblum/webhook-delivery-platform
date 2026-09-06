package com.musblum.webhookdelivery.service;

import com.musblum.webhookdelivery.exception.ResourceNotFoundException;
import com.musblum.webhookdelivery.model.WebhookDelivery;
import com.musblum.webhookdelivery.model.WebhookEndpoint;
import com.musblum.webhookdelivery.model.WebhookEvent;
import com.musblum.webhookdelivery.repository.WebhookDeliveryRepository;
import com.musblum.webhookdelivery.repository.WebhookEndpointRepository;
import com.musblum.webhookdelivery.repository.WebhookEventRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

@Service
public class WebhookEventService {

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final WebhookEndpointRepository webhookEndpointRepository;

    public WebhookEventService(
            WebhookEventRepository webhookEventRepository,
            WebhookDeliveryRepository webhookDeliveryRepository,
            WebhookEndpointRepository webhookEndpointRepository
            ) {
        this.webhookEventRepository = webhookEventRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.webhookEndpointRepository = webhookEndpointRepository;
    }


    @Transactional
    public WebhookEvent createEvent(
            UUID endpointId,
            String eventType,
            JsonNode payload
            ) {

        WebhookEndpoint endpoint =
                webhookEndpointRepository.findById(endpointId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Endpoint not found: " + endpointId)
                        );

        WebhookEvent event = new WebhookEvent(eventType, payload);
        WebhookEvent savedEvent = webhookEventRepository.save(event);

        WebhookDelivery delivery = new WebhookDelivery(savedEvent, endpoint);

        webhookDeliveryRepository.save(delivery);

        return savedEvent;
    }

    public WebhookEvent getEvent(UUID eventId) {
        return webhookEventRepository.findById(eventId).orElseThrow(() ->
                new ResourceNotFoundException("Event not found: " + eventId)
        );
    }



}

