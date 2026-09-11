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

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_error")
    private String lastError;

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
        this.attemptCount = 0;
    }

    public void markSucceeded() {
        this.attemptCount++;
        this.status = DeliveryStatus.SUCCEEDED;
        this.nextAttemptAt = null;
        this.lastError = null;
    }

    public void scheduleRetry(Instant nextAttemptAt, String errorMessage) {
        this.attemptCount++;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = errorMessage;
    }

    public void markFailed(String errorMessage) {
        this.attemptCount++;
        this.status = DeliveryStatus.FAILED;
        this.nextAttemptAt = null;
        this.lastError = errorMessage;
    }

    public void clearNextAttempt() {
        this.nextAttemptAt = null;
    }

    public void resetForReplay() {
        this.status = DeliveryStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = null;
        this.lastError = null;
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

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }
}
