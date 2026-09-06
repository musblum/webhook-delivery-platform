package com.musblum.webhookdelivery.repository;

import com.musblum.webhookdelivery.model.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookEndpointRepository
        extends JpaRepository<WebhookEndpoint, UUID> {
}