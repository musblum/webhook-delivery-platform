package com.musblum.webhookdelivery.repository;

import com.musblum.webhookdelivery.model.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxMessageRepository
        extends JpaRepository<OutboxMessage, UUID> {

    List<OutboxMessage> findByPublishedAtIsNull();
}
