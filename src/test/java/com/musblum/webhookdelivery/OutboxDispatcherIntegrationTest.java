package com.musblum.webhookdelivery;

import com.musblum.webhookdelivery.service.OutboxDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class OutboxDispatcherIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Container
    static GenericContainer<?> valkey =
            new GenericContainer<>("valkey/valkey:8")
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", valkey::getHost);
        registry.add(
                "spring.data.redis.port",
                () -> valkey.getMappedPort(6379)
        );
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    OutboxDispatcher outboxDispatcher;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void dispatchesPendingOutboxMessageToValkey() throws Exception {

        String endpointResponse =
                mockMvc.perform(post("/api/v1/endpoints")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "url": "https://example.com/webhooks"
                                        }
                                        """))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String endpointId = objectMapper
                .readTree(endpointResponse)
                .get("id")
                .asText();

        String eventResponse =
                mockMvc.perform(post("/api/v1/events")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "endpointId": "%s",
                                          "eventType": "order.paid",
                                          "payload": {
                                            "orderId": 123
                                          }
                                        }
                                        """.formatted(endpointId)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String deliveryId = objectMapper
                .readTree(eventResponse)
                .get("deliveryId")
                .asText();

        outboxDispatcher.dispatchPendingMessages();

        var records = redisTemplate
                .opsForStream()
                .range("webhook-deliveries", Range.unbounded());

        assertNotNull(records);

        boolean deliveryWasPublished = records.stream()
                .anyMatch(record ->
                        deliveryId.equals(
                                record.getValue().get("deliveryId")
                        )
                );

        assertTrue(deliveryWasPublished);

        Integer publishedCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM outbox_messages
                WHERE delivery_id = ?
                  AND published_at IS NOT NULL
                """,
                Integer.class,
                UUID.fromString(deliveryId)
        );

        assertEquals(1, publishedCount);
    }
}