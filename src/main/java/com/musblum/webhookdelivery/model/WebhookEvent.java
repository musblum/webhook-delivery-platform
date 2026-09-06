package com.musblum.webhookdelivery.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
public class WebhookEvent {

    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private JsonNode payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    protected WebhookEvent() {}

    public WebhookEvent(String eventType, JsonNode payload) {
        this.id = UUID.randomUUID();
        this.eventType = eventType;
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }
}
