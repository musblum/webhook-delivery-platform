package com.musblum.webhookdelivery.repository;

import com.musblum.webhookdelivery.model.DeliveryStatus;
import com.musblum.webhookdelivery.model.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository
        extends JpaRepository<WebhookDelivery, UUID> {

    List<WebhookDelivery> findByStatusAndNextAttemptAtIsNotNullAndNextAttemptAtIsLessThanEqual
            (DeliveryStatus status, Instant now);
}
