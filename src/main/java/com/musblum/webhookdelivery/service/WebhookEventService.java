package com.musblum.webhookdelivery.service;

import com.musblum.webhookdelivery.exception.ResourceNotFoundException;
import com.musblum.webhookdelivery.model.OutboxMessage;
import com.musblum.webhookdelivery.model.WebhookDelivery;
import com.musblum.webhookdelivery.model.WebhookEndpoint;
import com.musblum.webhookdelivery.model.WebhookEvent;
import com.musblum.webhookdelivery.repository.OutboxMessageRepository;
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
    private final OutboxMessageRepository outboxMessageRepository;

    public WebhookEventService(
            WebhookEventRepository webhookEventRepository,
            WebhookDeliveryRepository webhookDeliveryRepository,
            WebhookEndpointRepository webhookEndpointRepository,
            OutboxMessageRepository outboxMessageRepository) {
        this.webhookEventRepository = webhookEventRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.outboxMessageRepository = outboxMessageRepository;
    }


    @Transactional
    public WebhookDelivery createEvent(
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

        WebhookDelivery savedDelivery = webhookDeliveryRepository.save(delivery);

        OutboxMessage outboxMessage = new OutboxMessage(savedDelivery);
        outboxMessageRepository.save(outboxMessage);

        return savedDelivery;
    }

    public WebhookEvent getEvent(UUID eventId) {
        return webhookEventRepository.findById(eventId).orElseThrow(() ->
                new ResourceNotFoundException("Event not found: " + eventId)
        );
    }



}

