package com.musblum.webhookdelivery.service;

import com.musblum.webhookdelivery.model.DeliveryStatus;
import com.musblum.webhookdelivery.model.OutboxMessage;
import com.musblum.webhookdelivery.model.WebhookDelivery;
import com.musblum.webhookdelivery.repository.OutboxMessageRepository;
import com.musblum.webhookdelivery.repository.WebhookDeliveryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeliveryReplayService {

    private final WebhookDeliveryRepository deliveryRepository;
    private final OutboxMessageRepository outboxMessageRepository;

    public DeliveryReplayService(
            WebhookDeliveryRepository deliveryRepository,
            OutboxMessageRepository outboxMessageRepository) {
        this.deliveryRepository = deliveryRepository;
        this.outboxMessageRepository = outboxMessageRepository;
    }

    @Transactional
    public WebhookDelivery replay(UUID deliveryId){

        WebhookDelivery delivery =
                deliveryRepository.findById(deliveryId)
                        .orElseThrow();


        if(delivery.getStatus() != DeliveryStatus.FAILED) {
            throw new IllegalStateException(
                    "Only FAILED deliveries can be replayed."
            );
        }

        delivery.resetForReplay();

        OutboxMessage outboxMessage = new OutboxMessage(delivery);
        deliveryRepository.save(delivery);
        outboxMessageRepository.save(outboxMessage);

        return delivery;
    }
}