package com.musblum.webhookdelivery.controller;

import com.musblum.webhookdelivery.dto.DeliveryResponse;
import com.musblum.webhookdelivery.model.WebhookDelivery;
import com.musblum.webhookdelivery.service.WebhookDeliveryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deliveries")
public class WebhookDeliveryController {

    private final WebhookDeliveryService deliveryService;

    public WebhookDeliveryController(WebhookDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/{id}")
    public DeliveryResponse getDelivery(@PathVariable UUID id) {
        WebhookDelivery delivery = deliveryService.getDelivery(id);
        return DeliveryResponse.from(delivery);
    }

}
