package com.musblum.webhookdelivery.repository;

import com.musblum.webhookdelivery.model.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookDeliveryRepository
        extends JpaRepository<WebhookDelivery, UUID> {
}
