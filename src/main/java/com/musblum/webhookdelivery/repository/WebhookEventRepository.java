package com.musblum.webhookdelivery.repository;

import com.musblum.webhookdelivery.model.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookEventRepository
        extends JpaRepository<WebhookEvent, UUID>{
}
