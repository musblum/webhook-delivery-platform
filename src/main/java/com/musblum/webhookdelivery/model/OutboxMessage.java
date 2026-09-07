package com.musblum.webhookdelivery.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {


    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "delivery_id", nullable = false)
    private WebhookDelivery delivery;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
    }

    protected OutboxMessage() {}

    public OutboxMessage(WebhookDelivery delivery){
        this.id = UUID.randomUUID();
        this.delivery = delivery;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public WebhookDelivery getDelivery() {
        return delivery;
    }

    public UUID getId() {
        return id;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
