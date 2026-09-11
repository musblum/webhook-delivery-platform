package com.musblum.webhookdelivery.service;

import com.musblum.webhookdelivery.model.DeliveryStatus;
import com.musblum.webhookdelivery.model.OutboxMessage;
import com.musblum.webhookdelivery.model.WebhookDelivery;
import com.musblum.webhookdelivery.repository.OutboxMessageRepository;
import com.musblum.webhookdelivery.repository.WebhookDeliveryRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;


@Service
@ConditionalOnProperty(
        name = "app.dispatcher.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RetryScheduler {
    private final WebhookDeliveryRepository deliveryRepository;
    private final OutboxMessageRepository outboxMessageRepository;

    RetryScheduler(WebhookDeliveryRepository webhookDeliveryRepository, OutboxMessageRepository messageRepository) {
        this.deliveryRepository = webhookDeliveryRepository;
        this.outboxMessageRepository = messageRepository;
    }

    @Scheduled(fixedRate = 1000)
    @Transactional
    public void scheduleDueRetries(){
        var deliveries =
                deliveryRepository
                        .findByStatusAndNextAttemptAtIsNotNullAndNextAttemptAtIsLessThanEqual(
                                DeliveryStatus.PENDING,
                                Instant.now()
                        );
        for (WebhookDelivery delivery : deliveries) {
            delivery.clearNextAttempt();

            deliveryRepository.save(delivery);

            OutboxMessage outboxMessage = new OutboxMessage(delivery);
            outboxMessageRepository.save(outboxMessage);
        }
    }
}
