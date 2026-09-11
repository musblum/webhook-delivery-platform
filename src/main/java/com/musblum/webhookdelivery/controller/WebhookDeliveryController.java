package com.musblum.webhookdelivery.controller;

import com.musblum.webhookdelivery.dto.DeliveryResponse;
import com.musblum.webhookdelivery.model.WebhookDelivery;
import com.musblum.webhookdelivery.service.WebhookDeliveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.musblum.webhookdelivery.service.DeliveryReplayService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deliveries")
public class WebhookDeliveryController {

    private final WebhookDeliveryService deliveryService;
    private final DeliveryReplayService replayService;

    public WebhookDeliveryController(
            WebhookDeliveryService deliveryService,
            DeliveryReplayService replayService) {
        this.deliveryService = deliveryService;
        this.replayService = replayService;
    }

    @GetMapping("/{id}")
    public DeliveryResponse getDelivery(@PathVariable UUID id) {
        WebhookDelivery delivery = deliveryService.getDelivery(id);
        return DeliveryResponse.from(delivery);
    }

    @PostMapping("/{id}/replay")
    public ResponseEntity<DeliveryResponse> replayDelivery(
            @PathVariable UUID id) {

        WebhookDelivery delivery =
                replayService.replay(id);

        return ResponseEntity.ok(
                new DeliveryResponse(
                        delivery.getId(),
                        delivery.getEvent().getId(),
                        delivery.getEndpoint().getId(),
                        delivery.getStatus(),
                        delivery.getCreatedAt(),
                        delivery.getUpdatedAt()
                )
        );
    }

}
