package com.musblum.webhookdelivery.service;

import com.musblum.webhookdelivery.exception.ResourceNotFoundException;
import com.musblum.webhookdelivery.model.WebhookDelivery;
import com.musblum.webhookdelivery.repository.WebhookDeliveryRepository;
import com.musblum.webhookdelivery.repository.WebhookEndpointRepository;
import com.musblum.webhookdelivery.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WebhookDeliveryService {

    private final WebhookDeliveryRepository webhookDeliveryRepository;

    public WebhookDeliveryService(
            WebhookDeliveryRepository webhookDeliveryRepository
    ){
        this.webhookDeliveryRepository = webhookDeliveryRepository;
    }

    public WebhookDelivery getDelivery(UUID deliveryId) {
        return webhookDeliveryRepository.findById(deliveryId).orElseThrow(() ->
                new ResourceNotFoundException("Webhook delivery not found: " + deliveryId)
        );
    }
}
