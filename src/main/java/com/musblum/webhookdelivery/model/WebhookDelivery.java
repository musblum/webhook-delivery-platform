package com.musblum.webhookdelivery.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deliveries")
public class WebhookDelivery {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private WebhookEvent event;

    @ManyToOne
    @JoinColumn(name = "endpoint_id", nullable = false)
    private WebhookEndpoint endpoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeliveryStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    protected WebhookDelivery() {}

    public WebhookDelivery(WebhookEvent event, WebhookEndpoint endpoint) {
        this.id = UUID.randomUUID();
        this.event = event;
        this.endpoint = endpoint;
        this.status =  DeliveryStatus.PENDING;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public WebhookEndpoint getEndpoint() {
        return endpoint;
    }

    public WebhookEvent getEvent() {
        return event;
    }

    public UUID getId() {
        return id;
    }

    public DeliveryStatus getStatus() {
        return status;
    }
}
